package com.maxistar.textpad.activities;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Toast;

import com.maxistar.textpad.FileDialog;
import com.maxistar.textpad.R;
import com.maxistar.textpad.SelectionMode;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.service.SettingsService;
import com.maxistar.textpad.TPStrings;
import com.maxistar.textpad.service.AlternativeUrlsService;
import com.maxistar.textpad.service.RecentFilesService;
import com.maxistar.textpad.service.ThemeService;
import com.maxistar.textpad.utils.EditTextUndoRedo;
import com.maxistar.textpad.utils.FileNameHelper;
import com.maxistar.textpad.utils.System;
import com.maxistar.textpad.utils.TextConverter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.documentfile.provider.DocumentFile;


import android.content.DialogInterface;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

public class EditorActivity extends AppCompatActivity {

    private static final String STATE_FILENAME = "filename";
    private static final String STATE_CHANGED = "changed";
    private static final String STATE_CURSOR_POSITION = "cursor-position";

    private static final int REQUEST_OPEN = 1;
    private static final int REQUEST_SAVE = 2;

    private static final int REQUEST_SETTINGS = 3;



    private static final int ACTION_CREATE_FILE = 4;
    private static final int ACTION_OPEN_FILE = 5;

    private static final int REQUEST_AUTHORIZE_FOLDER = 6;


    private static final int DO_NOTHING = 0;
    private static final int DO_OPEN = 1;
    private static final int DO_NEW = 2;
    private static final int DO_EXIT = 3;
    private static final int DO_OPEN_RECENT = 4;
    private static final int DO_SHOW_SETTINGS = 5;

    private static final String LOG_TAG = "TextEditor";



    String [] mimeTypes = {
            "*/*",
            "text/*",
            "plain/*",
            "text/javascript",
            "application/ecmascript",
            "application/javascript"
    };

    private EditText mText;
    private ScrollView scrollView;
    
    String urlFilename = TPStrings.EMPTY;

    Uri lastTriedSystemUri = null;

    
    boolean changed = false;

    boolean exitDialogShown = false;

    private int next_action = DO_NOTHING; // to figure out better way

    private String next_action_filename = "";

    static int selectionStart = 0;

    SettingsService settingsService;

    RecentFilesService recentFilesService;

    AlternativeUrlsService alternativeUrlsService;

    private QueryTextListener queryTextListener;

    private MenuItem searchItem;

    private TextWatcher textWatcher;

    EditTextUndoRedo editTextUndoRedo;

    WebView mWebView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsService = ServiceLocator.getInstance().getSettingsService(this.getApplicationContext());
        recentFilesService = ServiceLocator.getInstance().getRecentFilesService();
        alternativeUrlsService = ServiceLocator.getInstance().getAlternativeUrlsService();

        setContentView(R.layout.main);
        mText = this.findViewById(R.id.editText1);
        mText.setBackgroundResource(android.R.color.transparent);
        editTextUndoRedo = new EditTextUndoRedo(mText);
        scrollView = findViewById(R.id.vscroll);
        applyPreferences();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {

            verifyPermissions(this);

            Intent i = this.getIntent();
            if (TPStrings.ACTION_VIEW.equals(i.getAction())) {
                Uri u = i.getData();
                if (u != null) {
                    openFileByUri(u);
                }
            } else { // it this is just created
                if (isFilenameEmpty()) {
                    if (settingsService.isOpenLastFile()) {
                        openLastFile();
                    }
                }
            }
        }

        setTextWatcher();
        updateTitle();
        mText.requestFocus();

        settingsService.applyLocale(this.getBaseContext());
    }

    private void openFileByUri(Uri u) {
        if (useAndroidManager()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (settingsService.isAlternativeFileAccess() &&
                    alternativeUrlsService.hasAlternativeUrl(u, getApplicationContext())) {
                    openNamedFile(alternativeUrlsService.getAlternativeUrl(u, getApplicationContext()));
                } else {
                    openNamedFile(u);
                }
            }
        } else {
            openNamedFileLegacy(u.getPath());
        }
    }

    private void setTextWatcher() {
        textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (changed) {
                    return;
                }
                changed = true;
                updateTitle();
            }
        };
    }

    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity Activity
     */
    public static void verifyPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        // Check if we have write permission
        int permission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK,
        };

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            activity.requestPermissions(
                    PERMISSIONS_STORAGE,
                    1
            );
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed()) {
            if (keyCode == KeyEvent.KEYCODE_S) {
                saveFile();
                return true;
            }
            else if (keyCode == KeyEvent.KEYCODE_Z) {
                editUndo();
                return true;
            }
            else if (keyCode == KeyEvent.KEYCODE_Y) {
                editRedo();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    protected void onResume() {
        super.onResume();
        String t = mText.getText().toString().toLowerCase(Locale.getDefault());
        mText.addTextChangedListener(textWatcher);
        if (selectionStart < t.length()) {
            mText.setSelection(selectionStart, selectionStart);
        }

        if (SettingsService.isLanguageWasChanged()) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }

        if (settingsService.useWakeLock()) {
            ServiceLocator.getInstance().getWakeLockService().acquireLock(this.getApplicationContext());
        }
    }

    protected void onPause() {
        if (settingsService.isAutosavingActive() && !isFilenameEmpty() && isChanged()) {
            this.saveFileIfNamed();
        }

        mText.removeTextChangedListener(textWatcher);
        selectionStart = mText.getSelectionStart();
        if (settingsService.useWakeLock()) {
            ServiceLocator.getInstance().getWakeLockService().releaseLock();
        }
        super.onPause();
    }

    private boolean isChanged() {
        return changed;
    }

    /**
     * @param state Bundle
     */
    private void restoreState(Bundle state) {
        urlFilename = state.getString(STATE_FILENAME);
        changed = state.getBoolean(STATE_CHANGED);
        selectionStart = state.getInt(STATE_CURSOR_POSITION);
    }

    /**
     * @param outState Bundle
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILENAME, urlFilename);
        outState.putBoolean(STATE_CHANGED, changed);
        outState.putInt(STATE_CURSOR_POSITION, mText.getSelectionStart());
    }

    protected void onStop() {
        super.onStop();
    }
    
    @Override
    public void onBackPressed() {
        if (this.changed && !exitDialogShown) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.You_have_made_some_changes)
                    .setMessage(R.string.Are_you_sure_to_quit)
                    .setNegativeButton(R.string.Yes, (arg0, arg1) -> {
                        EditorActivity.super.onBackPressed();
                        exitDialogShown = false;
                    })
                    .setPositiveButton(R.string.No, (arg0, arg1) -> {
                        //do nothing
                        exitDialogShown = false;
                    })
                    .setOnCancelListener(arg0 -> EditorActivity.super.onBackPressed())
                    .create()
                    .show();
            exitDialogShown = true;
        } else {
            super.onBackPressed();
        }
    }

    String formatString(int stringId, String parameter) {
        return this.getResources().getString(stringId, parameter);
    }

    boolean useAndroidManager() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return false;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            return true;
        }

        return !settingsService.isLegacyFilePicker();
    }

    void openLastFile() {
        if (!settingsService.getLastFilename().equals(TPStrings.EMPTY)) {
            if (useAndroidManager()) {
                Uri uri = Uri.parse(settingsService.getLastFilename());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // duplicated in useAndroidManager
                    this.openNamedFile(uri);
                }
            } else {
                this.openNamedFileLegacy(settingsService.getLastFilename());
            }
            showToast(formatString(R.string.opened_last_edited_file, settingsService.getLastFilename()));
        }
    }

    void updateTitle() {
        this.setTitle(getEditingTitle());
    }

    private String getEditingTitle() {
        String title;
        if (isFilenameEmpty()) {
            title = TPStrings.NEW_FILE_TXT;
        } else {
            Uri uri = Uri.parse(getFilename());
            title = FileNameHelper.getFilenameByUri(getApplicationContext(), uri);
        }
        if (changed) {
            title = title + TPStrings.STAR;
        }
        return title;
    }

    private String getFilename() {
        return urlFilename;
    }

    private boolean isFilenameEmpty() {
        return urlFilename.equals(TPStrings.EMPTY);
    }

    void applyPreferences() {
        applyFontFace();
        applyFontSize();
        applyColors();
    }

    private void applyFontFace() {
        mText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_VARIATION_NORMAL |
                InputType.TYPE_CLASS_TEXT);

        String font = settingsService.getFont();

        if (font.equals(TPStrings.FONT_SERIF))
            mText.setTypeface(Typeface.SERIF);
        else if (font.equals(TPStrings.FONT_SANS_SERIF))
            mText.setTypeface(Typeface.SANS_SERIF);
        else
            mText.setTypeface(Typeface.MONOSPACE);
    }

    private void applyFontSize() {
        String fontsize = settingsService.getFontSize();

        switch (fontsize) {
            case (SettingsService.SETTING_EXTRA_SMALL):
                mText.setTextSize(12.0f);
                break;
            case (SettingsService.SETTING_SMALL):
                mText.setTextSize(16.0f);
                break;
            case (SettingsService.SETTING_LARGE):
                mText.setTextSize(24.0f);
                break;
            case (SettingsService.SETTING_HUGE):
                mText.setTextSize(28.0f);
                break;
            case (SettingsService.SETTING_MEDIUM):
            default:
                mText.setTextSize(20.0f);
        }
    }

    private void applyColors() {
        mText.setHighlightColor(settingsService.getTextSelectionColor());
        if (settingsService.isThemeForced()) {
            ThemeService themeService = ServiceLocator.getInstance().getThemeService(this);
            themeService.applyColorTheme(this);
        }
        if (settingsService.isCustomTheme()) {
            scrollView.setBackgroundColor(settingsService.getBgColor());
            mText.setTextColor(settingsService.getFontColor());
        }
    }

    private QueryTextListener getQueryTextListener() {
        if (queryTextListener == null) {
            queryTextListener = new QueryTextListener();
        }
        return queryTextListener;
    }

    private void initSearch(MenuItem searchItem) {
        // Set up search view
        SearchView searchView = (SearchView) searchItem.getActionView();
        // Set up search view options and listener
        if (searchView != null) {
            searchView.setSubmitButtonEnabled(true);
            searchView.setIconified(false);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(getQueryTextListener());
            searchItem.setOnActionExpandListener(getQueryTextListener());
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        this.searchItem = menu.findItem(R.id.menu_document_search);

        MenuItem undoMenu = menu.findItem(R.id.menu_edit_undo);
        undoMenu.setEnabled(editTextUndoRedo.getCanUndo());

        MenuItem redoMenu = menu.findItem(R.id.menu_edit_redo);
        redoMenu.setEnabled(editTextUndoRedo.getCanRedo());
        
        updateRecentFiles(menu);

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            MenuItem printMenu = menu.findItem(R.id.menu_document_print);
            printMenu.setVisible(false);
        }

        return true;
    }

    private void updateRecentFiles(Menu menu) {
        MenuItem recentFilesMenuItem = menu.findItem(R.id.menu_document_open_last);
        if (settingsService.isShowLastEditedFiles()) {
            recentFilesMenuItem.setVisible(true);
        } else {
            recentFilesMenuItem.setVisible(false);
            return;
        }
        ArrayList<String> recentFiles = recentFilesService.getLastFiles(1, this.getApplicationContext());
        MenuItem recentFilesMenuItem1 = menu.findItem(R.id.menu_document_open_last1);
        MenuItem recentFilesMenuItem2 = menu.findItem(R.id.menu_document_open_last2);
        MenuItem recentFilesMenuItem3 = menu.findItem(R.id.menu_document_open_last3);
        MenuItem recentFilesMenuItem4 = menu.findItem(R.id.menu_document_open_last4);
        MenuItem recentFilesMenuItem5 = menu.findItem(R.id.menu_document_open_last5);

        int historySize = recentFiles.size();
        switch (historySize) {
            case 0:
                recentFilesMenuItem.setVisible(false);
                recentFilesMenuItem1.setVisible(false);
                recentFilesMenuItem2.setVisible(false);
                recentFilesMenuItem3.setVisible(false);
                recentFilesMenuItem4.setVisible(false);
                recentFilesMenuItem5.setVisible(false);
                break;
            case 1:
                recentFilesMenuItem.setVisible(true);
                recentFilesMenuItem1.setVisible(true);
                recentFilesMenuItem1.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(0)));
                recentFilesMenuItem2.setVisible(false);
                recentFilesMenuItem3.setVisible(false);
                recentFilesMenuItem4.setVisible(false);
                recentFilesMenuItem5.setVisible(false);
                break;
            case 2:
                recentFilesMenuItem.setVisible(true);
                recentFilesMenuItem1.setVisible(true);
                recentFilesMenuItem1.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(0)));
                recentFilesMenuItem2.setVisible(true);
                recentFilesMenuItem2.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(1)));
                recentFilesMenuItem3.setVisible(false);
                recentFilesMenuItem4.setVisible(false);
                recentFilesMenuItem5.setVisible(false);
                break;
            case 3:
                recentFilesMenuItem.setVisible(true);
                recentFilesMenuItem1.setVisible(true);
                recentFilesMenuItem1.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(0)));
                recentFilesMenuItem2.setVisible(true);
                recentFilesMenuItem2.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(1)));
                recentFilesMenuItem3.setVisible(true);
                recentFilesMenuItem3.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(2)));
                recentFilesMenuItem4.setVisible(false);
                recentFilesMenuItem5.setVisible(false);
                break;
            case 4:
                recentFilesMenuItem.setVisible(true);
                recentFilesMenuItem1.setVisible(true);
                recentFilesMenuItem1.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(0)));
                recentFilesMenuItem2.setVisible(true);
                recentFilesMenuItem2.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(1)));
                recentFilesMenuItem3.setVisible(true);
                recentFilesMenuItem3.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(2)));
                recentFilesMenuItem4.setVisible(true);
                recentFilesMenuItem4.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(3)));
                recentFilesMenuItem5.setVisible(false);
                break;
            default:
                recentFilesMenuItem.setVisible(true);
                recentFilesMenuItem1.setVisible(true);
                recentFilesMenuItem1.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(0)));
                recentFilesMenuItem2.setVisible(true);
                recentFilesMenuItem2.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(1)));
                recentFilesMenuItem3.setVisible(true);
                recentFilesMenuItem3.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(2)));
                recentFilesMenuItem4.setVisible(true);
                recentFilesMenuItem4.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(3)));
                recentFilesMenuItem5.setVisible(true);
                recentFilesMenuItem5.setTitle(FileNameHelper.getFilenameByUri(getApplicationContext(), recentFiles.get(4)));
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Close text search
        if (searchItem != null && searchItem.isActionViewExpanded()) {
            searchItem.collapseActionView();
        }

        int itemId = item.getItemId();
        if (itemId == R.id.menu_document_open) {
            openFile();
        } else if (itemId == R.id.menu_document_open_other) {
            openFile();
        } else if (itemId == R.id.menu_document_search) {
            initSearch(item);
        } else if (itemId == R.id.menu_document_open_last1) {
            openRecentFile(0);
        } else if (itemId == R.id.menu_document_open_last2) {
            openRecentFile(1);
        } else if (itemId == R.id.menu_document_open_last3) {
            openRecentFile(2);
        } else if (itemId == R.id.menu_document_open_last4) {
            openRecentFile(3);
        } else if (itemId == R.id.menu_document_open_last5) {
            openRecentFile(4);
        } else if (itemId == R.id.menu_document_new) {
            newFile();
        } else if (itemId == R.id.menu_document_save) {
            saveFile();
        } else if (itemId == R.id.menu_document_save_as) {
            saveAs();
        } else if (itemId == R.id.menu_edit_undo) {
            editUndo();
        } else if (itemId == R.id.menu_edit_redo) {
            editRedo();
        } else if (itemId == R.id.menu_document_share) {
            shareText();
        } else if (itemId == R.id.menu_document_print) {
            printText();
        } else if (itemId == R.id.menu_document_settings) {
            showSettings();
        } else if (itemId == R.id.menu_exit) {
            exitApplication();
        }

        return super.onOptionsItemSelected(item);
    }

    private void printText() {
         if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
             // Create a WebView object specifically for printing
             WebView webView = new WebView(this);
             webView.setWebViewClient(new WebViewClient() {

                 public boolean shouldOverrideUrlLoading(WebView view, String url) {
                     return false;
                 }

                 @Override
                 public void onPageFinished(WebView view, String url) {
                     createWebPrintJob(view);
                     mWebView = null;
                 }
             });

             // Generate an HTML document on the fly:
             String htmlDocument = "<html><body><pre style='padding:1.5cm 1cm 1.5cm 2cm'>" +
                     mText.getText() +
                     "</pre></body></html>";
             webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null);

             mWebView = webView;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createWebPrintJob(WebView webView) {

        // Get a PrintManager instance
        PrintManager printManager = null;
            printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        String jobName = getString(R.string.app_name) + " Document";

        // Get a print adapter instance
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);

        // Create a print job with name and adapter instance
        PrintJob printJob = printManager.print(jobName, printAdapter,
                new PrintAttributes.Builder().build());

        // Save the job object for later status checking
        List<PrintJob> printJobs = new ArrayList<>();
        printJobs.add(printJob);
    }

    private void shareText() {
        String textToShare = this.mText.getText().toString();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void showSettings() {
        if (changed) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.File_not_saved)
                    .setMessage(R.string.Save_current_file)
                    .setPositiveButton(R.string.Yes,
                            (dialog, which) -> {
                                // Stop the activity
                                next_action = DO_SHOW_SETTINGS;
                                saveFile();
                            })
                    .setNegativeButton(R.string.No,
                            (dialog, which) -> showSettingsActivity()).show();
        } else {
            showSettingsActivity();
        }
    }

    private void showSettingsActivity() {
        Intent intent = new Intent(this.getBaseContext(),
                SettingsActivity.class);
        this.startActivityForResult(intent, REQUEST_SETTINGS);
    }

    private void openRecentFile(int i) {
        ArrayList<String> lastFiles = recentFilesService.getLastFiles(1, getApplicationContext());
        if (i >= lastFiles.size()) {
            return;
        }
        final String filename = lastFiles.get(i);
        if (changed) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.File_not_saved)
                    .setMessage(R.string.Save_current_file)
                    .setPositiveButton(R.string.Yes,
                            (dialog, which) -> {
                                // Stop the activity
                                next_action = DO_OPEN_RECENT;
                                next_action_filename = filename;
                                EditorActivity.this.saveFile();
                            })
                    .setNegativeButton(R.string.No,
                            (dialog, which) -> openFileByName(filename)).show();
        } else {
            openFileByName(filename);
        }
    }

    private void openFileByName(String filename) {
        if (useAndroidManager()) {
            Uri uri = Uri.parse(filename);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //duplicated in useAndroidManager
                this.openNamedFile(uri);
            }
        } else {
            this.openNamedFileLegacy(filename);
        }
    }

    public void newFile() {
        if (changed) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.File_not_saved)
                    .setMessage(R.string.Save_current_file)
                    .setPositiveButton(R.string.Yes,
                            (dialog, which) -> {
                                // Stop the activity
                                next_action = DO_NEW;
                                EditorActivity.this.saveFile();
                            })
                    .setNegativeButton(R.string.No,
                            (dialog, which) -> clearFile()).show();
        } else {
            clearFile();
        }
    }

    public void clearFile() {
        mText.setText(TPStrings.EMPTY);
        setFilename(TPStrings.EMPTY);
        initEditor();
        updateTitle();
    }

    private void setFilename(String value) {
        this.urlFilename = value;
        storeLastFileName(value);
    }

    private void storeLastFileName(String value) {
        if (isFilenameEmpty()) {
            return;
        }
        if (!settingsService.isShowLastEditedFiles()) {
            return;
        }
        recentFilesService.addRecentFile(value, getApplicationContext());
    }

    protected void initEditor() {
        changed = false;
        editTextUndoRedo.clearHistory();
        queryTextListener = null;
        searchItem = null;
    }

    protected void editRedo() {
        editTextUndoRedo.redo();
    }

    protected void editUndo() {
        editTextUndoRedo.undo();
    }

    protected void saveAs() {
        if (useAndroidManager()) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_TITLE, TPStrings.NEW_FILE_TXT);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            startActivityForResult(intent, ACTION_CREATE_FILE);
        } else {
            Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
            this.startActivityForResult(intent, REQUEST_SAVE);
        }
    }

    protected void openFile() {
        if (changed) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.File_not_saved)
                    .setMessage(R.string.Save_current_file)
                    .setPositiveButton(R.string.Yes,
                            (dialog, which) -> {
                                // Stop the activity
                                next_action = DO_OPEN;
                                saveFile();
                            })
                    .setNegativeButton(R.string.No,
                            (dialog, which) -> openNewFile()).show();
        } else {
            openNewFile();
        }
    }

    protected void exitApplication() {
        if (changed) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.File_not_saved)
                    .setMessage(R.string.Save_current_file)
                    .setPositiveButton(R.string.Yes,
                            (dialog, which) -> {
                                // Stop the activity
                                next_action = DO_EXIT;
                                EditorActivity.this.saveFile();
                            })
                    .setNegativeButton(R.string.No,
                            (dialog, which) -> System.exitFromApp(EditorActivity.this)).show();
        } else {
            System.exitFromApp(EditorActivity.this);
        }
    }
    
    protected void selectFileUsingAndroidSystemPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_TITLE, TPStrings.NEW_FILE_TXT);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        startActivityForResult(intent, ACTION_OPEN_FILE);
    }

    protected void openNewFile() {
        if (useAndroidManager()) {
            selectFileUsingAndroidSystemPicker();
        } else {
            Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
            intent.putExtra(TPStrings.SELECTION_MODE, SelectionMode.MODE_OPEN);
            this.startActivityForResult(intent, REQUEST_OPEN);
        }
    }

    protected void saveFile() {
        if (isFilenameEmpty()) {
            saveAs();
        } else {
            saveFileIfNamed();
        }
    }

    protected void saveFileIfNamed() {
        if (useAndroidManager()) {
            saveNamedFile();
        } else {
            saveNamedFileLegacy();
        }
    }

    protected void saveFileWithConfirmation() {
        if (this.fileAlreadyExists()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.File_already_exists)
                    .setMessage(R.string.Existing_file_will_be_overwritten)
                    .setPositiveButton(R.string.Yes,
                            (dialog, which) -> {
                                // Stop the activity
                                next_action = DO_OPEN;
                                EditorActivity.this.saveFile();
                            }).setNegativeButton(R.string.No,
                    (dialog, which) -> {
                        //do nothing!!
                    }).show();
        } else {
            saveFileIfNamed();
        }
    }

    protected boolean fileAlreadyExists() {
        File f = new File(getFilename());
        return f.exists();
    }

    protected void saveNamedFileLegacy() {
        try {
            File f = new File(getFilename());
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    showToast(R.string.Can_not_write_file);
                    return;
                }
            }

            FileOutputStream fos = new FileOutputStream(f);
            String s = this.mText.getText().toString();

            s = applyEndings(s);

            fos.write(s.getBytes(settingsService.getFileEncoding()));
            fos.close();
            showToast(R.string.File_Written);
            initEditor();
            updateTitle();

            if (next_action == DO_OPEN) {
                // because of multithread nature
                // figure out better way to do
                // it
                next_action = DO_NOTHING;
                openNewFile();
            }
            if (next_action == DO_NEW) {
                // because of multithread nature
                // figure out better way to do
                // it
                next_action = DO_NOTHING;
                clearFile();
            }
            if (next_action == DO_SHOW_SETTINGS) { // because of multithread nature
                next_action = DO_NOTHING;
                showSettingsActivity();
            }
            if (next_action == DO_OPEN_RECENT) {
                next_action = DO_NOTHING;
                openFileByName(next_action_filename);
            }
            if (next_action == DO_EXIT) {
                exitApplication();
            }
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
            this.showToast(R.string.Can_not_write_file);
        } catch (Exception e) {
            this.showToast(R.string.Can_not_write_file);
        }
    }

    protected void saveFile(Uri uri) throws IOException {
        ContentResolver contentResolver = getContentResolver();
        OutputStream outputStream = contentResolver.openOutputStream(uri, "wt");
        if (outputStream == null) {
            throw new IOException();
        }

        try {
            String s = this.mText.getText().toString();

            s = applyEndings(s);

            outputStream.write(s.getBytes(settingsService.getFileEncoding()));
        } finally {
            outputStream.close();
        }
    }

    protected void saveNamedFile() {
        try {
            Uri uri = Uri.parse(getFilename());
            saveFile(uri);

            showToast(R.string.File_Written);
            initEditor();
            updateTitle();

            if (next_action == DO_OPEN) {   // because of multithread nature
                next_action = DO_NOTHING;
                openNewFile();
            }
            if (next_action == DO_NEW) { // because of multithread nature
                next_action = DO_NOTHING;
                clearFile();
            }
            if (next_action == DO_SHOW_SETTINGS) { // because of multithread nature
                next_action = DO_NOTHING;
                showSettingsActivity();
            }
            if (next_action == DO_OPEN_RECENT) {
                next_action = DO_NOTHING;
                openFileByName(next_action_filename);
            }
            if (next_action == DO_EXIT) {
                exitApplication();
            }
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
            this.showToast(R.string.Can_not_write_file);
        } catch (Exception e) {
            this.showToast(R.string.Can_not_write_file);
        }
    }

    protected void openNamedFileLegacy(String filename) {
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
                    settingsService.getFileEncoding());

            ttt = toUnixEndings(ttt);

            mText.setText(ttt);
            editTextUndoRedo.clearHistory();

            showToast(getBaseContext().getResources().getString(R.string.File_opened_, filename));
            initEditor();
            this.setFilename(filename);
            if (!settingsService.getLastFilename().equals(filename)) {
                settingsService.setLastFilename(filename, this.getApplicationContext());
            }
            selectionStart = 0;
            updateTitle();
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
            this.showToast(R.string.Can_not_read_file);
        } catch (Exception e) {
            this.showToast(R.string.Can_not_read_file);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void openNamedFile(final Uri uri) {
        try {
            ContentResolver contentResolver = getContentResolver();

            boolean isReadOnly = false;

            //DocumentFile file = DocumentFile.fromSingleUri(getApplicationContext(), uri);
            //isReadOnly = !file.canWrite();


            try {
                // Try opening the file with write mode
                ParcelFileDescriptor pfdWrite = getContentResolver().openFileDescriptor(uri, "rw");
                if (pfdWrite == null) {
                    isReadOnly = true;
                } else {
                    pfdWrite.close(); // Close it if opened successfully
                }
            } catch (Exception e) {
                isReadOnly = true;
            }

            if (isReadOnly) {
                // Inform the user that the file is read-only
                //Toast.makeText(this, "The file is opened for reading only. Please open it for writing or save it with a different name.", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(this::showReadOnlyDialog, 1000);
            }



            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                throw new IOException();
            }
            int size = inputStream.available();
            DataInputStream dis = new DataInputStream(inputStream);
            byte[] b = new byte[size];
            int length = dis.read(b, 0, size);

            String ttt = new String(b, 0, length, settingsService.getFileEncoding());
            ttt = toUnixEndings(ttt);

            inputStream.close();
            dis.close();

            mText.setText(ttt);
            editTextUndoRedo.clearHistory();

            showToast(getBaseContext().getResources().getString(R.string.File_opened_, getFilename()));
            initEditor();
            setFilename(uri.toString());
            if (!settingsService.getLastFilename().equals(getFilename())) {
                settingsService.setLastFilename(getFilename(), this.getApplicationContext());
            }
            selectionStart = 0;
            if (lastTriedSystemUri != null) {
                alternativeUrlsService.addAlternativeUrl(lastTriedSystemUri, uri, getApplicationContext());
                lastTriedSystemUri = null;
            }
            updateTitle();
        } catch (FileNotFoundException e) {
            if (isAccessDeniedException(e)) {
                showAlternativeFileDialog(uri);
            } else {
                this.showToast(R.string.File_not_found);
            }
        } catch (IOException e) {
            this.showToast(R.string.Can_not_read_file);
        } catch (Exception e) {
            this.showToast(R.string.Can_not_read_file);
        }
    }

    public void showReadOnlyDialog() {
        Context context = this;
        // Message text with a clickable link
        SpannableString spannableMessage = new SpannableString("This file is in read only mode. You can save it with a different name, or you can open it again using 'Open' command from the application menu. Alternatively, you can allow the parent folder for write access, so next time this file will be writable. Click 'authorize folder' to allow all files in the folder to be writable. Click here to read more.");

        // Set the clickable part
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                // Open the link in an external browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://your-link.com"));
                context.startActivity(browserIntent);
            }
        };
        spannableMessage.setSpan(clickableSpan, spannableMessage.length() - "Click here to read more.".length(), spannableMessage.length(), 0);
        spannableMessage.setSpan(new UnderlineSpan(), spannableMessage.length() - "Click here to read more.".length(), spannableMessage.length(), 0);

        // Creating the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("File Access");

        // Including the message and link in the dialog
        builder.setMessage(spannableMessage);
        builder.setCancelable(true);

        // Setting the dialog buttons
        builder.setPositiveButton("Authorize Folder", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                authorizeFolder();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Make the link clickable
        ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    void authorizeFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_AUTHORIZE_FOLDER);
    }

    private void showAlternativeFileDialog(final Uri uri) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.AlternativeFileAccessTitle)
            .setMessage(R.string.SelectAlternativeLocationForFile)
            .setNegativeButton(R.string.Yes, (arg0, arg1) -> {
                lastTriedSystemUri = uri;
                selectFileUsingAndroidSystemPicker();
            })
            .setPositiveButton(R.string.No, (arg0, arg1) -> lastTriedSystemUri = null)
            .setOnCancelListener(arg0 -> {
                lastTriedSystemUri = null;
                EditorActivity.super.onBackPressed();
        })
            .create()
            .show();
    }

    private boolean isAccessDeniedException(FileNotFoundException e) {
        if (!settingsService.isAlternativeFileAccess()) {
            return false;
        }
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return (message.contains("EACCES"));
    }

    /**
     * @param value String to fix
     * @return Fixed String
     */
    String applyEndings(String value){
        String to = settingsService.getDelimiters();
        value = TextConverter.getInstance().applyEndings(value, to);
        return value;
    }

    /**
     * @param value Value
     *
     * @return String
     */
    String toUnixEndings(String value) {
        String from = settingsService.getDelimiters();
        if (TPStrings.DEFAULT.equals(from)) {
            return value; //this way we spare memory but will be unable to fix delimiters
        }

        //we should anyway fix any line delimiters
        //replace \r\n first, then \r into \n this way we will get pure unix ending used in android
        return TextConverter.getInstance().applyEndings(value, TextConverter.UNIX);
    }

    int getSearchSelectionColor() {
        return settingsService.getSearchSelectionColor();
    }

    /**
     *
     */
    @SuppressLint("WrongConstant")
    public synchronized void onActivityResult(
        final int requestCode,
        int resultCode,
        final Intent data
    ) {

        if (requestCode == REQUEST_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                setFilename(
                    data.getStringExtra(TPStrings.RESULT_PATH)
                );
                this.saveFileWithConfirmation();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                showToast(R.string.Operation_Canceled);
            }
        } else if (requestCode == REQUEST_OPEN) {
            if (resultCode == Activity.RESULT_OK) {
                this.openNamedFileLegacy(data.getStringExtra(TPStrings.RESULT_PATH));
            } else if (resultCode == Activity.RESULT_CANCELED) {
                showToast(R.string.Operation_Canceled);
            }
        } else if (requestCode == REQUEST_SETTINGS) {
            applyPreferences();
        } else if (requestCode == ACTION_OPEN_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri;
            if (data != null) {
                uri = data.getData();
                if (uri != null) {
                    // Check for the freshest data.
                    persistUriPermissions(data);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        openNamedFile(uri);
                    }
                }
            }
        } else if (requestCode == ACTION_CREATE_FILE) {
            if (data != null) {
                persistUriPermissions(data);
                Uri uri = data.getData();
                if (uri != null) {
                    setFilename(uri.toString());
                    this.saveFileWithConfirmation();
                }
            }
        } else if (requestCode == REQUEST_AUTHORIZE_FOLDER) {
            Uri folderUri = data.getData();
            if (folderUri != null) {
                getContentResolver().takePersistableUriPermission(folderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            persistUriPermissions(data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("WrongConstant")
    private void persistUriPermissions(Intent data) {
        // Check for the freshest data.
        Uri uri = data.getData();
        if (uri == null) {
           return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
        }
    }

    protected void showToast(int toast_str) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, toast_str, duration);
        toast.show();
    }

    protected void showToast(String toast_str) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, toast_str, duration);
        toast.show();
    }

    // QueryTextListener
    private class QueryTextListener
            implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener
    {
        private final BackgroundColorSpan span = new BackgroundColorSpan(getSearchSelectionColor());
        private final Editable editable;
        private Matcher matcher;
        private int index;
        private final int height;

        public QueryTextListener() {
            // Use regex search and spannable for highlighting
            height = scrollView.getHeight();
            editable = mText.getEditableText();
        }

        // onQueryTextChange
        @Override
        public boolean onQueryTextChange(String newText)
        {
            // Reset the index and clear highlighting
            if (newText.length() == 0) {
                index = 0;
                editable.removeSpan(span);
                return false;
            }

            // Check pattern
            try {
                String escapedTextToFind = Pattern.quote(newText);
                Pattern pattern = Pattern.compile(escapedTextToFind, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(editable);
            } catch (Exception e) {
                return false;
            }

            // Find text
            if (matcher.find(index)) {
                // Check layout
                if (mText.getLayout() == null) {
                    return false;
                }
                doSearch();
            } else {
                index = 0;
            }
            return true;
        }

        // onQueryTextSubmit
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            // Find next text
            if (matcher!= null && matcher.find()) {
                // Check layout
                if (mText.getLayout() == null) {
                    return false;
                }
                doSearch();
            } else {
                Toast.makeText(
                        EditorActivity.this,
                        formatString(R.string.s_not_found, query),
                        Toast.LENGTH_SHORT
                ).show();
                matcher.reset();
                index = 0;
                editable.removeSpan(span);
            }

            return true;
        }

        private void doSearch() {
            // Get index
            index = matcher.start();

            // Get text position
            int line = mText.getLayout().getLineForOffset(index);
            int pos = mText.getLayout().getLineBaseline(line);

            // Scroll to it
            scrollView.smoothScrollTo(0, pos - height / 2);

            // Highlight it
            editable.setSpan(
                    span,
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem menuItem) {
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem menuItem) {
            Log.d(LOG_TAG, "onClose");
            editable.removeSpan(span);
            mText.requestFocus();
            queryTextListener = null;
            return true;
        }
    }
}
