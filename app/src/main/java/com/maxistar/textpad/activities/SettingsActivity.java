package com.maxistar.textpad.activities;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.SettingsService;
import com.maxistar.textpad.TPStrings;
import com.maxistar.textpad.service.AlternativeUrlsService;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    Preference mVersion;

    SettingsService settingsService;

    AlternativeUrlsService alternativeUrlsService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsService = ServiceLocator.getInstance().getSettingsService(getApplicationContext());
        alternativeUrlsService = ServiceLocator.getInstance().getAlternativeUrlsService();
        addPreferencesFromResource(R.xml.preferences);

        if (hideLegacyPicker()) {
            Preference legacyPicker = this.findPreference(SettingsService.SETTING_LEGASY_FILE_PICKER);
            legacyPicker.setEnabled(false);
        }

        //get default value for count of files

        mVersion = this.findPreference(TPStrings.VERSION_NAME);
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersion.setSummary(pInfo.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        ListPreference encoding = (ListPreference)this.findPreference(SettingsService.SETTING_FILE_ENCODING);

        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entry_values = new ArrayList<CharSequence>();

        Map<String, Charset> avmap = Charset.availableCharsets();
        for(String name : avmap.keySet()) {
            entries.add(avmap.get(name).name());
            entry_values.add(avmap.get(name).displayName());
        }

        CharSequence[] entries_arr = new CharSequence[entries.size()];
        CharSequence[] entry_values_arr = new CharSequence[entry_values.size()];

        encoding.setEntries(entries.toArray(entries_arr));
        encoding.setEntryValues(entry_values.toArray(entry_values_arr));
        
        initAlternativeLocations();
    }

    private void initAlternativeLocations() {
        final Preference resetAlternativeLocations = this.findPreference("reset_alternative_file_paths");
        final Preference useAlternativeLocations = this.findPreference("use_alternative_file_access");

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            resetAlternativeLocations.setEnabled(false);
            useAlternativeLocations.setEnabled(false);
        }

        resetAlternativeLocations.setEnabled(settingsService.isAlternativeFileAccess());
        resetAlternativeLocations.setOnPreferenceClickListener(
                preference -> {
                    resetAlternativeLocations();
                    return false;
                }
        );

        useAlternativeLocations.setOnPreferenceClickListener(
                preference -> {
                    resetAlternativeLocations.setEnabled(settingsService.isAlternativeFileAccess());
                    return true;
                }
        );
    }

    void resetAlternativeLocations() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.AlternativeFileAccessTitle)
                .setMessage(R.string.ResetAlternativeFileLocations)
                .setPositiveButton(R.string.Yes, (arg0, arg1) -> alternativeUrlsService.clearAlternativeUrls(getApplicationContext()))
                .create()
                .show();
    }

    boolean hideLegacyPicker() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        }

        return Build.VERSION.SDK_INT > Build.VERSION_CODES.P;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Setup the initial values
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set up a listener whenever a key changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        settingsService.reloadSettings(this.getApplicationContext());
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsService.SETTING_LANGUAGE.equals(key) ||
            SettingsService.SETTING_FONT.equals(key) ||
            SettingsService.SETTING_BG_COLOR.equals(key) ||
            SettingsService.SETTING_FONT_COLOR.equals(key) ||
            SettingsService.SETTING_FONT_SIZE.equals(key)
        ) {
            String lang = sharedPreferences.getString(SettingsService.SETTING_LANGUAGE, TPStrings.EN);
            setLocale(lang);
            SettingsService.setLanguageChangedFlag();
        }
        settingsService.reloadSettings(this.getApplicationContext());
    }


    public void setLocale(String lang) {
        Locale locale2 = new Locale(lang);
        Locale.setDefault(locale2);
        Configuration config2 = new Configuration();
        config2.locale = locale2;

        getBaseContext().getResources().updateConfiguration(config2, null);
        showPreferences();
    }

    protected void showPreferences(){
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}