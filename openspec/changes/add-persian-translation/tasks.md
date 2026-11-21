## 1. Add Persian locale resources

- [ ] 1.1 Prepare and review Persian translations for the app's user-facing strings, using the project's Crowdin workflow where available.
- [ ] 1.2 Add the reviewed Persian strings in `app/src/main/res/values-fa/strings.xml`, preserving resource names and formatting placeholders from the default strings.
- [ ] 1.3 Add `فارسی` and locale code `fa` at matching positions in the language picker arrays.

## 2. Verify Persian and RTL behavior on Android

- [ ] 2.1 Build and launch the app in the Android emulator or on a connected physical device in the implementation environment.
- [ ] 2.2 Select Persian in settings and confirm the selection persists and Persian strings are loaded.
- [ ] 2.3 Inspect the settings and editor screens for correct right-to-left direction, readable text, and controls without clipping; make only necessary RTL configuration or layout adjustments.
- [ ] 2.4 Recheck the settings and editor screens after any RTL adjustments on the emulator or physical device.

## 3. Verify project integration

- [ ] 3.1 Run the project build and relevant localization/resource checks, resolving any resource or language-picker errors.
