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
        self.assertIn("scripts/test_release_workflows.py", workflow)
        self.assertIn("scripts/test_release_protection.py", workflow)

    def test_orchestrator_only_accepts_merged_release_candidates(self):
        workflow = self.read("create-release.yml")
        self.assertIn("pull_request:", workflow)
        self.assertIn("types: [closed]", workflow)
        self.assertIn("github.event.pull_request.merged == true", workflow)
        self.assertIn("startsWith(github.event.pull_request.head.ref, 'release/')", workflow)
        self.assertIn("startsWith(github.event.pull_request.head.ref, 'hotfix/')", workflow)
        self.assertIn("scripts/release_tool.py candidate", workflow)
        self.assertIn('gh pr checks "$PR_NUMBER" --required', workflow)
        self.assertIn("--check-play", workflow)

    def test_orchestrator_derives_version_and_creates_annotated_tag(self):
        workflow = self.read("create-release.yml")
        dispatch_inputs = workflow.split("workflow_dispatch:", 1)[1].split("permissions:", 1)[0]
        self.assertIn("head_branch:", dispatch_inputs)
        self.assertIn("commit_sha:", dispatch_inputs)
        self.assertNotIn("version:", dispatch_inputs)
        self.assertIn('git tag -a "$TAG" "$MERGE_COMMIT"', workflow)
        self.assertIn('git push origin "refs/tags/$TAG"', workflow)
        self.assertIn("environment: release", workflow)

    def test_orchestrator_reuses_publish_sync_and_promotion_workflows(self):
        workflow = self.read("create-release.yml")
        self.assertIn("uses: ./.github/workflows/tagged-release.yml", workflow)
        self.assertIn("uses: ./.github/workflows/sync-master-to-dev.yml", workflow)
        self.assertIn("uses: ./.github/workflows/promote-production.yml", workflow)
        self.assertEqual(2, workflow.count("needs: [create-tag, publish]"))
        self.assertLess(workflow.index("  publish:"), workflow.index("  sync-development:"))
        self.assertLess(workflow.index("  publish:"), workflow.index("  promote-production:"))

    def test_tagged_publication_orders_github_before_play_and_is_recoverable(self):
        workflow = self.read("tagged-release.yml")
        github = workflow.index("gh release create")
        play = workflow.index("fastlane android upload_release")
        self.assertLess(github, play)
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("workflow_call:", workflow)
        self.assertIn('if [ -n "$INPUT_TAG" ]', workflow)
        self.assertIn("sha256sum --check SHA256SUMS", workflow)
        self.assertIn("environment: release", workflow)
        self.assertIn('gh release view "$TAG"', workflow)
        self.assertIn('gh release download "$TAG"', workflow)
        self.assertIn('echo "complete=true"', workflow)

    def test_production_is_reusable_tag_only_and_has_no_build_or_keystore(self):
        workflow = self.read("promote-production.yml")
        dispatch_inputs = workflow.split("workflow_dispatch:", 1)[1].split("workflow_call:", 1)[0]
        self.assertIn("tag:", dispatch_inputs)
        self.assertNotIn("version_code:", dispatch_inputs)
        self.assertIn("workflow_call:", workflow)
        self.assertIn("environment: production", workflow)
        self.assertIn("steps.identity.outputs.version_code", workflow)
        self.assertIn("assert_track_version_code", workflow)
        self.assertIn("promote_release", workflow)
        self.assertNotIn("gradlew", workflow)
        self.assertNotIn("ANDROID_KEYSTORE", workflow)

    def test_master_sync_is_reusable_and_accepts_post_tag_release_fixes(self):
        workflow = self.read("sync-master-to-dev.yml")
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("workflow_call:", workflow)
        self.assertIn('test "$(git cat-file -t "$TAG")" = "tag"', workflow)
        self.assertIn('git merge-base --is-ancestor "$TAG^{}" origin/master', workflow)
        self.assertIn("git merge --no-ff origin/master", workflow)
        self.assertNotIn('git push --force', workflow)

    def test_release_permissions_keep_trust_boundaries(self):
        release_pr = self.read("release-pr.yml")
        orchestrator = self.read("create-release.yml")
        production = self.read("promote-production.yml")
        self.assertIn("contents: read", release_pr)
        self.assertIn("contents: write", orchestrator)
        self.assertIn("pull-requests: read", orchestrator)
        self.assertIn("checks: read", orchestrator)
        self.assertIn("contents: read", production)
        self.assertNotIn("ANDROID_KEYSTORE", production)

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
        self.assertIn("apk: apk", fastfile)
        self.assertIn("skip_upload_aab: true", fastfile)
        self.assertIn('track_promote_release_status: "completed"', fastfile)
        self.assertNotIn("rollout:", fastfile)

    def test_fastlane_upload_and_promotion_are_idempotent(self):
        fastfile = (ROOT / "fastlane" / "Fastfile").read_text(encoding="utf-8")
        self.assertIn("if existing.include?(version_code)", fastfile)
        self.assertIn("upload is complete", fastfile)
        self.assertIn("if production_codes.include?(version_code)", fastfile)
        self.assertIn("promotion is complete", fastfile)

    def test_fastlane_handles_empty_google_play_tracks(self):
        gemfile = (ROOT / "Gemfile").read_text(encoding="utf-8")
        lockfile = (ROOT / "Gemfile.lock").read_text(encoding="utf-8")
        self.assertIn('gem "fastlane", "2.235.0"', gemfile)
        self.assertIn('gem "multi_json", "~> 1.15"', gemfile)
        self.assertIn("fastlane (2.235.0)", lockfile)
        self.assertIn("multi_json (", lockfile)
        for name in ("create-release.yml", "tagged-release.yml", "promote-production.yml"):
            self.assertIn("ruby-version: '3.3'", self.read(name))

    def test_legacy_direct_production_deploy_is_removed(self):
        self.assertFalse((WORKFLOWS / "android-deploy.yml").exists())
        fastfile = (ROOT / "fastlane" / "Fastfile").read_text(encoding="utf-8")
        self.assertNotIn("ALLOW_LEGACY_PRODUCTION", fastfile)


if __name__ == "__main__":
    unittest.main()
