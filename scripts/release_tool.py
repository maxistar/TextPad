#!/usr/bin/env python3
"""Repository-owned Android release preparation and validation tool."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence


ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = ROOT / "release-config.json"
GRADLE_PATH = ROOT / "app" / "build.gradle"
MANIFEST_PATH = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
SEMVER_RE = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")
VERSION_CODE_RE = re.compile(r"(?m)^\s*versionCode\s+(\d+)\s*$")
VERSION_NAME_RE = re.compile(r'(?m)^\s*versionName\s+["\']([^"\']+)["\']\s*$')


class ReleaseError(RuntimeError):
    pass


@dataclass(frozen=True)
class Version:
    name: str
    code: int

    @property
    def semver(self) -> tuple[int, int, int]:
        return parse_semver(self.name)


def load_config(path: Path = CONFIG_PATH) -> dict:
    with path.open(encoding="utf-8") as stream:
        config = json.load(stream)
    required = {"branches", "tagPrefix", "metadataRoot", "requiredLocales", "placeholderMarkers"}
    missing = required.difference(config)
    if missing:
        raise ReleaseError(f"release-config.json is missing: {', '.join(sorted(missing))}")
    return config


def read_version(path: Path = GRADLE_PATH) -> Version:
    text = path.read_text(encoding="utf-8")
    codes = VERSION_CODE_RE.findall(text)
    names = VERSION_NAME_RE.findall(text)
    if len(codes) != 1 or len(names) != 1:
        raise ReleaseError("Gradle must contain exactly one versionCode and versionName")
    parse_semver(names[0])
    return Version(names[0], int(codes[0]))


def write_version(version: Version, path: Path = GRADLE_PATH) -> None:
    text = path.read_text(encoding="utf-8")
    text, code_count = VERSION_CODE_RE.subn(f"        versionCode {version.code}", text)
    text, name_count = VERSION_NAME_RE.subn(f'        versionName "{version.name}"', text)
    if code_count != 1 or name_count != 1:
        raise ReleaseError("Refusing to update ambiguous Gradle version declarations")
    path.write_text(text, encoding="utf-8")


def parse_semver(value: str) -> tuple[int, int, int]:
    match = SEMVER_RE.fullmatch(value)
    if not match:
        raise ReleaseError(f"Invalid release SemVer: {value}")
    return tuple(int(part) for part in match.groups())


def next_version(current: Version, bump: str | None, explicit: str | None) -> Version:
    if bool(bump) == bool(explicit):
        raise ReleaseError("Specify exactly one of --bump or --version")
    major, minor, patch = current.semver
    if explicit:
        candidate = parse_semver(explicit)
        if candidate <= current.semver:
            raise ReleaseError("Explicit version must be greater than the current version")
        name = explicit
    elif bump == "major":
        name = f"{major + 1}.0.0"
    elif bump == "minor":
        name = f"{major}.{minor + 1}.0"
    else:
        name = f"{major}.{minor}.{patch + 1}"
    return Version(name, current.code + 1)


def run(command: Sequence[str], *, check: bool = True) -> str:
    result = subprocess.run(command, cwd=ROOT, check=False, text=True, capture_output=True)
    if check and result.returncode:
        message = result.stderr.strip() or result.stdout.strip() or "command failed"
        raise ReleaseError(f"{' '.join(command)}: {message}")
    return result.stdout.strip()


def git_ref_exists(ref: str) -> bool:
    return subprocess.run(
        ["git", "show-ref", "--verify", "--quiet", ref], cwd=ROOT, check=False
    ).returncode == 0


def discover_locales(config: dict, root: Path = ROOT) -> list[str]:
    metadata_root = root / config["metadataRoot"]
    ignored = set(config.get("ignoredLocales", []))
    locales = sorted(
        item.name for item in metadata_root.iterdir()
        if item.is_dir() and item.name not in ignored and (item / "title.txt").is_file()
    )
    missing = sorted(set(config["requiredLocales"]) - set(locales))
    if missing:
        raise ReleaseError(f"Required Fastlane locales are missing: {', '.join(missing)}")
    if not locales:
        raise ReleaseError("No Fastlane metadata locales discovered")
    return locales


def changelog_paths(version: Version, config: dict, root: Path = ROOT) -> list[Path]:
    return [
        root / config["metadataRoot"] / locale / "changelogs" / f"{version.code}.txt"
        for locale in discover_locales(config, root)
    ]


def validate_changelogs(version: Version, config: dict, root: Path = ROOT) -> None:
    markers = tuple(config["placeholderMarkers"])
    errors = []
    for path in changelog_paths(version, config, root):
        if not path.is_file():
            errors.append(f"missing {path.relative_to(root)}")
            continue
        content = path.read_text(encoding="utf-8").strip()
        if not content:
            errors.append(f"empty {path.relative_to(root)}")
        elif any(marker in content for marker in markers):
            errors.append(f"placeholder remains in {path.relative_to(root)}")
    if errors:
        raise ReleaseError("Invalid localized release notes:\n- " + "\n- ".join(errors))


def verify_build_identity(version: Version, manifest_path: Path, metadata_path: Path) -> None:
    android = "{http://schemas.android.com/apk/res/android}"
    root = ET.parse(manifest_path).getroot()
    manifest_code = int(root.attrib[android + "versionCode"])
    manifest_name = root.attrib[android + "versionName"]
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    element = metadata["elements"][0]
    identities = {
        (version.code, version.name),
        (manifest_code, manifest_name),
        (int(element["versionCode"]), element["versionName"]),
    }
    if len(identities) != 1:
        raise ReleaseError(f"Build output identity does not match Gradle: {sorted(identities)}")


def version_from_tag(tag: str, config: dict) -> Version | None:
    gradle = run(["git", "show", f"{tag}:app/build.gradle"], check=False)
    code_match = VERSION_CODE_RE.search(gradle)
    name_match = VERSION_NAME_RE.search(gradle)
    if code_match and name_match:
        return Version(name_match.group(1), int(code_match.group(1)))
    manifest = run(["git", "show", f"{tag}:app/src/main/AndroidManifest.xml"], check=False)
    code_match = re.search(r'android:versionCode="(\d+)"', manifest)
    name_match = re.search(r'android:versionName="([^"]+)"', manifest)
    if code_match and name_match:
        return Version(name_match.group(1), int(code_match.group(1)))
    return None


def validate_history(version: Version, config: dict, *, require_absent_tag: bool = True) -> None:
    expected_tag = config["tagPrefix"] + version.name
    if require_absent_tag and git_ref_exists(f"refs/tags/{expected_tag}"):
        raise ReleaseError(f"Tag already exists: {expected_tag}")
    historical = [
        parsed for tag in run(["git", "tag", "--list", f'{config["tagPrefix"]}*']).splitlines()
        if require_absent_tag or tag != expected_tag
        if (parsed := version_from_tag(tag, config)) is not None
    ]
    if historical and version.code <= max(item.code for item in historical):
        raise ReleaseError("versionCode must be greater than all tagged releases")
    same_code = [item.name for item in historical if item.code == version.code]
    if same_code:
        raise ReleaseError(f"versionCode {version.code} already belongs to {same_code[0]}")


def current_branch() -> str:
    branch = run(["git", "branch", "--show-current"])
    if not branch:
        raise ReleaseError("A branch checkout is required")
    return branch


def validate_branch(version: Version, config: dict, *, mode: str | None = None) -> str:
    branch = current_branch()
    branches = config["branches"]
    inferred = mode
    if inferred is None:
        if branch == f'{branches["releasePrefix"]}{version.name}':
            inferred = "release"
        elif branch == f'{branches["hotfixPrefix"]}{version.name}':
            inferred = "hotfix"
        else:
            raise ReleaseError(f"Unexpected release branch: {branch}")
    base = branches["development"] if inferred == "release" else branches["stable"]
    expected = f'{branches[inferred + "Prefix"]}{version.name}'
    if branch != expected:
        raise ReleaseError(f"Expected branch {expected}, found {branch}")
    base_ref = base if git_ref_exists(f"refs/heads/{base}") else f"origin/{base}"
    if subprocess.run(["git", "merge-base", "--is-ancestor", base_ref, "HEAD"], cwd=ROOT).returncode:
        raise ReleaseError(f"{branch} is not based on {base}")
    return inferred


def candidate_conflicts(version: Version, config: dict, mode: str) -> list[str]:
    branches = config["branches"]
    branch = f'{branches[mode + "Prefix"]}{version.name}'
    tag = config["tagPrefix"] + version.name
    conflicts = []
    for ref in (f"refs/heads/{branch}", f"refs/remotes/origin/{branch}", f"refs/tags/{tag}"):
        if git_ref_exists(ref):
            conflicts.append(ref)
    if shutil.which("gh"):
        gh = subprocess.run(
            ["gh", "pr", "list", "--state", "open", "--head", branch, "--json", "number"],
            cwd=ROOT, text=True, capture_output=True, check=False,
        )
        if gh.returncode == 0 and json.loads(gh.stdout or "[]"):
            conflicts.append(f"open pull request for {branch}")
    return conflicts


def ensure_clean_current_base(config: dict, mode: str) -> str:
    if run(["git", "status", "--porcelain"]):
        raise ReleaseError("Working tree must be clean")
    base = config["branches"]["development" if mode == "release" else "stable"]
    if current_branch() != base:
        raise ReleaseError(f"Preparation must start on {base}")
    run(["git", "fetch", "origin", "--prune", "--tags"])
    local = run(["git", "rev-parse", base])
    remote = run(["git", "rev-parse", f"origin/{base}"])
    if local != remote:
        raise ReleaseError(f"{base} must exactly match origin/{base}")
    return base


def create_placeholders(version: Version, config: dict) -> None:
    marker = config["placeholderMarkers"][0]
    for path in changelog_paths(version, config):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"{marker} ({version.name})\n", encoding="utf-8")


def prepare(args: argparse.Namespace) -> None:
    config = load_config()
    current = read_version()
    candidate = next_version(current, args.bump, args.version)
    mode = "hotfix" if args.hotfix else "release"
    branches = config["branches"]
    branch = f'{branches[mode + "Prefix"]}{candidate.name}'
    base = branches["stable" if mode == "hotfix" else "development"]
    summary = {"base": base, "branch": branch, "versionName": candidate.name, "versionCode": candidate.code}
    if args.dry_run:
        conflicts = candidate_conflicts(candidate, config, mode)
        validate_history(candidate, config)
        if conflicts:
            raise ReleaseError("Release candidate already exists: " + ", ".join(conflicts))
        print(json.dumps(summary, indent=2))
        return
    if not shutil.which("gh"):
        raise ReleaseError("GitHub CLI (gh) is required before preparation can mutate the repository")
    ensure_clean_current_base(config, mode)
    conflicts = candidate_conflicts(candidate, config, mode)
    validate_history(candidate, config)
    if conflicts:
        raise ReleaseError("Release candidate already exists: " + ", ".join(conflicts))
    run(["git", "switch", "-c", branch])
    write_version(candidate)
    create_placeholders(candidate, config)
    run(["git", "add", "app/build.gradle", config["metadataRoot"]])
    run(["git", "commit", "-m", f"Prepare release {candidate.name}"])
    run(["git", "push", "-u", "origin", branch])
    checklist = "\n".join([
        "## Release checklist", "", "- [ ] Localized release notes completed",
        "- [ ] Release validation passes", "- [ ] Release APK and AAB assemble successfully",
    ])
    run(["gh", "pr", "create", "--base", branches["stable"], "--head", branch,
         "--title", f"Release {candidate.name}", "--body", checklist])
    print(json.dumps(summary, indent=2))


def metadata(args: argparse.Namespace) -> None:
    config = load_config()
    version = read_version()
    print(json.dumps({
        "versionName": version.name,
        "versionCode": version.code,
        "tag": config["tagPrefix"] + version.name,
        "locales": discover_locales(config),
        "changelogs": [str(path.relative_to(ROOT)) for path in changelog_paths(version, config)],
    }, indent=2))


def validate(args: argparse.Namespace) -> None:
    config = load_config()
    version = read_version()
    manifest = MANIFEST_PATH.read_text(encoding="utf-8")
    if "android:versionCode" in manifest or "android:versionName" in manifest:
        raise ReleaseError("Manifest contains competing Android version declarations")
    validate_history(version, config, require_absent_tag=not args.allow_existing_tag)
    validate_changelogs(version, config)
    if not args.skip_branch:
        validate_branch(version, config, args.mode)
    if args.check_play:
        if not args.service_account:
            raise ReleaseError("--check-play requires --service-account")
        run([
            "bundle", "exec", "fastlane", "android", "validate_play_version_code",
            f"version_code:{version.code}", f"json_key:{Path(args.service_account).resolve()}",
        ])
    print(json.dumps({"valid": True, "versionName": version.name, "versionCode": version.code}))


def verify_build(args: argparse.Namespace) -> None:
    version = read_version()
    verify_build_identity(version, Path(args.manifest), Path(args.metadata))
    print(json.dumps({"valid": True, "versionName": version.name, "versionCode": version.code}))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    metadata_parser = commands.add_parser("metadata")
    metadata_parser.set_defaults(handler=metadata)
    validate_parser = commands.add_parser("validate")
    validate_parser.add_argument("--mode", choices=("release", "hotfix"))
    validate_parser.add_argument("--skip-branch", action="store_true")
    validate_parser.add_argument("--allow-existing-tag", action="store_true")
    validate_parser.add_argument("--check-play", action="store_true")
    validate_parser.add_argument("--service-account")
    validate_parser.set_defaults(handler=validate)
    verify_parser = commands.add_parser("verify-build")
    verify_parser.add_argument("--manifest", required=True)
    verify_parser.add_argument("--metadata", required=True)
    verify_parser.set_defaults(handler=verify_build)
    prepare_parser = commands.add_parser("prepare")
    group = prepare_parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--bump", choices=("patch", "minor", "major"))
    group.add_argument("--version")
    prepare_parser.add_argument("--hotfix", action="store_true")
    prepare_parser.add_argument("--dry-run", action="store_true")
    prepare_parser.set_defaults(handler=prepare)
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    try:
        args = build_parser().parse_args(argv)
        args.handler(args)
        return 0
    except (ReleaseError, OSError, json.JSONDecodeError) as error:
        print(f"release-tool: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
