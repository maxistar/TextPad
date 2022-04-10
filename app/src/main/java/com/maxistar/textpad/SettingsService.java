package com.maxistar.textpad;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.preference.PreferenceManager;

import java.util.Locale;

public class SettingsService {
    public static final String SETTING_FONT = "font";
    public static final String SETTING_LAST_FILENAME = "last_filename";
    public static final String SETTING_AUTO_SAVE_CURRENT_FILE = "auto_save_current_file";
    public static final String SETTING_OPEN_LAST_FILE = "open_last_file";
    public static final String SETTING_DELIMITERS = "delimeters";
    public static final String SETTING_FILE_ENCODING = "encoding";
    public static final String SETTING_FONT_SIZE = "fontsize";
    public static final String SETTING_BG_COLOR = "bgcolor";
    public static final String SETTING_FONT_COLOR = "fontcolor";

    public static final String SETTING_SEARCH_SELECTION_COLOR = "search_selection_color";
    public static final String SETTING_TEXT_SELECTION_COLOR = "text_selection_color";

    public static final String SETTING_LANGUAGE = "language";
    public static final String SETTING_LEGASY_FILE_PICKER = "use_legacy_file_picker";
    public static final String SETTING_ALTERNATIVE_FILE_ACCESS = "use_alternative_file_access";

    public static final String SETTING_MEDIUM = "Medium";
    public static final String SETTING_EXTRA_SMALL = "Extra Small";
    public static final String SETTING_SMALL = "Small";
    public static final String SETTING_LARGE = "Large";
    public static final String SETTING_HUGE = "Huge";

    public static final int DEFAULT_BACKGROUND_COLOR = 0xFFCCCCCC;
    public static final int DEFAULT_TEXT_COLOR = 0xFF000000;
    public static final int DEFAULT_SEARCH_SELECTION_COLOR = 0xFFFFFF00;
    public static final int DEFAULT_TEXT_SELECTION_COLOR = 0xFF00FF00;

    private boolean open_last_file = true;
    private boolean legacy_file_picker = false;
    private boolean alternative_file_access = true;

    private String file_encoding = "";
    private String last_filename = "";
    private String delimiters;
    private String font;
    private String font_size;
    private String language;
    private int bgcolor;
    private int fontcolor;
    private int searchSelectionColor;
    private int textSelectionColor;


    private static boolean languageWasChanged = false;


    SettingsService(Context context) {
        loadSettings(context);
    }

    private void loadSettings(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        open_last_file = sharedPref.getBoolean(SETTING_OPEN_LAST_FILE, false);
        legacy_file_picker = sharedPref.getBoolean(SETTING_LEGASY_FILE_PICKER, false);
        alternative_file_access = sharedPref.getBoolean(SETTING_ALTERNATIVE_FILE_ACCESS, true);
        last_filename = sharedPref.getString(SETTING_LAST_FILENAME, TPStrings.EMPTY);
        file_encoding = sharedPref.getString(SETTING_FILE_ENCODING, TPStrings.UTF_8);
        delimiters = sharedPref.getString(SETTING_DELIMITERS, TPStrings.EMPTY);
        font = sharedPref.getString(SETTING_FONT, TPStrings.FONT_SANS_SERIF);
        font_size = sharedPref.getString(SETTING_FONT_SIZE, SETTING_MEDIUM);
        bgcolor = sharedPref.getInt(SETTING_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
        fontcolor = sharedPref.getInt(SETTING_FONT_COLOR, DEFAULT_TEXT_COLOR);
        searchSelectionColor = sharedPref.getInt(SETTING_SEARCH_SELECTION_COLOR, DEFAULT_SEARCH_SELECTION_COLOR);
        textSelectionColor = sharedPref.getInt(SETTING_TEXT_SELECTION_COLOR, DEFAULT_TEXT_SELECTION_COLOR);
        language = sharedPref.getString(SETTING_LANGUAGE, TPStrings.EMPTY);
    }

    public void reloadSettings(Context context) {
        loadSettings(context);
    }

    private void setSettingValue(String name, String value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.apply();
    }

    private void setSettingValue(String name, int value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(name, value);
        editor.apply();
    }

    private void setSettingValue(String name, boolean value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    public boolean isOpenLastFile() {
        return open_last_file;
    }

    public boolean isLegacyFilePicker() {
        return legacy_file_picker;
    }

    public String getFileEncoding() {
        return file_encoding;
    }

    public String getDelimiters() {
        return delimiters;
    }

    public String getFontSize() {
        return font_size;
    }

    public int getBgColor() {
        return bgcolor;
    }

    public int getSearchSelectionColor() {
        return searchSelectionColor;
    }

    public int getTextSelectionColor() {
        return textSelectionColor;
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

    public void setLegacyFilePicker(boolean value) {
        legacy_file_picker = value;
    }

    public void setLegacyFilePicker(boolean value, Context context) {
        this.setSettingValue(SETTING_LEGASY_FILE_PICKER, legacy_file_picker, context);
        legacy_file_picker = value;
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

    public void setTextSelectionColor(int color, Context context) {
        this.setSettingValue(SETTING_TEXT_SELECTION_COLOR, color, context);
        this.textSelectionColor = color;
    }

    public void setSearchSelectionColor(int color, Context context) {
        this.setSettingValue(SETTING_SEARCH_SELECTION_COLOR, color, context);
        this.searchSelectionColor = color;
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

    public boolean isAlternativeFileAccess() {
        return alternative_file_access;
    }
}
