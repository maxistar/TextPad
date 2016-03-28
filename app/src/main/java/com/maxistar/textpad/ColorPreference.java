package com.maxistar.textpad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class ColorPreference extends DialogPreference
{
	protected int color;
	protected int defcolor;
	protected String attribute;
	protected String title;
	
	// This is the constructor called by the inflater
	public ColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		attribute = attrs.getAttributeValue(1);
		defcolor = attrs.getAttributeIntValue(TPStrings.ANDROID_NS, TPStrings.DEFAULT_VALUE, 0xFFCCCCCC);
		title = attrs.getAttributeValue(TPStrings.ANDROID_NS, TPStrings.DIALOG_TITLE);
		if (TPStrings.EMPTY.equals(title)){
			title = context.getResources().getString(R.string.Choose_a_color);
		}
		
		// set the layout so we can see the preview color
		setWidgetLayoutResource(R.layout.colorpref);

		// figure out what the current color is
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
		
		color = sharedPref.getInt(attribute, defcolor);
	}

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        
        // Set our custom views inside the layout
        final View myView = (View) view.findViewById(R.id.currentcolor);
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
				Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();						
				editor.putInt(attribute, color);
				editor.commit();
				
				notifyChanged();
			}
		});
		builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// set it back to original
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
				color = sharedPref.getInt(attribute, defcolor);
			}
		});
		
		// setup the view
		LayoutInflater factory = LayoutInflater.from(getContext());
		final View colorView = factory.inflate(R.layout.colorpicker, null);
		final ImageView colormap = (ImageView) colorView.findViewById(R.id.colormap);

		// set the background to the current color
		colorView.setBackgroundColor(color);
		
		// setup the click listener
		colormap.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				BitmapDrawable bd = (BitmapDrawable) colormap.getDrawable();
				Bitmap bitmap = bd.getBitmap();

				// get the color value. 
				// scale the touch location
				int x = (int) ((event.getX()-15) * bitmap.getWidth() / (colormap.getWidth()-30));
				int y = (int) ((event.getY()-15) * bitmap.getHeight() / (colormap.getHeight()-30));

				if (x >= bitmap.getWidth())
					x = (int) bitmap.getWidth() - 1;
				if (x < 0)
					x = 0;

				if (y >= bitmap.getHeight())
					y = (int) bitmap.getHeight() - 1;
                if (y < 0)
                	y = 0;
				
                // set the color
				color = bitmap.getPixel(x, y);
				colorView.setBackgroundColor(color);
				
				return true;
			}
		});
		builder.setView(colorView);
    }
}