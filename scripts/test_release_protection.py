import unittest

from pathlib import Path
import sys


sys.path.insert(0, str(Path(__file__).resolve().parent))
import release_protection


class ReleaseProtectionTest(unittest.TestCase):
    def setUp(self):
        self.config = {
            "repository": "maxistar/TextPad",
            "branches": {
                "master": {
                    "requirePullRequest": True,
                    "requiredChecks": ["build", "validate"],
                    "requiredCheckAppId": 15368,
                    "allowForcePushes": False,
                    "allowDeletions": False,
                },
                "dev": {
                    "requirePullRequest": False,
                    "requiredChecks": [],
                    "allowForcePushes": False,
                    "allowDeletions": False,
                },
            },
            "environments": {
                "release": {
                    "allowedRefs": [
                        {"name": "master", "type": "branch"},
                        {"name": "v*", "type": "tag"},
                    ],
                    "requiredReviewers": [],
                },
                "production": {
                    "allowedRefs": [
                        {"name": "master", "type": "branch"},
                        {"name": "v*", "type": "tag"},
                    ],
                    "requiredReviewers": ["maxistar"],
                },
            },
        }

    def test_accepts_compliant_snapshot(self):
        self.assertEqual([], release_protection.audit_snapshot(self.snapshot(), self.config))

    def test_reports_absent_branch_protection(self):
        snapshot = self.snapshot()
        del snapshot["branches"]["dev"]
        self.assertIn(
            "branch dev has no protection",
            release_protection.audit_snapshot(snapshot, self.config),
        )

    def test_reports_missing_required_check(self):
        snapshot = self.snapshot()
        snapshot["branches"]["master"]["required_status_checks"]["contexts"] = ["build"]
        self.assertIn(
            "branch master is missing required checks: validate",
            release_protection.audit_snapshot(snapshot, self.config),
        )

    def test_reports_required_check_from_wrong_app(self):
        snapshot = self.snapshot()
        snapshot["branches"]["master"]["required_status_checks"]["checks"][1]["app_id"] = None
        self.assertIn(
            "branch master checks are not pinned to app 15368: validate",
            release_protection.audit_snapshot(snapshot, self.config),
        )

    def test_reports_unprotected_environment(self):
        snapshot = self.snapshot()
        snapshot["environments"]["release"]["environment"]["deployment_branch_policy"] = None
        self.assertIn(
            "environment release does not restrict deployment refs",
            release_protection.audit_snapshot(snapshot, self.config),
        )

    def test_reports_missing_production_reviewer(self):
        snapshot = self.snapshot()
        snapshot["environments"]["production"]["environment"]["protection_rules"] = []
        self.assertIn(
            "environment production is missing required reviewers: maxistar",
            release_protection.audit_snapshot(snapshot, self.config),
        )

    @staticmethod
    def branch(required_checks, require_pull_request=False):
        return {
            "required_status_checks": {
                "contexts": required_checks,
                "checks": [
                    {"context": context, "app_id": 15368}
                    for context in required_checks
                ],
            },
            "required_pull_request_reviews": (
                {"required_approving_review_count": 0}
                if require_pull_request else None
            ),
            "allow_force_pushes": {"enabled": False},
            "allow_deletions": {"enabled": False},
        }

    @staticmethod
    def environment(required_reviewer=None):
        rules = []
        if required_reviewer:
            rules.append({
                "type": "required_reviewers",
                "reviewers": [{
                    "type": "User",
                    "reviewer": {"login": required_reviewer},
                }],
            })
        return {
            "environment": {
                "deployment_branch_policy": {
                    "protected_branches": False,
                    "custom_branch_policies": True,
                },
                "protection_rules": rules,
            },
            "policies": {
                "branch_policies": [
                    {"name": "master", "type": "branch"},
                    {"name": "v*", "type": "tag"},
                ]
            },
        }

    def snapshot(self):
        return {
            "branches": {
                "master": self.branch(["build", "validate"], require_pull_request=True),
                "dev": self.branch([]),
            },
            "environments": {
                "release": self.environment(),
                "production": self.environment("maxistar"),
            },
        }


if __name__ == "__main__":
    unittest.main()
