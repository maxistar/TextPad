import json
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
import release_tool


class ReleaseToolTest(unittest.TestCase):
    def test_reads_single_gradle_version(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "build.gradle"
            path.write_text('defaultConfig {\n versionCode 60\n versionName "1.30.0"\n}\n')
            self.assertEqual(release_tool.Version("1.30.0", 60), release_tool.read_version(path))

    def test_rejects_competing_gradle_versions(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "build.gradle"
            path.write_text('versionCode 60\nversionCode 61\nversionName "1.30.0"\n')
            with self.assertRaises(release_tool.ReleaseError):
                release_tool.read_version(path)

    def test_semver_bumps_increment_code_once(self):
        current = release_tool.Version("1.30.9", 60)
        self.assertEqual(release_tool.Version("1.30.10", 61), release_tool.next_version(current, "patch", None))
        self.assertEqual(release_tool.Version("1.31.0", 61), release_tool.next_version(current, "minor", None))
        self.assertEqual(release_tool.Version("2.0.0", 61), release_tool.next_version(current, "major", None))
        self.assertEqual(release_tool.Version("3.2.1", 61), release_tool.next_version(current, None, "3.2.1"))

    def test_rejects_invalid_or_non_increasing_semver(self):
        current = release_tool.Version("1.30.0", 60)
        for value in ("1.30", "v1.31.0", "01.31.0", "1.29.0", "1.30.0"):
            with self.subTest(value=value), self.assertRaises(release_tool.ReleaseError):
                release_tool.next_version(current, None, value)

    def test_locale_discovery_and_changelog_validation(self):
        config = {
            "metadataRoot": "metadata", "requiredLocales": ["en-US", "ru-RU"],
            "ignoredLocales": [], "placeholderMarkers": ["TODO: RELEASE NOTES"],
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for locale in config["requiredLocales"]:
                base = root / "metadata" / locale
                base.mkdir(parents=True)
                (base / "title.txt").write_text("Title")
                changes = base / "changelogs"
                changes.mkdir()
                (changes / "61.txt").write_text("A useful note")
            self.assertEqual(["en-US", "ru-RU"], release_tool.discover_locales(config, root))
            release_tool.validate_changelogs(release_tool.Version("1.31.0", 61), config, root)
            (root / "metadata/en-US/changelogs/61.txt").write_text("TODO: RELEASE NOTES")
            with self.assertRaises(release_tool.ReleaseError):
                release_tool.validate_changelogs(release_tool.Version("1.31.0", 61), config, root)

    def test_release_config_is_machine_readable_and_complete(self):
        config = release_tool.load_config()
        self.assertEqual("dev", config["branches"]["development"])
        self.assertEqual("master", config["branches"]["stable"])
        self.assertEqual("v", config["tagPrefix"])
        self.assertTrue(config["requiredLocales"])
        self.assertTrue(config["placeholderMarkers"])

    def test_manifest_has_no_competing_version(self):
        manifest = release_tool.MANIFEST_PATH.read_text(encoding="utf-8")
        self.assertNotIn("android:versionCode", manifest)
        self.assertNotIn("android:versionName", manifest)

    def test_build_outputs_match_gradle_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "AndroidManifest.xml"
            metadata = root / "output-metadata.json"
            manifest.write_text(
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                'android:versionCode="60" android:versionName="1.30.0"/>', encoding="utf-8"
            )
            metadata.write_text(json.dumps({"elements": [{"versionCode": 60, "versionName": "1.30.0"}]}))
            release_tool.verify_build_identity(release_tool.Version("1.30.0", 60), manifest, metadata)
            metadata.write_text(json.dumps({"elements": [{"versionCode": 59, "versionName": "1.29.2"}]}))
            with self.assertRaises(release_tool.ReleaseError):
                release_tool.verify_build_identity(release_tool.Version("1.30.0", 60), manifest, metadata)

    @mock.patch.object(release_tool, "version_from_tag")
    @mock.patch.object(release_tool, "run")
    @mock.patch.object(release_tool, "git_ref_exists")
    def test_history_rejects_duplicate_tag_and_code(self, ref_exists, run, from_tag):
        config = {"tagPrefix": "v"}
        ref_exists.return_value = True
        with self.assertRaises(release_tool.ReleaseError):
            release_tool.validate_history(release_tool.Version("1.31.0", 61), config)
        ref_exists.return_value = False
        run.return_value = "v1.30.0"
        from_tag.return_value = release_tool.Version("1.30.0", 61)
        with self.assertRaises(release_tool.ReleaseError):
            release_tool.validate_history(release_tool.Version("1.31.0", 61), config)

    @mock.patch.object(release_tool.subprocess, "run")
    @mock.patch.object(release_tool, "current_branch")
    def test_branch_validation_rejects_incorrect_ancestry(self, branch, process):
        branch.return_value = "release/1.31.0"
        process.return_value = SimpleNamespace(returncode=1)
        config = {"branches": {"development": "dev", "stable": "master", "releasePrefix": "release/", "hotfixPrefix": "hotfix/"}}
        with self.assertRaises(release_tool.ReleaseError):
            release_tool.validate_branch(release_tool.Version("1.31.0", 61), config)

    @mock.patch.object(release_tool, "run")
    @mock.patch.object(release_tool, "validate_changelogs")
    @mock.patch.object(release_tool, "validate_history")
    @mock.patch.object(release_tool, "read_version")
    @mock.patch.object(release_tool, "load_config")
    def test_protected_validation_passes_code_to_fastlane(self, config, version, history, notes, run):
        config.return_value = {}
        version.return_value = release_tool.Version("1.31.0", 61)
        args = SimpleNamespace(
            allow_existing_tag=False, skip_branch=True, mode=None,
            check_play=True, service_account="service.json",
        )
        release_tool.validate(args)
        command = run.call_args.args[0]
        self.assertIn("validate_play_version_code", command)
        self.assertIn("version_code:61", command)


if __name__ == "__main__":
    unittest.main()
