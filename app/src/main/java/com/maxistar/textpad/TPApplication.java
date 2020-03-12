package com.maxistar.textpad;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

public class TPApplication {
	private static TPApplication instance;
	
    public TPApplication() {
    }

    static TPApplication getInstance() {
    	if (instance == null) {
    		instance = new TPApplication();
		}
		return instance;
	}
}