package com.maxistar.textpad;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

public class TPApplication {
	private static TPApplication instance;
	
	//settings properties
	SettingsService settingsService;
	
    public TPApplication(Context context, SettingsService settingsService) {
		this.settingsService = settingsService;

		//settings = new Settings();
        //readSettings(context);
        readLocale(context);
    }

    static TPApplication getInstance(Context context) {
    	if (instance == null) {
    		instance = new TPApplication(context, SettingsService.getInstance(context));
		}
		return instance;
	}
    
    public void readLocale(Context context){

		String lang = settingsService.getLanguage();

		Locale locale2 = new Locale(lang);
		Locale.setDefault(locale2);
		Configuration config2 = new Configuration();
		config2.locale = locale2;

		context.getResources().updateConfiguration(config2, null);
    }
}