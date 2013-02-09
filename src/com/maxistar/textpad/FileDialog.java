package com.maxistar.textpad;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FileDialog extends ListActivity {

	private List<String> path = null;
	private TextView myPath;
	private EditText mFileName;
	private ArrayList<HashMap<String, Object>> mList;

	// private Button selectButton;

	// private LinearLayout layoutSelect;
	private LinearLayout layoutCreate;
	// private InputMethodManager inputManager;
	private String parentPath;
	private String currentPath = TPStrings.ROOT;
	private String startPath = TPStrings.ROOT;
	private int selectionMode = SelectionMode.MODE_CREATE;

	private File selectedFile;
	private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED, getIntent());

		setContentView(R.layout.file_dialog_main);
		myPath = (TextView) findViewById(R.id.path);
		mFileName = (EditText) findViewById(R.id.fdEditTextFile);

		// inputManager = (InputMethodManager)
		// getSystemService(INPUT_METHOD_SERVICE);

		/*
		 * selectButton = (Button) findViewById(R.id.fdButtonSelect);
		 * selectButton.setEnabled(false); selectButton.setOnClickListener(new
		 * OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { if (selectedFile != null) {
		 * getIntent().putExtra(TPStrings.RESULT_PATH, selectedFile.getPath());
		 * setResult(RESULT_OK, getIntent()); finish(); } } });
		 */

		/*
		 * final Button newButton = (Button) findViewById(R.id.fdButtonNew);
		 * newButton.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { setCreateVisible(v);
		 * 
		 * mFileName.setText(TPStrings.EMPTY); mFileName.requestFocus(); } });
		 */
		mFileName.setText(TPStrings.NEW_FILE_TXT);
		selectionMode = getIntent().getIntExtra(TPStrings.SELECTION_MODE,
				SelectionMode.MODE_CREATE);

		// layoutSelect = (LinearLayout)
		// findViewById(R.id.fdLinearLayoutSelect);
		layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);

		if (selectionMode == SelectionMode.MODE_OPEN) {
			// newButton.setEnabled(false);
			layoutCreate.setVisibility(View.GONE);
			setTitle("Open File");
		} else {
			layoutCreate.setVisibility(View.VISIBLE);
			setTitle("Save File");
		}

		final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// setSelectVisible(v);
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

		// startPath = getIntent().getStringExtra(TPStrings.START_PATH);
		// startPath = Environment.getExternalStorageDirectory().getPath();

		settings = getSharedPreferences("FileDialog", 0);
		editor = settings.edit();
		startPath = settings.getString("startPath", Environment
				.getExternalStorageDirectory().getPath());

		if (startPath != null) {
			getDir(startPath);
		} else {
			getDir(TPStrings.ROOT);
		}
	}

	private void getDir(String dirPath) {

		boolean useAutoSelection = dirPath.length() < currentPath.length();

		Integer position = lastPositions.get(parentPath);

		getDirImpl(dirPath);

		if (position != null && useAutoSelection) {
			getListView().setSelection(position);
		}

	}

	private void getDirImpl(final String dirPath) {

		currentPath = dirPath;

		final List<String> item = new ArrayList<String>();
		path = new ArrayList<String>();
		mList = new ArrayList<HashMap<String, Object>>();

		File f = new File(currentPath);
		File[] files = f.listFiles();
		if (files == null) {
			currentPath = TPStrings.ROOT;
			f = new File(currentPath);
			files = f.listFiles();
		}
		myPath.setText(getText(R.string.Location) + TPStrings.COLON
				+ TPStrings.SPACE + currentPath);

		if (!currentPath.equals(TPStrings.ROOT)) {

			// item.add(TPStrings.ROOT);
			// addItem(TPStrings.ROOT, R.drawable.folder);
			// path.add(TPStrings.ROOT);

			// item.add(TPStrings.UP_SLASH);
			// addItem(TPStrings.UP_SLASH, R.drawable.folder);
			// path.add(f.getParent());
			parentPath = f.getParent();

		}

		TreeMap<String, String> dirsMap = new TreeMap<String, String>();
		TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
		TreeMap<String, String> filesMap = new TreeMap<String, String>();
		TreeMap<String, String> filesPathMap = new TreeMap<String, String>();
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
		item.addAll(dirsMap.tailMap(TPStrings.EMPTY).values());
		item.addAll(filesMap.tailMap(TPStrings.EMPTY).values());
		path.addAll(dirsPathMap.tailMap(TPStrings.EMPTY).values());
		path.addAll(filesPathMap.tailMap(TPStrings.EMPTY).values());

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

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		File file = new File(path.get(position));

		// setSelectVisible(v);

		if (file.isDirectory()) {
			// selectButton.setEnabled(false);
			if (file.canRead()) {
				lastPositions.put(currentPath, position);
				getDir(path.get(position));
				editor.putString("startPath", currentPath);
				editor.commit();
			} else {
				new AlertDialog.Builder(this)
						.setIcon(R.drawable.icon)
						.setTitle(
								TPStrings.RECT_OPEN + file.getName()
										+ TPStrings.RECT_CLOSE
										+ TPStrings.SPACE
										+ getText(R.string.cant_read_folder))
						.setPositiveButton(l(R.string.OK),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {

									}
								}).show();
			}
		} else {
			selectedFile = file;
			editor.putString("startPath", currentPath);
			editor.commit();
			v.setSelected(true);

			getIntent().putExtra(TPStrings.RESULT_PATH, selectedFile.getPath());
			setResult(RESULT_OK, getIntent());
			finish();
			// selectButton.setEnabled(true);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			// selectButton.setEnabled(false);

			// if (layoutCreate.getVisibility() == View.VISIBLE) {
			// layoutCreate.setVisibility(View.GONE);
			// layoutSelect.setVisibility(View.VISIBLE);
			// } else {
			// if(currentPath.contains("\""))
			// if (!currentPath.equals(startPath)) {
			if (currentPath.indexOf("/", 2) != -1) {
				getDir(parentPath);
			} else {
				return super.onKeyDown(keyCode, event);
			}

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/*
	 * private void setCreateVisible(View v) {
	 * layoutCreate.setVisibility(View.VISIBLE);
	 * //layoutSelect.setVisibility(View.GONE);
	 * 
	 * inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
	 * //selectButton.setEnabled(false); }
	 */

	/*
	 * private void setSelectVisible(View v) {
	 * layoutCreate.setVisibility(View.GONE);
	 * //layoutSelect.setVisibility(View.VISIBLE);
	 * 
	 * inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
	 * //selectButton.setEnabled(false); }
	 */

	String l(int id) {
		return getBaseContext().getResources().getString(id);
	}
}