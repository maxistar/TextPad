package com.maxistar.textpad;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TPApplication extends Application {
	static TPApplication instance;
	
	//settings properties
	static Settings settings;
	
	
    @Override
    public void onCreate() {
    	TPApplication.instance = this;
        settings = new Settings();
        updateSettings(); 
    }
    
    
    
    public void updateSettings(){
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        settings.autosave = sharedPref.getBoolean("auto_save_current_file", false);
        settings.open_last_file = sharedPref.getBoolean("open_last_file", false);
        settings.last_filename = sharedPref.getString("last_filename", "");
    }
    
    void setSettingValue(String name, String value) {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(name, value);
		editor.commit();
	}
    
    public void saveLastFilename(String value){
    	this.setSettingValue("last_filename", value);
    	TPApplication.settings.last_filename = value;
    }
    
    
    

    String l(int id){
		return getBaseContext().getResources().getString(id);
	}    
    
}