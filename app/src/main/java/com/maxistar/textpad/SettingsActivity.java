package com.maxistar.textpad;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	/** Called when the activity is first created. */
	//Preference mCountOfFilesToRemember;
	
	Preference mVersion;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		//get default value for count of files

		mVersion = this.findPreference(TPStrings.VERSION_NAME);
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			mVersion.setSummary(pInfo.versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ListPreference encoding = (ListPreference)this.findPreference(TPStrings.ENCODING);
		
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
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		TPApplication.instance.readSettings();

		//setSummaries();
    	
    	if (TPStrings.LANGUAGE.equals(key)){
    		String lang = sharedPreferences.getString(TPStrings.LANGUAGE, TPStrings.EN);
    		setLocale(lang);
    	}
    }	

    
	public void setLocale(String lang) {
		Locale locale2 = new Locale(lang);
		Locale.setDefault(locale2);
		Configuration config2 = new Configuration();
		config2.locale = locale2;

		// updating locale
		getBaseContext().getResources().updateConfiguration(config2, null);

		showPreferences();
	}

	protected void showPreferences(){
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);		
	}
 
}