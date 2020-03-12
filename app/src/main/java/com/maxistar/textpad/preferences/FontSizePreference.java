package com.maxistar.textpad.preferences;

import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.maxistar.textpad.R;
import com.maxistar.textpad.SettingsService;
import com.maxistar.textpad.TPStrings;

public class FontSizePreference extends DialogPreference
{
    private int selected;

    private SettingsService settingsService;

    // This is the constructor called by the inflater
    public FontSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        settingsService = SettingsService.getInstance(context);
        // figure out the current size.
        String font = settingsService.getFont();

        if (font.equals(SettingsService.SETTING_EXTRA_SMALL))
            selected = 0;
        else if (font.equals(SettingsService.SETTING_SMALL))
            selected = 1;
        else if (font.equals(SettingsService.SETTING_MEDIUM))
            selected = 2;
        else if (font.equals(SettingsService.SETTING_LARGE))
            selected = 3;
        else if (font.equals(SettingsService.SETTING_HUGE))
            selected = 4;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder){
        // Data has changed, notify so UI can be refreshed!
        builder.setTitle(R.string.Choose_a_font_type);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (selected == 0)
                    settingsService.setFontSize(SettingsService.SETTING_EXTRA_SMALL, getContext());
                else if (selected == 1)
                    settingsService.setFontSize(SettingsService.SETTING_SMALL, getContext());
                else if (selected == 2)
                    settingsService.setFontSize(SettingsService.SETTING_MEDIUM, getContext());
                else if (selected == 3)
                    settingsService.setFontSize(SettingsService.SETTING_LARGE, getContext());
                else if (selected == 4)
                    settingsService.setFontSize(SettingsService.SETTING_HUGE, getContext());

                notifyChanged();
            }
        });
        builder.setNegativeButton(R.string.Cancel, null);

        // load the font names and create the adapter
        String[] arrayOfFonts = {
                SettingsService.SETTING_EXTRA_SMALL,
                SettingsService.SETTING_SMALL,
                SettingsService.SETTING_MEDIUM,
                SettingsService.SETTING_LARGE,
                SettingsService.SETTING_HUGE
        };

        List<String> fonts = Arrays.asList(arrayOfFonts);

        FontTypeArrayAdapter adapter = new FontTypeArrayAdapter(getContext(), android.R.layout.simple_list_item_single_choice, fonts);
        builder.setSingleChoiceItems(adapter, selected, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // make sure we know what is selected
                selected = which;
            }
        });
    } // onPrepareDialogBuilder()


    /********************************************************************
     * class FontTypeArrayAdapter
     * 		Array adapter for font type picker */
    public class FontTypeArrayAdapter extends ArrayAdapter<String>
    {
        // just a basic constructor
        public FontTypeArrayAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);

        } // end constructor one

        /****************************************************************
         * getView
         * 		the overroad getView method */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // get the view that would normally be returned
            View v = super.getView(position, convertView, parent);
            final TextView tv = (TextView) v;


            final String option = tv.getText().toString();
            if (option.equals(SettingsService.SETTING_EXTRA_SMALL))
                tv.setTextSize(12.0f);
            else if (option.equals(SettingsService.SETTING_SMALL))
                tv.setTextSize(16.0f);
            else if (option.equals(SettingsService.SETTING_MEDIUM))
                tv.setTextSize(20.0f);
            else if (option.equals(SettingsService.SETTING_LARGE))
                tv.setTextSize(24.0f);
            else if (option.equals(SettingsService.SETTING_HUGE))
                tv.setTextSize(28.0f);

            // general options
            tv.setTextColor(Color.BLACK);
            tv.setPadding(10, 3, 3, 3);

            return v;
        } // end getView()

    } // end class FontTypeArrayAdapter

} // end class ClearListPreference