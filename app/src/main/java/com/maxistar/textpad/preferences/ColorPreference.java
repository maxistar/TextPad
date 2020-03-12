package com.maxistar.textpad.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.maxistar.textpad.R;
import com.maxistar.textpad.SettingsService;
import com.maxistar.textpad.TPStrings;

public class ColorPreference extends DialogPreference
{
    protected int color;
    protected String attribute;
    protected String title;

    private SettingsService settingsService;

    // This is the constructor called by the inflater
    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        settingsService = SettingsService.getInstance(context);

        attribute = attrs.getAttributeValue(1);

        // set the layout so we can see the preview color
        setWidgetLayoutResource(R.layout.colorpref);

        if (SettingsService.SETTING_BG_COLOR.equals(attribute)) {
            color = settingsService.getBgColor();
            title = context.getResources().getString(R.string.Choose_a_background_color);
        } else {
            color = settingsService.getFontColor();
            title = context.getResources().getString(R.string.Choose_a_font_color);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        
        // Set our custom views inside the layout
        final View myView = view.findViewById(R.id.currentcolor);
        if (myView != null) {
            myView.setBackgroundColor(color);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder){
        // Data has changed, notify so UI can be refreshed!
        builder.setTitle(title);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // save the color

                if (SettingsService.SETTING_FONT_COLOR.equals(attribute)) {
                    settingsService.setFontColor(color, getContext());
                } else {
                    settingsService.setBgColor(color, getContext());
                }

                notifyChanged();
            }
        });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // set it back to original
                if (SettingsService.SETTING_BG_COLOR.equals(attribute)) {
                    color = settingsService.getBgColor();
                } else {
                    color = settingsService.getFontColor();
                }

            }
        });

        // setup the view
        LayoutInflater factory = LayoutInflater.from(getContext());
        final ViewGroup nullParent = null;
        final View colorView = factory.inflate(R.layout.colorpicker, nullParent);
        final ImageView colormap = colorView.findViewById(R.id.colormap);

        // set the background to the current color
        colorView.setBackgroundColor(color);

        // setup the click listener
        colormap.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                BitmapDrawable bd = (BitmapDrawable) colormap.getDrawable();
                Bitmap bitmap = bd.getBitmap();

                // get the color value.
                // scale the touch location
                int x = (int) ((event.getX() - 15) * bitmap.getWidth() / (colormap.getWidth() - 30));
                int y = (int) ((event.getY() - 15) * bitmap.getHeight() / (colormap.getHeight() - 30));

                if (x >= bitmap.getWidth())
                    x = bitmap.getWidth() - 1;
                if (x < 0)
                    x = 0;

                if (y >= bitmap.getHeight())
                    y = bitmap.getHeight() - 1;
                if (y < 0)
                    y = 0;

                // set the color
                color = bitmap.getPixel(x, y);
                colorView.setBackgroundColor(color);
                v.performClick();
                return true;
            }
        });
        builder.setView(colorView);
    }
}