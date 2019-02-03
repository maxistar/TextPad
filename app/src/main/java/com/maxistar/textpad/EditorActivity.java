package com.maxistar.textpad;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.maxistar.textpad.EditTextSelectable.OnSelectionChangedListener;

public class EditorActivity extends Activity {
	private static final int REQUEST_OPEN = 1;
	private static final int REQUEST_SAVE = 2;
	private static final int REQUEST_SETTINGS = 3;

	private static final int DO_NOTHING = 0;
	private static final int DO_OPEN = 1;
	private static final int DO_NEW = 2;

	private EditTextSelectable mText;
	private TextWatcher watcher;
	String filename = TPStrings.EMPTY;
	boolean changed = false;
	boolean exitDialogShown = false;
	
	private int open_when_saved = DO_NOTHING; // to figure out better way

	Handler handler = new Handler();
	
	static int selectionStart = 0;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mText = (EditTextSelectable) this.findViewById(R.id.editText1);

        verifyPermissions(this);

		applyPreferences();

		if (savedInstanceState!=null) {
        	restoreState(savedInstanceState);
        } else {
        	Intent i = this.getIntent();
			if (TPStrings.ACTION_VIEW.equals(i.getAction())) {
				android.net.Uri u = i.getData();
				openNamedFile(u.getPath());
				
			} else { // it this is just created
				if (this.filename.equals(TPStrings.EMPTY)) {
					if (TPApplication.settings.open_last_file) {
						openLastFile();
					}
				}
			}
        }
		
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
				if (!changed) {
					changed = true;
					updateTitle();
				}
			}
		};
		handler.postDelayed(new Runnable(){
			@Override
			public void run() {
				mText.addTextChangedListener(watcher);
				
				mText.addOnSelectionChangedListener(new OnSelectionChangedListener(){

					@Override
					public void onSelectionChanged(int selStart, int selEnd) {
						// TODO Auto-generated method stub
						selectionStart = mText.getSelectionStart();
					}
					
				});
				
			}
		}, 1000);

		updateTitle();
		mText.requestFocus();
		
		TPApplication.instance.readLocale(); //additionally check locale
	}

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyPermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    1
            );
        }
    }


    protected void onResume(){
		super.onResume();
		String t = mText.getText().toString().toLowerCase(Locale.getDefault());
		if (selectionStart<t.length()) {
			mText.setSelection(selectionStart,selectionStart);			
		}
	}

	protected void onPause(){
		super.onPause();
	}

	
	void restoreState(Bundle state){
		mText.setText(state.getString(TPStrings.TEXT));
        filename = state.getString(TPStrings.FILENAME);
        changed = state.getBoolean(TPStrings.CHANGED);
    }
    
	
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TPStrings.TEXT, mText.getText().toString());
        outState.putString(TPStrings.FILENAME,filename);
        outState.putBoolean(TPStrings.CHANGED, changed);
        
    }

	protected void onStop() {
		mText.removeTextChangedListener(watcher); 	// to prevent text
													// modification once rotated
		super.onStop();
	}
	
	public void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		// search action
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String query = intent.getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, TPStrings.AUTHORITY, SearchSuggestions.MODE);
			suggestions.saveRecentQuery(query, null);
			
			handler.postDelayed(new Runnable(){
				@Override
				public void run() {
					doSearch(query);
				}
			}, 1000);
		}
	}
	
	@Override
	public void onBackPressed() {       
	    if (this.changed && !exitDialogShown) {          
	        new AlertDialog.Builder(this)
	            .setTitle(R.string.You_have_made_some_changes)
	            .setMessage(R.string.Are_you_sure_to_quit)
	            .setNegativeButton(R.string.Yes, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface arg0, int arg1) {
	                    EditorActivity.super.onBackPressed();
	                    exitDialogShown = false;
	                }
	            })            
	            .setPositiveButton(R.string.No, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface arg0, int arg1) {                    
	                    //do nothing
	                	exitDialogShown = false;
	                }
	            })
	            .setOnCancelListener(new DialogInterface.OnCancelListener(){
					@Override
					public void onCancel(DialogInterface arg0) {
						// TODO Auto-generated method stub
						EditorActivity.super.onBackPressed();
					}
	            })
	            .create().show();
	        exitDialogShown = true;
	    }
	    else {
	        super.onBackPressed();
	    }
	}

	void doSearch(String query){
			String t = mText.getText().toString().toLowerCase(Locale.getDefault());
			
			if (selectionStart >= t.length()) {
				selectionStart = -1;
			}

		    int start;			
			start = t.indexOf(query.toLowerCase(Locale.getDefault()), selectionStart+1);
			if (start == -1) {	// loop search
				start = t.indexOf(query.toLowerCase(Locale.getDefault()), 0);
			}			
				
			if (start != -1) {
				selectionStart = start;
				mText.setSelection(start, start + query.length());
			} else {
				selectionStart = 0;
				Toast.makeText(this, formatString(R.string.s_not_found, query), Toast.LENGTH_SHORT).show();
			}
	}
	
	String formatString(int stringId, String parameter){
		return this.getResources().getString(stringId, parameter);
	}
	
	void openLastFile() {
		if (!TPApplication.settings.last_filename.equals(TPStrings.EMPTY)) {
			showToast(formatString(R.string.opened_last_edited_file,TPApplication.settings.last_filename));
			this.openNamedFile(TPApplication.settings.last_filename);
		}
	}

	void updateTitle() {
		String title;
		if (filename.equals(TPStrings.EMPTY)) {
			title = TPStrings.NEW_FILE_TXT;
		} else {
			title = filename;
		}
		if (changed) {
			title = title + TPStrings.STAR;
		}
		this.setTitle(title);
	}

	void applyPreferences() {
		mText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE |
						   InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
						   InputType.TYPE_TEXT_VARIATION_NORMAL |
						   InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD |
						   InputType.TYPE_CLASS_TEXT);

		TPApplication.instance.readSettings();
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		/********************************
		 * font face
		 */
		String font = sharedPref.getString(TPStrings.FONT, TPStrings.MONOSPACE);

		if (font.equals(TPStrings.SERIF))
			mText.setTypeface(Typeface.SERIF);
		else if (font.equals(TPStrings.SANS_SERIF))
			mText.setTypeface(Typeface.SANS_SERIF);
		else
			mText.setTypeface(Typeface.MONOSPACE);

		/********************************
		 * font size
		 */
		String fontsize = sharedPref.getString(TPStrings.FONTSIZE, TPStrings.MEDIUM);

		if (fontsize.equals(TPStrings.EXTRA_SMALL))
			mText.setTextSize(12.0f);
		else if (fontsize.equals(TPStrings.SMALL))
			mText.setTextSize(16.0f);
		else if (fontsize.equals(TPStrings.MEDIUM))
			mText.setTextSize(20.0f);
		else if (fontsize.equals(TPStrings.LARGE))
			mText.setTextSize(24.0f);
		else if (fontsize.equals(TPStrings.HUGE))
			mText.setTextSize(28.0f);
		else
			mText.setTextSize(20.0f);

		/********************************
		 * Colors
		 */
		int bgcolor = sharedPref.getInt(TPStrings.BGCOLOR, 0xFFCCCCCC);
		mText.setBackgroundColor(bgcolor);

		int fontcolor = sharedPref.getInt(TPStrings.FONTCOLOR, 0xFF000000);
		mText.setTextColor(fontcolor);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_document_open:
			openFile();
			return true;
		case R.id.menu_document_new:
			newFile();
			return true;
		case R.id.menu_document_save:
			saveFile();
			return true;
		case R.id.menu_document_save_as:
			saveAs();
			return true;
		case R.id.menu_document_search: // Trigger search
			this.onSearchRequested();
			break;
		case R.id.menu_document_settings:
			Intent intent = new Intent(this.getBaseContext(),
					SettingsActivity.class);
			this.startActivityForResult(intent, REQUEST_SETTINGS);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void newFile() {
		if (changed) {
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
		filename = TPStrings.EMPTY;
		changed = false;
		this.updateTitle();
	}

	protected void saveAs() {
		Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
		this.startActivityForResult(intent, REQUEST_SAVE);
	}

	protected void openFile() {
		if (changed) {
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
		intent.putExtra(TPStrings.SELECTION_MODE, SelectionMode.MODE_OPEN);
		this.startActivityForResult(intent, REQUEST_OPEN);
	}

	protected void saveFile() {
		if (filename.equals(TPStrings.EMPTY)) {
			Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
			this.startActivityForResult(intent, REQUEST_SAVE);
		} else {
		    saveNamedFile();
		}
	}

	protected void saveFileWithConfirmation() {
        if (this.fileAlreadyExists()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.File_already_exists)
                    .setMessage(R.string.Existing_file_will_be_overwritten)
                    .setPositiveButton(R.string.Yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    // Stop the activity
                                    open_when_saved = DO_OPEN;
                                    EditorActivity.this.saveFile();
                                }

                            }).setNegativeButton(R.string.No,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            //do nothing!!
                        }
                    }).show();
        } else {
            saveNamedFile();
        }
    }

	protected boolean fileAlreadyExists() {
        File f = new File(filename);
        if (f.exists()) {
            return true;
        }
        return false;
    }

	protected void saveNamedFile() {
		try {
			File f = new File(filename);
			if (!f.exists()) {
				f.createNewFile();
			}

			FileOutputStream fos = new FileOutputStream(f);
			String s = this.mText.getText().toString();
			
			s = applyEndings(s);
			
			fos.write(s.getBytes(TPApplication.settings.file_encoding));
			fos.close();
			showToast(l(R.string.File_Written));
			changed = false;
			updateTitle();

			if (open_when_saved == DO_OPEN) {   // because of multithread nature
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

			ttt = toUnixEndings(ttt);
			
			this.mText.setText(ttt);
			showToast(l(R.string.File_opened_) + filename);
			changed = false;
			this.filename = filename;
			if (!TPApplication.settings.last_filename.equals(filename)) {
				TPApplication.instance.saveLastFilename(filename);
			}
			selectionStart = 0;
			updateTitle();
		} catch (FileNotFoundException e) {
			this.showToast(l(R.string.File_not_found));
		} catch (IOException e) {
			this.showToast(l(R.string.Can_not_read_file));
		}
	}
	
	/**
	 * @param value
	 * @return
	 */
	String applyEndings(String value){
		String to = TPApplication.settings.delimiters;
		if (TPStrings.DEFAULT.equals(to)) return value; //this way we spare memory but will be unable to fix delimiters

		if (TPStrings.WINDOWS.equals(to)){
			value = value.replace(TPStrings.RN, TPStrings.N);
			value = value.replace(TPStrings.R, TPStrings.N);
			value = value.replace(TPStrings.N, TPStrings.RN); //simply replace unix endings to win endings			
		}
		else if (TPStrings.UNIX.equals(to)){ //just in case it was previously read as other encoding
			value = value.replace(TPStrings.RN, TPStrings.N);
			value = value.replace(TPStrings.R, TPStrings.N);			
		}
		else if (TPStrings.MACOS.equals(to)){
			value = value.replace(TPStrings.RN, TPStrings.N);
			value = value.replace(TPStrings.R, TPStrings.N);
			value = value.replace(TPStrings.N, TPStrings.R); //simply replace unix endings to mac endings
		}
		return value;
	}
	
	/**
	 * @param value
	 *
	 * @return
	 */
	String toUnixEndings(String value){
		String from = TPApplication.settings.delimiters;
		if (TPStrings.DEFAULT.equals(from)) return value; //this way we spare memory but will be unable to fix delimiters
		
		//we should anyway fix any line delimenters
		//replace \r\n first, then \r into \n this way we will get pure unix ending used in android
		value = value.replace(TPStrings.RN, TPStrings.N);
		value = value.replace(TPStrings.R, TPStrings.N);
		
		return value;
	}

	/**
	 * 
	 */
	public synchronized void onActivityResult(final int requestCode,
			int resultCode, final Intent data) {

		if (requestCode == REQUEST_SAVE) {
			if (resultCode == Activity.RESULT_OK) {
				filename = data
						.getStringExtra(TPStrings.RESULT_PATH);
				this.saveFileWithConfirmation();
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
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, toast_str, duration);
		toast.show();
	}

	String l(int id) {
		return getBaseContext().getResources().getString(id);
	}
}