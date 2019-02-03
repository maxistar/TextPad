package com.maxistar.textpad;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class FileDialog extends ListActivity {

	/**
	 *
	 */
	private List<String> path = null;

	/**
	 * Text view with the name
	 */
	private TextView myPath;

	/**
	 * Edit text with the file name
	 */
	private EditText mFileName;

	/**
	 *
	 */
	private ArrayList<HashMap<String, Object>> mList;

	/**
	 *
	 */
	private String parentPath;

	/**
	 *
	 */
	private String currentPath;

	/**
	 *
	 */
	private String rootPath;



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED, getIntent());

		setContentView(R.layout.file_dialog_main);
		myPath = (TextView) findViewById(R.id.path);
		mFileName = (EditText) findViewById(R.id.fdEditTextFile);

		/*
		 * final Button newButton = (Button) findViewById(R.id.fdButtonNew);
		 * newButton.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { setCreateVisible(v);
		 * 
		 * mFileName.setText(TPStrings.EMPTY); mFileName.requestFocus(); } });
		 */
		mFileName.setText(TPStrings.NEW_FILE_TXT);
		int selectionMode = getIntent().getIntExtra(TPStrings.SELECTION_MODE,
				SelectionMode.MODE_CREATE);

        /**
         *
         */
        LinearLayout layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);

		if (selectionMode == SelectionMode.MODE_OPEN) {
			layoutCreate.setVisibility(View.GONE);
			setTitle(R.string.Open_File);
		} else {
			layoutCreate.setVisibility(View.VISIBLE);
			setTitle(R.string.Save_File);
		}

		final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}

		});
		final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
		createButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mFileName.getText().length() > 0) {
					getIntent()
							.putExtra(
									TPStrings.RESULT_PATH,
									currentPath + TPStrings.SLASH
											+ mFileName.getText());
					setResult(RESULT_OK, getIntent());
					finish();
				}
			}
		});


        SharedPreferences settings = getSharedPreferences(TPStrings.FILE_DIALOG, 0);
        rootPath = Environment.getExternalStorageDirectory().getPath();
        //rootPath = Environment.getRootDirectory().getPath();
		String startPath = settings.getString(TPStrings.START_PATH, rootPath);
        currentPath = startPath;

		readDir(startPath);
	}

	private void readDir(String dirPath) {
		currentPath = dirPath;

		path = new ArrayList<String>();
		mList = new ArrayList<HashMap<String, Object>>();

		File f = new File(currentPath);
		File[] files = f.listFiles();
		if (files == null) { //in case we can not show this
			currentPath = rootPath;
			f = new File(currentPath);
			files = f.listFiles();
		}

		myPath.setText(getString(R.string.Location, currentPath));

		parentPath = f.getParent();
        File parentFolder = new File(parentPath);
        if (!parentFolder.canRead()) {
            parentPath = parentFolder.getParent();
        }

		TreeMap<String, String> dirsMap = new TreeMap<String, String>();
		TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();

		TreeMap<String, String> filesMap = new TreeMap<String, String>();
		TreeMap<String, String> filesPathMap = new TreeMap<String, String>();

        if (parentPath != null) {
            dirsMap.put(TPStrings.FOLDER_UP, TPStrings.FOLDER_UP);
            dirsPathMap.put(TPStrings.FOLDER_UP, parentPath);
        }

		for (File file : files) {
			if (file.isDirectory()) {
				String dirName = file.getName();
				dirsMap.put(dirName, dirName);
				dirsPathMap.put(dirName, file.getPath());
			} else {
				filesMap.put(file.getName(), file.getName());
				filesPathMap.put(file.getName(), file.getPath());
			}
		}
        path.addAll(dirsPathMap.values());
        path.addAll(filesPathMap.values());

		SimpleAdapter fileList = new SimpleAdapter(this, mList,
				R.layout.file_dialog_row, new String[] { TPStrings.ITEM_KEY,
				TPStrings.ITEM_IMAGE }, new int[] { R.id.fdrowtext,
				R.id.fdrowimage });

		for (String dir : dirsMap.tailMap(TPStrings.EMPTY).values()) {
			addItem(dir, R.drawable.folder);
		}

		for (String file : filesMap.tailMap(TPStrings.EMPTY).values()) {
			addItem(file, R.drawable.file);
		}

		fileList.notifyDataSetChanged();

		setListAdapter(fileList);
	}


	private void addItem(String fileName, int imageId) {
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put(TPStrings.ITEM_KEY, fileName);
		item.put(TPStrings.ITEM_IMAGE, imageId);
		mList.add(item);
	}

	private void saveStartPath(String currentPath) {
        SharedPreferences settings = getSharedPreferences(TPStrings.FILE_DIALOG, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TPStrings.START_PATH, currentPath);
		editor.commit();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
        File file = new File(path.get(position));
        if (file.isDirectory()) {
            readDir(path.get(position));
            saveStartPath(currentPath);
		} else {
			saveStartPath(currentPath);
			v.setSelected(true);

			getIntent().putExtra(TPStrings.RESULT_PATH, file.getPath());
			setResult(RESULT_OK, getIntent());
			finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (parentPath != null && !currentPath.equals(rootPath)) {
				readDir(parentPath);
			} else {
				return super.onKeyDown(keyCode, event);
			}
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	String l(int id) {
		return getBaseContext().getResources().getString(id);
	}
}