# TextPad E2E Tests

External installed-app smoke tests for TextPad using TypeScript, Cucumber, Appium 2, and WebdriverIO.

## Scope

These tests exercise TextPad as an installed Android app. They complement, but do not replace, the Gradle tests under:

- `TextPad/app/src/test`: fast JVM/unit tests for app logic
- `TextPad/app/src/androidTest`: Android instrumentation and Espresso tests

Use this harness for release smoke flows where the real installed app, Android UI, and device storage matter.

## Project Layout

```text
scripts/
  package.json
  tsconfig.json
  cucumber.js
  features/
  src/
    drivers/
    pages/
    steps/
    support/
    world/
  artifacts/   generated reports, screenshots, and page sources
  dist/        generated compiled JavaScript
```

## Prerequisites

- Node.js 20+
- Appium 2 server already running
- Android device or emulator already running
- TextPad already installed on the target
- Appium UiAutomator2 driver installed on the Appium server

This project does not start Appium, boot emulators, or install TextPad for you.

Device-storage cleanup uses Appium's `mobile: shell` command when available. If your Appium server blocks shell commands, start it with relaxed security or rely on generated test filenames:

```sh
appium --relaxed-security
```

## Setup

```sh
cd TextPad/scripts
npm install
```

Optional local configuration can be placed in `.env`:

```sh
APPIUM_SERVER_URL=http://127.0.0.1:4723
APPIUM_WAIT_TIMEOUT_MS=20000

ANDROID_DEVICE_NAME=Android
ANDROID_UDID=
ANDROID_PLATFORM_VERSION=
ANDROID_AUTOMATION_NAME=UiAutomator2

TEXTPAD_APP_PACKAGE=com.maxistar.textpad
TEXTPAD_APP_ACTIVITY=com.maxistar.textpad.activities.EditorActivity
TEXTPAD_E2E_TEST_FILE_DIR=/sdcard/Download
TEXTPAD_E2E_TEST_FILE_PREFIX=textpad-e2e-save
```

`TEXTPAD_E2E_TEST_FILE_DIR` must match where Android's document picker saves the test file. The default assumes the picker creates files in Downloads.

## Commands

```sh
cd TextPad/scripts
npm run build
npm run test:smoke
```

`npm run test:smoke` compiles the TypeScript sources and runs the save-file smoke feature.

## Smoke Test Behavior

The save-file smoke scenario:

1. Terminates any already-running TextPad instance.
2. Opens the installed TextPad app.
3. Waits for the editor to be ready.
4. Enters predictable test text.
5. Opens the Save action through the overflow menu.
6. Saves with a generated filename using the Android document picker or legacy TextPad file dialog.
7. Verifies the editor remains available after saving.
8. Reads the saved file from the configured device-storage path and verifies the content.
9. Stops TextPad before closing the Appium session.

Generated filenames use the configured prefix and a timestamp so repeated local runs do not fail only because a previous file exists.

## Diagnostics

Generated diagnostics are written under `TextPad/scripts/artifacts/`:

- `cucumber-report.html`: readable scenario and step summary
- `save-flow/<timestamp>/*.png`: screenshots captured on failure
- `save-flow/<timestamp>/*.xml`: Appium page source dumps captured on failure

The `artifacts/` and `dist/` directories are generated output and are ignored by Git.
