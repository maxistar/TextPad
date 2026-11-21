## Context

The app stores localized strings in Android resource directories such as `app/src/main/res/values-ar/strings.xml` and `values-hi/strings.xml`. The language picker is maintained manually in the parallel `language_names` and `language_values` arrays in `app/src/main/res/values/arrays.xml`. `SettingsService` applies the selected language using its locale code. Persian has no resource directory or language-picker entry yet. The repository README already directs translators to Crowdin.

The app targets modern Android versions, but the manifest does not currently declare `android:supportsRtl`, and a few older layouts use physical left/right padding. Persian is a right-to-left language, so adding strings alone does not establish that the UI renders correctly.

## Goals / Non-Goals

**Goals:**
- Make Persian (`fa`) selectable from the app's language settings.
- Provide Persian translations for the app's user-facing strings using Android's `values-fa` resources.
- Confirm that the settings and editor screens render correctly when Persian is selected, using an Android emulator or physical device.

**Non-Goals:**
- Change the app's default language or its existing translations.
- Redesign the UI or add language-selection behavior beyond the existing manual picker.
- Translate store listings or release notes as part of this change.

## Decisions

- Use `values-fa/strings.xml` and the locale code `fa`. This follows Android's language resource convention and matches the existing `values-ar` and `values-hi` pattern; no region-specific locale is needed for this app-level choice.
- Add the display name `فارسی` and code `fa` at corresponding positions in `language_names` and `language_values`. These arrays are parallel and must remain aligned.
- Use the existing Crowdin project as the preferred source for reviewed Persian wording; keep the Android resource file synchronized with the project's established translation workflow.
- Verify RTL behavior on-device before deciding whether manifest or layout changes are needed. If the app does not mirror/directionally arrange the settings and editor correctly, enable Android RTL support and replace affected physical left/right layout attributes with start/end equivalents as needed.

## Risks / Trade-offs

- [Translation completeness or quality may vary] → Use Crowdin and review all user-facing strings before treating the locale as complete.
- [RTL rendering may expose existing layout assumptions] → Check settings and editor on an emulator or physical device and make only the layout/configuration changes needed for correct Persian presentation.
- [Parallel language arrays can become mismatched] → Add the Persian display name and locale code together and verify they occupy matching positions.
