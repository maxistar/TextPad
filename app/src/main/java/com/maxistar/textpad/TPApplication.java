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
	Settings settings;
	
	
    public TPApplication(Context context) {
		settings = new Settings();
        readSettings(context);
        readLocale(context);
    }

    static TPApplication getInstance(Context context) {
    	if (instance == null) {
    		instance = new TPApplication(context);
		}
		return instance;
	}
    
    public void readLocale(Context context){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		Configuration config = context.getResources().getConfiguration();
		Locale locale;

		String lang = settings.getString(TPStrings.LANGUAGE, TPStrings.EMPTY);
		//if (
		//		!TPStrings.EMPTY.equals(lang) &&
		//		!config.locale.getLanguage().equals(lang)
		//) {
		Locale locale2 = new Locale(lang);
		Locale.setDefault(locale2);
		Configuration config2 = new Configuration();
		config2.locale = locale2;
		Log.w("LOCALE","New locale " + lang);

			//locale = new Locale(lang);
			//Locale.setDefault(locale);
			//config.locale = locale;
			context.getResources().updateConfiguration(config2, null);
		//}
    }
    
    
    public void readSettings(Context context){
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        settings.autosave = sharedPref.getBoolean(TPStrings.AUTO_SAVE_CURRENT_FILE, false);
        settings.open_last_file = sharedPref.getBoolean(TPStrings.OPEN_LAST_FILE, false);
        settings.last_filename = sharedPref.getString(TPStrings.LAST_FILENAME, TPStrings.EMPTY);
        settings.file_encoding = sharedPref.getString(TPStrings.ENCODING, TPStrings.UTF_8);
        settings.delimiters = sharedPref.getString(TPStrings.DELIMITERS, TPStrings.DEFAULT);
    }
    
    void setSettingValue(Context context, String name, String value) {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(name, value);
		editor.apply();
	}
    
    public void saveLastFilename(Context context, String value){
    	this.setSettingValue(context, TPStrings.LAST_FILENAME, value);
    	settings.last_filename = value;
    }
    
    public static TPApplication getApplication() {
    	return TPApplication.instance;
	}
    

    String l(Context context, int id) {
    	return context.getResources().getString(id);
	}    
    
}