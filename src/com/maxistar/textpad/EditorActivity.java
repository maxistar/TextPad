package com.maxistar.textpad;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class EditorActivity extends Activity {
	private static final int OPEN_FILE = 1;
	private static final int SAVE_FILE = 2;
	private static final int SETTINGS = 3;
	private static final int NEW_FILE = 4;
	private static final int SAVE_AS = 5;

	private static final int REQUEST_OPEN = 1;
	private static final int REQUEST_SAVE = 2;
	private static final int REQUEST_SETTINGS = 3;

	private static final int DO_NOTHING = 0;
	private static final int DO_OPEN = 1;
	private static final int DO_NEW = 2;

	private EditText mText;
	private TextWatcher watcher;

	private int open_when_saved = DO_NOTHING; // to figure out better way

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mText = (EditText) this.findViewById(R.id.editText1);

		applyPreferences();

		if (savedInstanceState!=null){
        	restoreState(savedInstanceState);
        } 
        else {
        	Intent i = this.getIntent();
			if (TPStrings.ACTION_VIEW.equals(i.getAction())) {
				android.net.Uri u = i.getData();
				openNamedFile(u.getPath());
			} else { // it this is just created
				if (TPApplication.filename.equals(TPStrings.EMPTY)) {
					if (TPApplication.settings.open_last_file) {
						openLastFile();
					}
				}
			}
        }
		

		

		/*
		 * Map<String, Charset> avmap = Charset.availableCharsets(); for(String
		 * name : avmap.keySet()) {
		 * 
		 * Log.w("!","Charset: "+avmap.get(name).displayName()); }
		 */
		watcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (!TPApplication.changed) {
					TPApplication.changed = true;
					updateTitle();
				}
			}
		};
		mText.addTextChangedListener(watcher);
		TPApplication.changed = false;
		updateTitle();
	}

	void restoreState(Bundle state){
        mText.setText(state.getString("text"));
    }
    
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("state", mText.getText().toString());
    }

	protected void onStop() {
		mText.removeTextChangedListener(watcher); // to prevent text
													// modification once rotated
		super.onStart();
	}

	void openLastFile() {
		if (!TPApplication.settings.last_filename.equals(TPStrings.EMPTY)) {
			Log.w("!", TPApplication.settings.last_filename);
			this.openNamedFile(TPApplication.settings.last_filename);
		}
	}

	void updateTitle() {
		String title;
		if (TPApplication.filename.equals(TPStrings.EMPTY)) {
			title = TPStrings.NEW_FILE_TXT;
		} else {
			title = TPApplication.filename;
		}
		if (TPApplication.changed) {
			title = title + "*";
		}
		this.setTitle(title);
	}

	void applyPreferences() {
		TPApplication.instance.updateSettings();
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		/********************************
		 * font face
		 */
		String font = sharedPref.getString("font", "Monospace");

		if (font.equals("Serif"))
			mText.setTypeface(Typeface.SERIF);
		else if (font.equals("Sans Serif"))
			mText.setTypeface(Typeface.SANS_SERIF);
		else
			mText.setTypeface(Typeface.MONOSPACE);

		/********************************
		 * font size
		 */
		String fontsize = sharedPref.getString("fontsize", "Medium");

		if (fontsize.equals("Extra Small"))
			mText.setTextSize(12.0f);
		else if (fontsize.equals("Small"))
			mText.setTextSize(16.0f);
		else if (fontsize.equals("Medium"))
			mText.setTextSize(20.0f);
		else if (fontsize.equals("Large"))
			mText.setTextSize(24.0f);
		else if (fontsize.equals("Huge"))
			mText.setTextSize(28.0f);
		else
			mText.setTextSize(20.0f);

		/********************************
		 * Colors
		 */
		int bgcolor = sharedPref.getInt("bgcolor", 0xFFCCCCCC);
		mText.setBackgroundColor(bgcolor);

		int fontcolor = sharedPref.getInt("fontcolor", 0xFF000000);
		mText.setTextColor(fontcolor);

		// title.setTextColor(bgcolor);
		// title.setBackgroundColor(fontcolor);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, NEW_FILE, 0, R.string.New).setIcon(R.drawable.documentnew);

		menu.add(0, OPEN_FILE, 0, R.string.Open).setIcon(
				R.drawable.documentopen);

		menu.add(0, SAVE_FILE, 0, R.string.Save).setIcon(
				R.drawable.documentsave);

		menu.add(0, SAVE_AS, 0, R.string.Save_As).setIcon(
				R.drawable.documentsave_as);

		menu.add(0, SETTINGS, 0, R.string.Settings)
				.setIcon(R.drawable.settings);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case OPEN_FILE:
			openFile();
			return true;
		case NEW_FILE:
			newFile();
			return true;
		case SAVE_FILE:
			saveFile();
			return true;
		case SAVE_AS:
			saveAs();
			return true;
		case SETTINGS:
			Intent intent = new Intent(this.getBaseContext(),
					SettingsActivity.class);
			this.startActivityForResult(intent, REQUEST_SETTINGS);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void newFile() {
		// this.showToast(Environment.getExternalStorageDirectory().getName());
		if (TPApplication.changed) {
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.File_not_saved)
					.setMessage(R.string.Save_current_file)
					.setPositiveButton(R.string.Yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// Stop the activity
									open_when_saved = DO_NEW;
									EditorActivity.this.saveFile();
								}

							})
					.setNegativeButton(R.string.No,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									clearFile();
								}
							}).show();
		} else {
			clearFile();
		}
	}

	protected void clearFile() {
		this.mText.setText(TPStrings.EMPTY);
		TPApplication.filename = TPStrings.EMPTY;
		TPApplication.changed = false;
		this.updateTitle();
	}

	protected void saveAs() {
		Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
		this.startActivityForResult(intent, REQUEST_SAVE);
	}

	protected void openFile() {
		// this.showToast(Environment.getExternalStorageDirectory().getName());
		if (TPApplication.changed) {
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.File_not_saved)
					.setMessage(R.string.Save_current_file)
					.setPositiveButton(R.string.Yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// Stop the activity
									open_when_saved = DO_OPEN;
									EditorActivity.this.saveFile();
								}

							})
					.setNegativeButton(R.string.No,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									openNewFile();
								}
							}).show();
		} else {
			openNewFile();
		}
	}

	protected void openNewFile() {
		Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
		// intent.putExtra(TPStrings.START_PATH,
		// TPApplication.settings.start_path);
		intent.putExtra(TPStrings.SELECTION_MODE, SelectionMode.MODE_OPEN);
		this.startActivityForResult(intent, REQUEST_OPEN);
	}

	protected void saveFile() {
		if (TPApplication.filename.equals(TPStrings.EMPTY)) {
			Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
			// intent.putExtra(TPStrings.START_PATH,
			// TPApplication.settings.start_path);
			this.startActivityForResult(intent, REQUEST_SAVE);
		} else {
			saveNamedFile();
		}
	}

	protected void saveNamedFile() {
		// String string = this.mText.toString();
		try {
			File f = new File(TPApplication.filename);

			if (!f.exists()) {
				f.createNewFile();
			}

			FileOutputStream fos = new FileOutputStream(f);
			String s = this.mText.getText().toString();
			fos.write(s.getBytes(TPApplication.settings.file_encoding));
			fos.close();
			showToast(l(R.string.File_Written));
			TPApplication.changed = false;
			updateTitle();

			if (open_when_saved == DO_OPEN) { // because of multithread nature
												// figure out better way to do
												// it
				open_when_saved = DO_NOTHING;
				openNewFile();
			}
			if (open_when_saved == DO_NEW) { // because of multithread nature
												// figure out better way to do
												// it
				open_when_saved = DO_NOTHING;
				clearFile();
			}
		} catch (FileNotFoundException e) {
			this.showToast(l(R.string.File_not_found));
		} catch (IOException e) {
			this.showToast(l(R.string.Can_not_write_file));
		}
	}

	protected void openNamedFile(String filename) {
		try {
			File f = new File(filename);
			FileInputStream fis = new FileInputStream(f);

			long size = f.length();
			DataInputStream dis = new DataInputStream(fis);
			byte[] b = new byte[(int) size];
			int length = dis.read(b, 0, (int) size);

			dis.close();
			fis.close();

			String ttt = new String(b, 0, length,
					TPApplication.settings.file_encoding);

			this.mText.setText(ttt);
			showToast(l(R.string.File_opened_) + TPApplication.filename);
			TPApplication.changed = false;
			TPApplication.filename = filename;
			if (!TPApplication.settings.last_filename.equals(filename)) {
				TPApplication.instance.saveLastFilename(filename);
			}
			updateTitle();
		} catch (FileNotFoundException e) {
			this.showToast(l(R.string.File_not_found));
		} catch (IOException e) {
			this.showToast(l(R.string.Can_not_read_file));
		}
		// fis.re
	}

	/*
	 * public void onDestroy(){ super.onDestroy(); Log.w("!","stopped"); }
	 */

	public synchronized void onActivityResult(final int requestCode,
			int resultCode, final Intent data) {

		if (requestCode == REQUEST_SAVE) {
			if (resultCode == Activity.RESULT_OK) {
				TPApplication.filename = data
						.getStringExtra(TPStrings.RESULT_PATH);
				this.saveNamedFile();
			} else if (resultCode == Activity.RESULT_CANCELED) {
				showToast(l(R.string.Operation_Canceled));
			}
		} else if (requestCode == REQUEST_OPEN) {
			if (resultCode == Activity.RESULT_OK) {
				this.openNamedFile(data.getStringExtra(TPStrings.RESULT_PATH));
			} else if (resultCode == Activity.RESULT_CANCELED) {
				showToast(l(R.string.Operation_Canceled));
			}
		} else if (requestCode == REQUEST_SETTINGS) {
			applyPreferences();
		}
	}

	protected void showToast(String toast_str) {
		Context context = getApplicationContext();
		CharSequence text = toast_str;
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	String l(int id) {
		return getBaseContext().getResources().getString(id);
	}
}