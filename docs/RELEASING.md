# Android release process

This is the canonical maintainer guide for TextPad Android releases. The release automation keeps one immutable identity across Gradle, the Git tag, GitHub Release assets, Google Play, and F-Droid.

## Release identity and invariants

- `app/build.gradle` is the source of truth for `versionName` and `versionCode`.
- Release tags are annotated and use `v<versionName>`. Never move or reuse a tag or version code.
- Every Fastlane locale contains `changelogs/<versionCode>.txt` before merge.
- The signed APK and AAB are built once from the tag and published with `SHA256SUMS`.
- Only the APK is uploaded to Google Play because the existing application is not enrolled in Play App Signing. The AAB remains a GitHub Release artifact.
- Production promotion reuses the version already uploaded to Internal; it never rebuilds or resigns the app.
- F-Droid treats the immutable source tag as the canonical release boundary and builds independently.

## Normal release

Start from a clean `dev` that exactly matches `origin/dev`. Preview the next identity:

```sh
python3 scripts/release_tool.py prepare --bump patch --dry-run
python3 scripts/release_tool.py prepare --bump minor --dry-run
python3 scripts/release_tool.py prepare --bump major --dry-run
python3 scripts/release_tool.py prepare --version 2.0.0 --dry-run
```

Remove `--dry-run` to create and push `release/<versionName>`, increment the Gradle version, create localized changelog placeholders, commit them, and open a pull request to `master`:

```sh
python3 scripts/release_tool.py prepare --bump patch
```

Replace every `TODO: RELEASE NOTES` placeholder with reviewed user-facing text. English becomes the GitHub Release body; every configured locale is uploaded to Google Play. The release PR must pass the `build` and `validate` checks and must be merged with a merge commit.

Merging the validated PR is the release command. No standard Create/Sync dispatch is required:

```text
dev → release/x.y.z → merge commit on master
                              │
                              ▼
                   validate identity and Play
                              │
                              ▼
                         tag vX.Y.Z
                              │
                              ▼
              signed APK/AAB + checksums
                              │
                              ▼
            GitHub Release → Play Internal
                              │
                  ┌───────────┴───────────┐
                  ▼                       ▼
        merge current master → dev   Production approval
                                          │
                                          ▼
                              promote existing APK
```

The synchronization job does not wait for production approval. Review the Internal release, then approve the pending `production` deployment in GitHub Actions. Delete the release branch only after synchronization succeeds.

## Hotfix

Start from a clean `master` that exactly matches `origin/master`:

```sh
python3 scripts/release_tool.py prepare --bump patch --hotfix --dry-run
python3 scripts/release_tool.py prepare --bump patch --hotfix
```

This creates `hotfix/<versionName>` from the published line and opens a PR to `master`. The same merge-triggered pipeline publishes it and synchronizes `master` back into `dev`; unreleased `dev` work is not part of the tagged artifact.

```text
master → hotfix/x.y.z → master → tag → Internal
                            └──────────────→ dev
```

## Repository protection audit

The workflow is safe to activate only when the live GitHub settings match `release-protection.json`:

```sh
python3 scripts/release_protection.py audit
```

The expected settings are:

- `master`: pull requests required; checks `build` and `validate` required from GitHub Actions app `15368`; force pushes and deletion disabled.
- `dev`: force pushes and deletion disabled. Ordinary fast-forward pushes remain allowed so the narrowly scoped synchronization workflow can push its merge commit; this is the recorded synchronization exception.
- `release`: deployments limited to the `master` branch and `v*` tags.
- `production`: deployments limited to the same refs and approval required from `maxistar`.

The production environment rule—not the YAML environment name by itself—is the human publication gate. Do not remove its required reviewer while automatic orchestration is enabled.

## Manual recovery

Recovery workflows recompute identity from repository state or an immutable tag. They must never be used to move a tag or reuse a version code.

### Preparation stopped before creating a branch

Fix the reported preflight condition and rerun `prepare`. It is safe because no mutation occurred.

### Candidate branch or PR already exists

Continue editing and validating the existing candidate. Do not rerun preparation to create a competing branch.

### Release PR merged but tag creation failed

Run **Create Android Release** manually with the merged `release/*` or `hotfix/*` head branch and exact merge commit SHA. The workflow derives the version and refuses a non-merge, divergent, mismatched, or already-tagged candidate.

### Tag exists but GitHub or Internal publication failed

Run **Publish Tagged Android Release** with the existing tag. Complete GitHub assets are downloaded and checksum-verified; the tag is never moved. If a partial GitHub Release is invalid, inspect it before retrying rather than replacing source identity.

### Internal upload succeeded but synchronization failed

Resolve the `master`/`dev` conflict without force-pushing, then run **Sync Released Master to Dev** with the release tag. Do not delete the candidate branch until the sync succeeds.

### Production approval was delayed, rejected, or the job expired

Run **Promote Android Production** with the tag only. The workflow derives `versionCode`, requires that exact code on Internal, and promotes it without a build or Android keystore.

### Published artifact is invalid

Do not roll back by reusing an old code. Prepare a corrected release with a higher `versionCode`. Never move the published tag.

## Safe verification

Run repository-owned checks before changing live settings or merging release automation:

```sh
python3 -m unittest \
  scripts/test_release_tool.py \
  scripts/test_release_workflows.py \
  scripts/test_release_protection.py

python3 scripts/release_tool.py prepare --bump patch --dry-run
python3 scripts/release_tool.py prepare --bump minor --dry-run
python3 scripts/release_tool.py prepare --bump major --dry-run
python3 scripts/release_tool.py prepare --version 2.0.0 --dry-run
python3 scripts/release_tool.py prepare --bump patch --hotfix --dry-run
```

A non-publishing approval rehearsal may dispatch **Promote Android Production** for an existing tag and cancel the run while it is waiting for `production` approval. Do not approve that rehearsal; verify that no tag, GitHub Release, or Play track changes, then cancel it.

## First real release observation checklist

- [ ] The merged PR head is `release/<versionName>` or `hotfix/<versionName>` and the tagged commit is its merge commit.
- [ ] Exactly one annotated `v<versionName>` tag exists.
- [ ] GitHub Release contains APK, AAB, and a passing `SHA256SUMS` file from that tag.
- [ ] Google Play Internal contains the same `versionCode` and localized notes.
- [ ] `master` was synchronized into `dev` without a force push.
- [ ] Production remained unchanged until an authorized reviewer approved the deployment.
- [ ] Production received the existing Internal version without a Gradle build or keystore access.
- [ ] Manual publish, sync, and promotion recovery inputs require only immutable or derived identity.

## Emergency override

Repository administrators own emergency overrides. Record the reason and retain the same invariants: reviewed source, unique version code, immutable annotated tag, signed checksummed artifacts, Internal-before-Production progression, and `master`-to-`dev` synchronization.
