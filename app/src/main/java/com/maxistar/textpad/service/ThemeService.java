package com.maxistar.textpad.service;

import android.app.UiModeManager;
import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeService {

    SettingsService settingsService;

    public ThemeService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void applyColorTheme(Context context) {
        String theme = settingsService.getColorThemeType();
        if (SettingsService.COLOR_THEME_DARK.equals(theme)) {
            applyDarkTheme(context);
        }
        if (SettingsService.COLOR_THEME_LIGHT.equals(theme)) {
            applyLightTheme(context);
        }
    }

    private void applyDarkTheme(Context context) {

        UiModeManager uiManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            uiManager.enableCarMode(0);
        }

        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            uiManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
        }
    }

    private void applyLightTheme(Context context) {

        UiModeManager uiManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            uiManager.enableCarMode(0);
        }

        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            uiManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
        }
    }
}
