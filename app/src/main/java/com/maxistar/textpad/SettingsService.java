package com.maxistar.textpad;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;

public class SettingsService {
    public static final String SETTING_FONT = "font";
    public static final String SETTING_LAST_FILENAME = "last_filename";
    public static final String SETTING_AUTO_SAVE_CURRENT_FILE = "auto_save_current_file";
    public static final String SETTING_OPEN_LAST_FILE = "open_last_file";
    public static final String SETTING_DELIMITERS = "delimeters";
    public static final String SETTING_FILE_ENCODING = "encoding";
    public static final String SETTING_AUTOSAVE = "autosave";
    public static final String SETTING_FONT_SIZE = "fontsize";
    public static final String SETTING_BG_COLOR = "bgcolor";
    public static final String SETTING_FONT_COLOR = "fontcolor";
    public static final String SETTING_LANGUAGE = "language";

    public static final String SETTING_MEDIUM = "Medium";
    public static final String SETTING_EXTRA_SMALL = "Extra Small";
    public static final String SETTING_SMALL = "Small";
    public static final String SETTING_LARGE = "Large";
    public static final String SETTING_HUGE = "Huge";




    //private static SettingsService instance;

    //private Context context;

    private boolean open_last_file = true;
    private boolean autosave = true;

    private String file_encoding = "";
    private String last_filename = "";
    private String delimiters;
    private String font;
    private String font_size;
    private String language;
    private int bgcolor;
    private int fontcolor;

    private static boolean languageWasChanged = false;


    private SettingsService(Context context) {
        loadSettings(context);
    }

    private void loadSettings(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        autosave = sharedPref.getBoolean(SETTING_AUTO_SAVE_CURRENT_FILE, false);
        open_last_file = sharedPref.getBoolean(SETTING_OPEN_LAST_FILE, false);
        last_filename = sharedPref.getString(SETTING_LAST_FILENAME, TPStrings.EMPTY);
        file_encoding = sharedPref.getString(SETTING_FILE_ENCODING, TPStrings.UTF_8);
        delimiters = sharedPref.getString(SETTING_DELIMITERS, TPStrings.EMPTY);
        font = sharedPref.getString(SETTING_FONT, TPStrings.FONT_SANS_SERIF);
        font_size = sharedPref.getString(SETTING_FONT_SIZE, SETTING_MEDIUM);
        bgcolor = sharedPref.getInt(SETTING_BG_COLOR, 0xFFCCCCCC);
        fontcolor = sharedPref.getInt(SETTING_FONT_COLOR, 0xFF000000);
        language = sharedPref.getString(SETTING_LANGUAGE, TPStrings.EMPTY);
    }

    public void reloadSettings(Context context) {
        loadSettings(context);
    }

    static public SettingsService getInstance(Context context) {
        return new SettingsService(context);
    }

    private void setSettingValue(String name, String value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.apply();
    }

    private void setSettingValue(String name, boolean value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    private void setSettingValue(String name, int value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(name, value);
        editor.apply();
    }

    //getters

    public boolean isOpenLastFile() {
        return open_last_file;
    };

    public boolean isAutosave() {
        return autosave;
    };

    public String getFileEncoding() {
        return file_encoding;
    }

    public String getDelimiters() {
        return delimiters;
    };

    public String getFontSize() {
        return font_size;
    };

    public int getBgColor() {
        return bgcolor;
    }

    public int getFontColor() {
        return fontcolor;
    }

    public String getLastFilename() {
        return last_filename;
    }

    public String getFont() {
        return font;
    }

    public String getLanguage() {
        return language;
    }

    //setters
    public void setFontSize(String font_size, Context context) {
        this.setSettingValue(SETTING_FONT_SIZE, font_size, context);
        this.font_size = font_size;
    }

    public void setBgColor(int bgcolor, Context context) {
        this.setSettingValue(SETTING_BG_COLOR, bgcolor, context);
        this.bgcolor = bgcolor;
    }

    public void setFontColor(int fontcolor, Context context) {
        this.setSettingValue(SETTING_FONT_COLOR, fontcolor, context);
        this.fontcolor = fontcolor;
    }

    public void setLastFilename(String value, Context context) {
        this.setSettingValue(SETTING_LAST_FILENAME, value, context);
        last_filename = value;
    }

    public void setFont(String value, Context context) {
        this.setSettingValue(SETTING_FONT, value, context);
        font = value;
    }

    static public void setLanguageChangedFlag() {
        languageWasChanged = true;
    }

    static public boolean isLanguageWasChanged() {
        boolean value = languageWasChanged;
        languageWasChanged = false;
        return value;
    }

    public void applyLocale(Context context){
        String lang = getLanguage();
        if ("".equals(lang)) {
            return; //use system default
        }
        Locale locale2 = new Locale(lang);
        Locale.setDefault(locale2);
        Configuration config2 = new Configuration();
        config2.locale = locale2;
        context.getResources().updateConfiguration(config2, null);
    }
}
