package com.maxistar.textpad;

import java.util.Locale;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

public class TPApplication extends Application {
	static TPApplication instance;
	
	//settings properties
	static Settings settings;
	
	
    @Override
    public void onCreate() {
		super.onCreate();
    	TPApplication.instance = this;
        settings = new Settings();
        readSettings(); 
        readLocale();
    }
    
    public void readLocale(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		Configuration config = this.getApplicationContext().getResources().getConfiguration();
		Locale locale;

		String lang = settings.getString(TPStrings.LANGUAGE, TPStrings.EMPTY);
		if (!TPStrings.EMPTY.equals(lang)
				&& !config.locale.getLanguage().equals(lang)) {
			locale = new Locale(lang);
			Locale.setDefault(locale);
			config.locale = locale;
			Context c = this.getBaseContext();
			c.getResources().updateConfiguration(config, null);
		}    	
    }
    
    
    public void readSettings(){
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        settings.autosave = sharedPref.getBoolean(TPStrings.AUTO_SAVE_CURRENT_FILE, false);
        settings.open_last_file = sharedPref.getBoolean(TPStrings.OPEN_LAST_FILE, false);
        settings.last_filename = sharedPref.getString(TPStrings.LAST_FILENAME, TPStrings.EMPTY);
        settings.file_encoding = sharedPref.getString(TPStrings.ENCODING, TPStrings.UTF_8);
        settings.delimiters = sharedPref.getString(TPStrings.DELIMITERS, TPStrings.DEFAULT);
        
        
        
    }
    
    void setSettingValue(String name, String value) {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(name, value);
		editor.commit();
	}
    
    public void saveLastFilename(String value){
    	this.setSettingValue(TPStrings.LAST_FILENAME, value);
    	TPApplication.settings.last_filename = value;
    }
    
    public static TPApplication getApplication() {
    	return TPApplication.instance;
	}
    

    String l(int id){
		return getBaseContext().getResources().getString(id);
	}    
    
}