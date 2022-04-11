package com.maxistar.textpad.preferences;

import android.content.Context;
import android.util.AttributeSet;

import com.maxistar.textpad.R;
import com.maxistar.textpad.SettingsService;

public class TextColorPreference extends ColorPreference{
    public TextColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        title = context.getResources().getString(R.string.Choose_a_font_color);
    }

    protected void saveColor(int color) {
        settingsService.setFontColor(color, getContext());
    }

    protected void initColor() {
        color = settingsService.getFontColor();
    }

    protected int getDefaultColor() {
        return SettingsService.DEFAULT_TEXT_COLOR;
    }
}
