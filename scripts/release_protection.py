#!/usr/bin/env python3
"""Audit GitHub repository settings required by the Android release workflow."""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Sequence


ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = ROOT / "release-protection.json"


class ProtectionError(RuntimeError):
    pass


def load_config(path: Path = CONFIG_PATH) -> dict:
    with path.open(encoding="utf-8") as stream:
        config = json.load(stream)
    if not config.get("repository") or not config.get("branches") or not config.get("environments"):
        raise ProtectionError("release-protection.json is incomplete")
    return config


def gh_json(endpoint: str, *, allow_missing: bool = False) -> dict:
    result = subprocess.run(
        ["gh", "api", endpoint], cwd=ROOT, text=True, capture_output=True, check=False
    )
    if result.returncode:
        message = result.stderr.strip() or result.stdout.strip() or "GitHub API request failed"
        if allow_missing and "HTTP 404" in message:
            return {}
        raise ProtectionError(f"{endpoint}: {message}")
    return json.loads(result.stdout)


def collect_live_snapshot(config: dict) -> dict:
    if not shutil.which("gh"):
        raise ProtectionError("GitHub CLI (gh) is required for a live settings audit")
    repository = config["repository"]
    branches = {
        name: gh_json(
            f"repos/{repository}/branches/{name}/protection", allow_missing=True
        )
        for name in config["branches"]
    }
    environments = {}
    for name in config["environments"]:
        environments[name] = {
            "environment": gh_json(
                f"repos/{repository}/environments/{name}", allow_missing=True
            ),
            "policies": gh_json(
                f"repos/{repository}/environments/{name}/deployment-branch-policies",
                allow_missing=True,
            ),
        }
    return {"branches": branches, "environments": environments}


def enabled(value: object) -> bool:
    if isinstance(value, dict):
        return bool(value.get("enabled"))
    return bool(value)


def reviewer_logins(environment: dict) -> set[str]:
    reviewers: set[str] = set()
    for rule in environment.get("protection_rules", []):
        if rule.get("type") != "required_reviewers":
            continue
        for item in rule.get("reviewers", []):
            reviewer = item.get("reviewer") or {}
            login = reviewer.get("login")
            if login:
                reviewers.add(login)
    return reviewers


def policy_keys(policies: dict) -> set[tuple[str, str | None]]:
    return {
        (item.get("name", ""), item.get("type"))
        for item in policies.get("branch_policies", [])
    }


def audit_snapshot(snapshot: dict, config: dict) -> list[str]:
    errors: list[str] = []
    actual_branches = snapshot.get("branches", {})
    for name, expected in config["branches"].items():
        actual = actual_branches.get(name)
        if not actual:
            errors.append(f"branch {name} has no protection")
            continue
        if expected.get("requirePullRequest") and not actual.get("required_pull_request_reviews"):
            errors.append(f"branch {name} does not require pull requests")
        contexts = set((actual.get("required_status_checks") or {}).get("contexts", []))
        missing_checks = sorted(set(expected.get("requiredChecks", [])) - contexts)
        if missing_checks:
            errors.append(f"branch {name} is missing required checks: {', '.join(missing_checks)}")
        expected_app_id = expected.get("requiredCheckAppId")
        if expected_app_id is not None:
            checks = {
                item.get("context"): item.get("app_id")
                for item in (actual.get("required_status_checks") or {}).get("checks", [])
            }
            wrong_apps = sorted(
                context for context in expected.get("requiredChecks", [])
                if checks.get(context) != expected_app_id
            )
            if wrong_apps:
                errors.append(
                    f"branch {name} checks are not pinned to app {expected_app_id}: "
                    + ", ".join(wrong_apps)
                )
        if enabled(actual.get("allow_force_pushes")) != expected.get("allowForcePushes", False):
            errors.append(f"branch {name} force-push policy does not match")
        if enabled(actual.get("allow_deletions")) != expected.get("allowDeletions", False):
            errors.append(f"branch {name} deletion policy does not match")

    actual_environments = snapshot.get("environments", {})
    for name, expected in config["environments"].items():
        bundle = actual_environments.get(name)
        if not bundle:
            errors.append(f"environment {name} is missing")
            continue
        environment = bundle.get("environment", {})
        branch_policy = environment.get("deployment_branch_policy") or {}
        if not branch_policy.get("custom_branch_policies"):
            errors.append(f"environment {name} does not restrict deployment refs")
        actual_policies = policy_keys(bundle.get("policies", {}))
        for required in expected.get("allowedRefs", []):
            key = (required["name"], required.get("type"))
            legacy_key = (required["name"], None)
            if key not in actual_policies and legacy_key not in actual_policies:
                errors.append(
                    f'environment {name} is missing {required.get("type", "ref")} '
                    f'policy {required["name"]}'
                )
        missing_reviewers = sorted(
            set(expected.get("requiredReviewers", [])) - reviewer_logins(environment)
        )
        if missing_reviewers:
            errors.append(
                f"environment {name} is missing required reviewers: "
                + ", ".join(missing_reviewers)
            )
    return errors


def audit(args: argparse.Namespace) -> None:
    config = load_config(Path(args.config))
    if args.snapshot:
        with Path(args.snapshot).open(encoding="utf-8") as stream:
            snapshot = json.load(stream)
    else:
        snapshot = collect_live_snapshot(config)
    errors = audit_snapshot(snapshot, config)
    print(json.dumps({
        "valid": not errors,
        "repository": config["repository"],
        "errors": errors,
    }, indent=2))
    if errors:
        raise ProtectionError("GitHub release protections do not match the repository contract")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("audit",))
    parser.add_argument("--config", default=str(CONFIG_PATH))
    parser.add_argument("--snapshot")
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    try:
        args = build_parser().parse_args(argv)
        audit(args)
        return 0
    except (ProtectionError, OSError, json.JSONDecodeError) as error:
        print(f"release-protection: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
