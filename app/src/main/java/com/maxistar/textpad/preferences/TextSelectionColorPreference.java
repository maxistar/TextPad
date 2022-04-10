package com.maxistar.textpad.preferences;

import android.content.Context;
import android.util.AttributeSet;

import com.maxistar.textpad.R;
import com.maxistar.textpad.SettingsService;

public class TextSelectionColorPreference extends ColorPreference {
    public TextSelectionColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        title = context.getResources().getString(R.string.Choose_a_background_color);
    }

    protected void saveColor(int color) {
        settingsService.setTextSelectionColor(color, getContext());
    }

    protected void initColor() {
        color = settingsService.getTextSelectionColor();
    }

    protected int getDefaultColor() {
        return SettingsService.DEFAULT_TEXT_SELECTION_COLOR;
    }
}
