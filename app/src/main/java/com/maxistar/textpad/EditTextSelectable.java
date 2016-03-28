package com.maxistar.textpad;

/**
 * very simple selectable interface
 * 
 * thanks: http://stackoverflow.com/questions/5962366/android-edittext-listener-for-cursor-position-change
 * 
 * 
 * 
 */
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

public class EditTextSelectable extends EditText {

	public interface OnSelectionChangedListener {
		public void onSelectionChanged(int selStart, int selEnd);
	}

	private List<OnSelectionChangedListener> listeners = null;

	public EditTextSelectable(Context context) {
		super(context);
	}

	public EditTextSelectable(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditTextSelectable(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
	}

	public void addOnSelectionChangedListener(OnSelectionChangedListener o) {
		if (listeners==null) listeners = new ArrayList<OnSelectionChangedListener>();
		listeners.add(o);
	}

	protected void onSelectionChanged(int selStart, int selEnd) {
		if (listeners==null) return;

		for (OnSelectionChangedListener l : listeners)
			l.onSelectionChanged(selStart, selEnd);

	}
}