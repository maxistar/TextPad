package com.maxistar.textpad;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Toast;
import com.maxistar.textpad.utils.System;
import com.maxistar.textpad.utils.TextConverter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;

public class EditorActivity extends AppCompatActivity {

    private static final String STATE_FILENAME = "filename";
    private static final String STATE_CHANGED = "changed";

    private static final int REQUEST_OPEN = 1;
    private static final int REQUEST_SAVE = 2;
    private static final int REQUEST_SETTINGS = 3;

    private static final int ACTION_OPTION_FILE = 4;
    private static final int ACTION_SAVE_FILE = 5;

    private static final int DO_NOTHING = 0;
    private static final int DO_OPEN = 1;
    private static final int DO_NEW = 2;
    private static final int DO_EXIT = 3;

    String [] mimeTypes = {
            "text/*",
            "plain/*",
            "text/javascript",
            "application/ecmascript",
            "application/javascript"
    };

    private EditText mText;
    private ScrollView scrollView;
    
    String urlFilename = TPStrings.EMPTY;
    
    boolean changed = false;
    boolean exitDialogShown = false;

    private int next_action = DO_NOTHING; // to figure out better way

    static int selectionStart = 0;

    SettingsService settingsService;

    private MenuItem searchItem;

    private TextWatcher textWatcher;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsService = ServiceLocator.getInstance().getSettingsService(this.getApplicationContext());

        setContentView(R.layout.main);
        mText = this.findViewById(R.id.editText1);
        scrollView = findViewById(R.id.vscroll);
        applyPreferences();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {

            verifyPermissions(this);

            Intent i = this.getIntent();
            if (TPStrings.ACTION_VIEW.equals(i.getAction())) {
                Uri u = i.getData();
                if (useAndroidManager()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        openNamedFile(u);
                    }
                } else {
                    openNamedFileLegacy(u.getPath());
                }
            } else { // it this is just created
                if (this.urlFilename.equals(TPStrings.EMPTY)) {
                    if (settingsService.isOpenLastFile()) {
                        openLastFile();
                    }
                }
            }
        }

        textWatcher = new TextWatcher() {
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

        updateTitle();
        mText.requestFocus();

        settingsService.applyLocale(this.getBaseContext());
    }

    /**
     * Checks if the app has permission to write to device storage
     *
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
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            activity.requestPermissions(
                    PERMISSIONS_STORAGE,
                    1
            );
        }
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
    }

    protected void onPause() {
        mText.removeTextChangedListener(textWatcher);      // to prevent text
        super.onPause();
    }

    /**
     * @param state Bundle
     */
    private void restoreState(Bundle state) {
        urlFilename = state.getString(STATE_FILENAME);
        changed = state.getBoolean(STATE_CHANGED);
    }

    /**
     * @param outState Bundle
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILENAME, urlFilename);
        outState.putBoolean(STATE_CHANGED, changed);
    }

    protected void onStop() {
        //
        // modification once rotated
        super.onStop();
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

        if (settingsService.isLegacyFilePicker()) {
            return false;
        }
        return true;
    }

    void openLastFile() {
        if (!settingsService.getLastFilename().equals(TPStrings.EMPTY)) {
            if (useAndroidManager()) {
                showToast(formatString(R.string.opened_last_edited_file, settingsService.getLastFilename()));
                Uri uri = Uri.parse(settingsService.getLastFilename());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //dublicated in useAndroidManager
                    this.openNamedFile(uri);
                }
            } else {
                this.openNamedFileLegacy(settingsService.getLastFilename());
            }

        }
    }

    void updateTitle() {
        String title;
        if (urlFilename.equals(TPStrings.EMPTY)) {
            title = TPStrings.NEW_FILE_TXT;
        } else {
            Uri uri = Uri.parse(urlFilename);
            title = getFilenameByUri(uri);
        }
        if (changed) {
            title = title + TPStrings.STAR;
        }
        this.setTitle(title);
    }

    String getFilenameByUri(Uri uri) {
        String path = uri.getPath();
        String[] paths = path.split("/");
        if (paths.length == 0) {
            return "";
        }
        return paths[paths.length -1];
    }

    void applyPreferences() {

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

        int bgcolor = settingsService.getBgColor();
        scrollView.setBackgroundColor(bgcolor);

        int fontcolor = settingsService.getFontColor();//
        mText.setTextColor(fontcolor);
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Set up search view
        searchItem = menu.findItem(R.id.menu_document_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        // Set up search view options and listener
        if (searchView != null) {
               searchView.setSubmitButtonEnabled(true);
               searchView.setIconified(false);
               searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
               searchView.setOnQueryTextListener(new QueryTextListener());
        }
        return true;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_document_open) {
            openFile();
        } else if (itemId == R.id.menu_document_new) {
            newFile();
        } else if (itemId == R.id.menu_document_save) {
            saveFile();
        } else if (itemId == R.id.menu_document_save_as) {
            saveAs();
        } else if (itemId == R.id.menu_document_settings) {
            Intent intent = new Intent(this.getBaseContext(),
                    SettingsActivity.class);
            this.startActivityForResult(intent, REQUEST_SETTINGS);
        } else if (itemId == R.id.menu_exit) {
            exitApplication();
        }

        // Close text search
        if (searchItem != null && searchItem.isActionViewExpanded())
            searchItem.collapseActionView();

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
                                    next_action = DO_NEW;
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
        urlFilename = TPStrings.EMPTY;
        changed = false;
        this.updateTitle();
    }

    protected void saveAs() {
        if (useAndroidManager()) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_TITLE, TPStrings.NEW_FILE_TXT);
            startActivityForResult(intent, ACTION_OPTION_FILE);
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
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    // Stop the activity
                                    next_action = DO_OPEN;
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

    protected void exitApplication() {
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
                                    next_action = DO_EXIT;
                                    EditorActivity.this.saveFile();
                                }

                            })
                    .setNegativeButton(R.string.No,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    System.exitFromApp(EditorActivity.this);
                                }
                            }).show();
        } else {
            System.exitFromApp(EditorActivity.this);
        }
    }

    protected void openNewFile() {
        if (useAndroidManager()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_TITLE, TPStrings.NEW_FILE_TXT);
            startActivityForResult(intent, ACTION_SAVE_FILE);
        } else {
            Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
            intent.putExtra(TPStrings.SELECTION_MODE, SelectionMode.MODE_OPEN);
            this.startActivityForResult(intent, REQUEST_OPEN);
        }
    }

    protected void saveFile() {
        if (urlFilename.equals(TPStrings.EMPTY)) {
            saveAs();
        } else {
            if (useAndroidManager()) {
                saveNamedFile();
            } else {
                saveNamedFileLegacy();
            }
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
                                    next_action = DO_OPEN;
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

            if (useAndroidManager()) {
                saveNamedFile();
            } else {
                saveNamedFileLegacy();
            }
        }
    }

    protected boolean fileAlreadyExists() {
        File f = new File(urlFilename);
        return f.exists();
    }

    protected void saveNamedFileLegacy() {
        try {
            File f = new File(urlFilename);
            if (!f.exists()) {
                f.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(f);
            String s = this.mText.getText().toString();

            s = applyEndings(s);

            fos.write(s.getBytes(settingsService.getFileEncoding()));
            fos.close();
            showToast(R.string.File_Written);
            changed = false;
            updateTitle();

            if (next_action == DO_OPEN) {   // because of multithread nature
                // figure out better way to do
                // it
                next_action = DO_NOTHING;
                openNewFile();
            }
            if (next_action == DO_NEW) { // because of multithread nature
                // figure out better way to do
                // it
                next_action = DO_NOTHING;
                clearFile();
            }
            if (next_action == DO_EXIT) {
                exitApplication();
            }
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
            this.showToast(R.string.Can_not_write_file);
        }
    }

    protected void saveFile(Uri uri) throws FileNotFoundException, IOException {
        ContentResolver contentResolver = getContentResolver();
        OutputStream fos = contentResolver.openOutputStream(uri);
        String s = this.mText.getText().toString();

        s = applyEndings(s);

        fos.write(s.getBytes(settingsService.getFileEncoding()));
        fos.close();
    }

    protected void saveNamedFile() {
        try {
            Uri uri = Uri.parse(urlFilename);
            saveFile(uri);

            showToast(R.string.File_Written);
            changed = false;
            updateTitle();

            if (next_action == DO_OPEN) {   // because of multithread nature
                next_action = DO_NOTHING;
                openNewFile();
            }
            if (next_action == DO_NEW) { // because of multithread nature
                next_action = DO_NOTHING;
                clearFile();
            }
            if (next_action == DO_EXIT) {
                exitApplication();
            }
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
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

            this.mText.setText(ttt);
            showToast(getBaseContext().getResources().getString(R.string.File_opened_, filename));
            changed = false;
            this.urlFilename = filename;
            if (!settingsService.getLastFilename().equals(filename)) {
                settingsService.setLastFilename(filename, this.getApplicationContext());
            }
            selectionStart = 0;
            updateTitle();
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
            this.showToast(R.string.Can_not_read_file);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void openNamedFile(Uri uri) {
        try {
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            int size = inputStream.available();
            DataInputStream dis = new DataInputStream(inputStream);
            byte[] b = new byte[(int) size];
            int length = dis.read(b, 0, (int) size);

            String ttt = new String(b, 0, length, settingsService.getFileEncoding());
            ttt = toUnixEndings(ttt);

            inputStream.close();
            dis.close();

            this.mText.setText(ttt);

            showToast(getBaseContext().getResources().getString(R.string.File_opened_, urlFilename));
            changed = false;
            this.urlFilename = uri.toString();
            if (!settingsService.getLastFilename().equals(urlFilename)) {
                settingsService.setLastFilename(urlFilename, this.getApplicationContext());
            }
            selectionStart = 0;
            updateTitle();
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
            this.showToast(R.string.Can_not_read_file);
        }
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

        //we should anyway fix any line delimenters
        //replace \r\n first, then \r into \n this way we will get pure unix ending used in android
        return TextConverter.getInstance().applyEndings(value, TextConverter.UNIX);
    }

    /**
     *
     */
    public synchronized void onActivityResult(
        final int requestCode,
        int resultCode,
        final Intent data
    ) {

        if (requestCode == REQUEST_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                urlFilename = data
                        .getStringExtra(TPStrings.RESULT_PATH);
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
        } else if (requestCode == ACTION_SAVE_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri;
            if (data != null) {
                uri = data.getData();
                //String path = uri.getPath();

                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                // Check for the freshest data.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    openNamedFile(uri);
                }
            }
        } else if (requestCode == ACTION_OPTION_FILE) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                urlFilename = uri.toString();
                this.saveFileWithConfirmation();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
    private class QueryTextListener implements SearchView.OnQueryTextListener
    {
        final private BackgroundColorSpan span = new BackgroundColorSpan(Color.YELLOW);
        private Editable editable;
        private Matcher matcher;
        private int index;
        private int height;

        // onQueryTextChange
        @Override
        public boolean onQueryTextChange(String newText)
        {
            // Use regex search and spannable for highlighting
            height = scrollView.getHeight();
            editable = mText.getEditableText();

            // Reset the index and clear highlighting
            if (newText.length() == 0) {
                index = 0;
                editable.removeSpan(span);
                return false;
            }

            // Check pattern
            try {
                Pattern pattern = Pattern.compile(newText, Pattern.MULTILINE);
                matcher = pattern.matcher(editable);
            } catch (Exception e) {
                return false;
            }

            // Find text
            if (matcher.find(index)) {
                // Get index
                index = matcher.start();

                // Check layout
                if (mText.getLayout() == null)
                    return false;

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
            if (matcher.find()) {
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
            } else {
                Toast.makeText(
                        EditorActivity.this,
                        formatString(R.string.s_not_found, query),
                        Toast.LENGTH_SHORT
                ).show();
                matcher.reset();
                index = 0;
            }

            return true;
        }
    }
}