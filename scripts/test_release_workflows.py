import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github" / "workflows"


class ReleaseWorkflowTest(unittest.TestCase):
    def read(self, name):
        return (WORKFLOWS / name).read_text(encoding="utf-8")

    def test_release_pr_is_read_only_and_has_no_protected_secrets(self):
        workflow = self.read("release-pr.yml")
        self.assertIn("contents: read", workflow)
        self.assertNotIn("secrets.", workflow)
        self.assertNotIn("git tag", workflow)
        self.assertIn("assembleRelease bundleRelease", workflow)

    def test_create_release_checks_exact_master_before_annotated_tag(self):
        workflow = self.read("create-release.yml")
        self.assertIn('git rev-parse origin/master', workflow)
        self.assertIn('git tag -a', workflow)
        self.assertIn('git config user.name "github-actions[bot]"', workflow)
        self.assertIn('git config user.email "41898282+github-actions[bot]@users.noreply.github.com"', workflow)
        self.assertIn("--check-play", workflow)
        self.assertIn("environment: release", workflow)

    def test_tagged_publication_orders_github_before_play(self):
        workflow = self.read("tagged-release.yml")
        github = workflow.index('gh release create')
        play = workflow.index('fastlane android upload_release')
        self.assertLess(github, play)
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn('if [ -n "$INPUT_TAG" ]', workflow)
        self.assertNotIn('EVENT_NAME: ${{ github.event_name }}', workflow)
        self.assertIn("sha256sum --check SHA256SUMS", workflow)
        self.assertIn("environment: release", workflow)

    def test_production_has_approval_but_no_build_or_keystore(self):
        workflow = self.read("promote-production.yml")
        self.assertIn("environment: production", workflow)
        self.assertIn("assert_track_version_code", workflow)
        self.assertIn("promote_release", workflow)
        self.assertNotIn("gradlew", workflow)
        self.assertNotIn("ANDROID_KEYSTORE", workflow)

    def test_actions_are_versioned(self):
        for path in WORKFLOWS.glob("*.yml"):
            for action in re.findall(r"uses:\s*([^\s]+)", path.read_text(encoding="utf-8")):
                if action.startswith("./"):
                    continue
                self.assertRegex(action, r"@v\d+(?:\.\d+\.\d+)?$", path.name)

    def test_fastlane_release_arguments_are_explicit(self):
        fastfile = (ROOT / "fastlane" / "Fastfile").read_text(encoding="utf-8")
        for token in ("aab:", "apk =", "metadata_path:", "json_key:", "track:"):
            self.assertIn(token, fastfile)
        self.assertIn('track_promote_release_status: "completed"', fastfile)
        self.assertNotIn("rollout:", fastfile)

    def test_fastlane_handles_empty_google_play_tracks(self):
        gemfile = (ROOT / "Gemfile").read_text(encoding="utf-8")
        lockfile = (ROOT / "Gemfile.lock").read_text(encoding="utf-8")
        self.assertIn('gem "fastlane", "2.235.0"', gemfile)
        self.assertIn('gem "multi_json", "~> 1.15"', gemfile)
        self.assertIn("fastlane (2.235.0)", lockfile)
        self.assertIn("multi_json (", lockfile)
        for name in (
            "create-release.yml",
            "tagged-release.yml",
            "promote-production.yml",
            "android-deploy.yml",
        ):
            self.assertIn("ruby-version: '3.3'", self.read(name))


if __name__ == "__main__":
    unittest.main()
