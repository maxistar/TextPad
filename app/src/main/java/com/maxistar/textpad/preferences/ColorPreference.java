package com.maxistar.textpad.preferences;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.SettingsService;

import java.util.Locale;

public class ColorPreference extends DialogPreference
{
    protected int color;
    protected String attribute;
    protected String title;

    protected SettingsService settingsService;

    // This is the constructor called by the inflater
    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        settingsService = ServiceLocator.getInstance().getSettingsService(context);
        attribute = attrs.getAttributeValue(1);

        // set the layout so we can see the preview color
        setWidgetLayoutResource(R.layout.colorpref);
        initColor();
    }

    protected void saveColor(int color) {
        // to be rewritten
    }

    protected void initColor() {
    }

    protected int getDefaultColor() {
        return 0;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        
        // Set our custom views inside the layout
        View myView = view.findViewById(R.id.currentcolor);
        if (myView == null) {
            return;
        }
        myView.setBackgroundColor(color);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder){
        // Data has changed, notify so UI can be refreshed!
        builder.setTitle(title);


        // setup the view
        LayoutInflater factory = LayoutInflater.from(getContext());
        final ViewGroup nullParent = null;
        final View colorView = factory.inflate(R.layout.colorpicker, nullParent);
        final ImageView colormap = colorView.findViewById(R.id.colormap);
        final EditText editText = colorView.findViewById(R.id.textColor);
        final CheckBox checkBox = colorView.findViewById(R.id.defaultColorCheckBox);

        checkBox.setChecked(color == getDefaultColor());


        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // save the color
                if (checkBox.isChecked()) {
                    saveColor(getDefaultColor());
                    color = getDefaultColor();
                } else {
                    saveColor(color);
                }
                notifyChanged();
            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // set it back to original
                initColor();
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    colormap.setBackgroundColor(getDefaultColor());
                    editText.setText(colorToText(getDefaultColor()));
                } else {
                    colormap.setBackgroundColor(color);
                }
            }
        });

        // set the background to the current color
        colormap.setBackgroundColor(color);
        editText.setText(colorToText(color));

        // setup the click listener
        colormap.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                checkBox.setChecked(false);
                BitmapDrawable bd = (BitmapDrawable) colormap.getDrawable();
                Bitmap bitmap = bd.getBitmap();

                // get the color value.
                // scale the touch location
                int x = (int) ((event.getX() - 15) * bitmap.getWidth() / (colormap.getWidth() - 30));
                int y = (int) ((event.getY() - 15) * bitmap.getHeight() / (colormap.getHeight() - 30));

                if (x >= bitmap.getWidth()) {
                    x = bitmap.getWidth() - 1;
                }
                if (x < 0) {
                    x = 0;
                }

                if (y >= bitmap.getHeight()) {
                    y = bitmap.getHeight() - 1;
                }
                if (y < 0) {
                    y = 0;
                }

                // set the color
                color = bitmap.getPixel(x, y);
                colormap.setBackgroundColor(color);
                editText.setText(colorToText(color));
                v.performClick();
                return true;
            }
        });
        builder.setView(colorView);
    }

    private String colorToText(int color) {
        String red = Integer.toString(Color.red(color), 16);
        String green = Integer.toString(Color.green(color), 16);
        String blue = Integer.toString(Color.blue(color), 16);
        return String.format(
            Locale.getDefault(),
            "%s%s%s" ,
            red.length() < 2 ? "0" + red : red,
            green.length() < 2 ? "0" + green : green,
            blue.length() < 2 ? "0" + blue : blue
        );
    }
}