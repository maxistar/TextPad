package com.maxistar.textpad;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
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
		//setContentView(R.layout.settings);
		//this.setP
		addPreferencesFromResource(R.xml.preferences);
		
		//get default value for count of files
		//mCountOfFilesToRemember = this.findPreference("count_of_files_to_remember");

		mVersion = this.findPreference("version_name");
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			mVersion.setSummary(pInfo.versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        // Setup the initial values        
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        //mCountOfFilesToRemember.setSummary("Current value is " + sharedPreferences.getString("count_of_files_to_remember", "")); 

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

        //if (key.equals("count_of_files_to_remember")) {
        //	mCountOfFilesToRemember.setSummary("Current value is " + sharedPreferences.getString(key, "")); 
        //}
    }	

 
}