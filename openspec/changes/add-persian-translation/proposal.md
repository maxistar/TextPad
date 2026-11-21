## Why

Persian-speaking users currently cannot choose Persian for the app interface, even though the project welcomes community translations and already supports a manually selectable set of locales. Adding Persian makes the editor accessible to these users and gives the interested contributor a clear path to help.

## What Changes

- Add a Persian (`fa`) interface translation using Android's `values-fa` resources.
- Add Persian/Farsi to the app's language selector.
- Verify the Persian interface and right-to-left rendering on an Android emulator or physical device.

## Capabilities

### New Capabilities
- `app-localization`: Available interface languages can be selected in settings, including Persian.

### Modified Capabilities

## Impact

- Android string resources under `app/src/main/res/values-fa/`.
- Language selector entries in `app/src/main/res/values/arrays.xml`.
- Android RTL configuration/layouts if device verification shows that Persian is not laid out correctly.
- Manual RTL verification of the settings and editor screens on an emulator or physical Android device.
