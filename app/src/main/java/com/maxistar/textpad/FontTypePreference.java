package com.maxistar.textpad;
        //com.maxistar.textpad.

import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

             //FontTypePreference
public class FontTypePreference extends DialogPreference
{
	private int selected;
	
	// This is the constructor called by the inflater
	public FontTypePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// figure out what is currently selected
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
		String font = sharedPref.getString(TPStrings.FONT, TPStrings.MONOSPACE);
		
		if (font.equals(TPStrings.SERIF))
			selected = 1;
		else if (font.equals(TPStrings.SANS_SERIF))
			selected = 2;
		else  
       		selected = 0;	
	}
	
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder){
	    // Data has changed, notify so UI can be refreshed!
		builder.setTitle(R.string.Choose_a_font_type);
		builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				
				Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
				
				if (selected == 0)
					editor.putString(TPStrings.FONT, TPStrings.MONOSPACE);
				else if (selected == 1)
					editor.putString(TPStrings.FONT, TPStrings.SERIF);
				else  
					editor.putString(TPStrings.FONT, TPStrings.SANS_SERIF);
				
				editor.commit();
				
				notifyChanged();
			}
		});
		builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// do nothing on a cancel 
			}
		});
					
		// load the font names
		String[] arrayOfFonts = { TPStrings.MONOSPACE, TPStrings.SERIF, TPStrings.SANS_SERIF };
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
			if (option.equals(TPStrings.SERIF))
				tv.setTypeface(Typeface.SERIF);
			else if (option.equals(TPStrings.SANS_SERIF))
				tv.setTypeface(Typeface.SANS_SERIF);
			else if (option.equals(TPStrings.MONOSPACE))
				tv.setTypeface(Typeface.MONOSPACE);

			// general options
			tv.setTextColor(Color.BLACK);
			tv.setPadding(10, 3, 3, 3);
		
			return v;	
		} // end getView()
				
	} // end class FontTypeArrayAdapter
	
} // end class FontTypePreference