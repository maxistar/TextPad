package com.maxistar.textpad.workspace;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class SharedPreferencesWorkspaceRegistryStore
        implements WorkspaceRegistryStore {
    private static final String KEY = "workspace_folders";

    private final SharedPreferences preferences;

    public SharedPreferencesWorkspaceRegistryStore(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
    }

    @Override
    public String read() {
        return preferences.getString(KEY, "");
    }

    @Override
    public void write(String value) {
        preferences.edit().putString(KEY, value).apply();
    }
}
