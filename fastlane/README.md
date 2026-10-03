# TextPad Fastlane lanes

Install the pinned Ruby dependencies from the repository root:

```sh
bundle install
```

Release automation normally invokes these lanes from GitHub Actions. Each release lane requires explicit artifact, metadata, credential, track, or version inputs so a local invocation cannot silently publish using defaults.

## Android lanes

### `android test`

Runs Gradle unit tests:

```sh
bundle exec fastlane android test
```

### `android beta`

Legacy Crashlytics Beta lane retained for compatibility. It is not part of the GitHub/Google Play release process.

### `android validate_play_version_code`

Fails if the supplied `versionCode` already exists on Internal, Alpha, Beta, or Production:

```sh
bundle exec fastlane android validate_play_version_code \
  version_code:63 json_key:/absolute/path/service-account.json
```

### `android assert_track_version_code`

Requires the supplied version code to exist on one explicit Play track:

```sh
bundle exec fastlane android assert_track_version_code \
  version_code:63 track:internal json_key:/absolute/path/service-account.json
```

### `android upload_release`

Uploads one already-built signed APK and localized metadata to an explicit track. The AAB path is validated and retained by the GitHub release workflow, but `skip_upload_aab` remains enabled because this existing Play application is not enrolled in Play App Signing.

```sh
bundle exec fastlane android upload_release \
  apk:/absolute/path/TextPad-v1.31.2.apk \
  aab:/absolute/path/TextPad-v1.31.2.aab \
  version_code:63 \
  metadata_path:/absolute/path/fastlane/metadata/android \
  json_key:/absolute/path/service-account.json \
  track:internal
```

### `android promote_release`

Promotes an existing Internal version to a completed Production release without rebuilding, resigning, or staged rollout. The workflow derives `versionCode` from the immutable release tag before calling this lane.

```sh
bundle exec fastlane android promote_release \
  version_code:63 json_key:/absolute/path/service-account.json
```

The end-to-end maintainer process and recovery paths are documented in [the canonical release guide](../docs/RELEASING.md).
