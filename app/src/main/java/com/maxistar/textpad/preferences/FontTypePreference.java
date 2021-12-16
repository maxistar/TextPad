package com.maxistar.textpad.preferences;

import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.SettingsService;
import com.maxistar.textpad.TPStrings;

import androidx.annotation.NonNull;

public class FontTypePreference extends DialogPreference
{
    private int selected;

    SettingsService settingsService;

    // This is the constructor called by the inflater
    public FontTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        settingsService = ServiceLocator.getInstance().getSettingsService(context);

        String font = settingsService.getFont();

        if (font.equals(TPStrings.FONT_SERIF))
            selected = 1;
        else if (font.equals(TPStrings.FONT_SANS_SERIF))
            selected = 2;
        else
            selected = 0;
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder){
        // Data has changed, notify so UI can be refreshed!
        builder.setTitle(R.string.Choose_a_font_type);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (selected == 0)
                    settingsService.setFont(TPStrings.FONT_MONOSPACE, getContext());
                else if (selected == 1)
                    settingsService.setFont(TPStrings.FONT_SERIF, getContext());
                else
                    settingsService.setFont(TPStrings.FONT_SANS_SERIF, getContext());


                notifyChanged();
            }
        });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // do nothing on a cancel
            }
        });

        // load the font names
        String[] arrayOfFonts = { TPStrings.FONT_MONOSPACE, TPStrings.FONT_SERIF, TPStrings.FONT_SANS_SERIF};
        List<String> fonts = Arrays.asList(arrayOfFonts);

        FontTypeArrayAdapter adapter = new FontTypeArrayAdapter(getContext(), android.R.layout.simple_list_item_single_choice, fonts);
        builder.setSingleChoiceItems(adapter, selected, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // make sure we know what is selected
                selected = which;
            }
        });
    }

    /**
     * class FontTypeArrayAdapter
     * Array adapter for font type picker
     */
    public static class FontTypeArrayAdapter extends ArrayAdapter<String>
    {
        // just a basic constructor
        public FontTypeArrayAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);

        }

        /**
         * getView
         * the overroad getView method
         */
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent)
        {
            // get the view that would normally be returned
            View v = super.getView(position, convertView, parent);
            final TextView tv = (TextView) v;

            final String option = tv.getText().toString();
            switch (option) {
                case TPStrings.FONT_SERIF:
                    tv.setTypeface(Typeface.SERIF);
                    break;
                case TPStrings.FONT_SANS_SERIF:
                    tv.setTypeface(Typeface.SANS_SERIF);
                    break;
                case TPStrings.FONT_MONOSPACE:
                    tv.setTypeface(Typeface.MONOSPACE);
                    break;
            }

            // general options
            tv.setTextColor(Color.BLACK);
            tv.setPadding(10, 3, 3, 3);

            return v;
        }
    }
}