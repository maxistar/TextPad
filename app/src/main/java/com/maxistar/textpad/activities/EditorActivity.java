package com.maxistar.textpad.activities;

import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
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
import android.graphics.Insets;
import android.graphics.Rect;
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
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.maxistar.textpad.recovery.RecoveryDraft;
import com.maxistar.textpad.recovery.RecoveryKeys;
import com.maxistar.textpad.recovery.RecoveryMetadata;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.recovery.RecoveryWriter;
import com.maxistar.textpad.utils.EditTextUndoRedo;
import com.maxistar.textpad.utils.DocumentSaveValidator;
import com.maxistar.textpad.utils.FileEncoding;
import com.maxistar.textpad.utils.FileNameHelper;
import com.maxistar.textpad.utils.System;
import com.maxistar.textpad.utils.TextConverter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;


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
    private static final String STATE_CURSOR_END = "cursor-end";
    private static final String STATE_RECOVERY_KEY = "recovery-key";

    private static final int REQUEST_OPEN = 1;
    private static final int REQUEST_SAVE = 2;

    private static final int REQUEST_SETTINGS = 3;


    private static final int ACTION_CREATE_FILE = 4;
    private static final int ACTION_OPEN_FILE = 5;


    private static final int DO_NOTHING = 0;
    private static final int DO_OPEN = 1;
    private static final int DO_NEW = 2;
    private static final int DO_EXIT = 3;
    private static final int DO_OPEN_RECENT = 4;
    private static final int DO_SHOW_SETTINGS = 5;

    private static final String LOG_TAG = "TextEditor";


    String[] mimeTypes = {
            "*/*",
            "text/*",
            "plain/*",
            "text/javascript",
            "application/ecmascript",
            "application/javascript"
    };

    private EditText mText;
    private ScrollView scrollView;
    private LinearLayout linearLayout;

    private FileEncoding documentEncoding;

    String urlFilename = TPStrings.EMPTY;

    Uri lastTriedSystemUri = null;


    boolean changed = false;

    boolean exitDialogShown = false;

    private int next_action = DO_NOTHING; // to figure out better way

    private String next_action_filename = "";

    static int selectionStart = 0;
    private int selectionEnd = 0;

    private RecoveryRepository recoveryRepository;
    private RecoveryWriter recoveryWriter;
    private String recoveryKey;
    private long editorGeneration = 0;
    private boolean suppressRecoveryTracking = false;
    private boolean recoveryDecisionPending = false;
    private boolean recoveryDialogShowing = false;
    private RecoveryDraft pendingRecoveryDraft;
    private Runnable pendingRecoveryDiscardLoader;
    private Long originalSize;
    private Long originalLastModified;
    private String originalContentSha256;
    private SaveRequest pendingExternalConflict;
    private boolean nextSaveCreatesDocument;
    private boolean hasEnteredForeground;

    private static final class SaveRequest {
        final long generation;
        final String recoveryKey;
        final byte[] bytes;

        SaveRequest(long generation, String recoveryKey, byte[] bytes) {
            this.generation = generation;
            this.recoveryKey = recoveryKey;
            this.bytes = bytes;
        }
    }

    SettingsService settingsService;

    RecentFilesService recentFilesService;

    AlternativeUrlsService alternativeUrlsService;

    private QueryTextListener queryTextListener;

    private TextWatcher textWatcher;

    EditTextUndoRedo editTextUndoRedo;

    WebView mWebView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsService = ServiceLocator.getInstance().getSettingsService(this.getApplicationContext());
        recentFilesService = ServiceLocator.getInstance().getRecentFilesService();
        alternativeUrlsService = ServiceLocator.getInstance().getAlternativeUrlsService();
        recoveryRepository = new RecoveryRepository(getApplicationContext());
        recoveryRepository.cleanupIncompleteArtifacts();
        recoveryWriter = new RecoveryWriter(recoveryRepository);

        if (simpleScrolling()) {
            setContentView(R.layout.main_simple_scrolling);
        } else {
            setContentView(R.layout.main);
        }
        Toolbar toolbar = findViewById(R.id.editor_toolbar);
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            applyEdgeToEdgeInsets();
        }
        mText = this.findViewById(R.id.editText1);
        mText.setBackgroundResource(android.R.color.transparent);
        mText.setOnTouchListener(new TwoFingerPanTouchListener());
        if (!settingsService.isAutoWrapping()) {
            disableEditorAutowrapping();
        }
        editTextUndoRedo = new EditTextUndoRedo(mText, this);
        setTextWatcher();

        if (simpleScrolling()) {
            linearLayout = findViewById(R.id.linear_layout);
        } else {
            scrollView = findViewById(R.id.vscroll);
        }
        applyPreferences();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
            restoreEditorContent();
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
                    } else {
                        offerActiveUntitledRecovery();
                    }
                }
            }
        }

        updateTitle();
        mText.requestFocus();

        settingsService.applyLocale(this.getBaseContext());
    }

    private boolean simpleScrolling() {
        return settingsService.isUseSimpleScrolling();
    }

    private String resolveFileEncodingName() {
        if (documentEncoding != null) {
            return documentEncoding.getCharsetName();
        }
        return settingsService.getFileEncoding();
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private void applyEdgeToEdgeInsets() {
        View editorRoot = findViewById(R.id.editor_root);
        int initialPaddingLeft = editorRoot.getPaddingLeft();
        int initialPaddingTop = editorRoot.getPaddingTop();
        int initialPaddingRight = editorRoot.getPaddingRight();
        int initialPaddingBottom = editorRoot.getPaddingBottom();
        editorRoot.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
            );
            Insets ime = windowInsets.getInsets(WindowInsets.Type.ime());
            view.setPadding(
                    initialPaddingLeft + bars.left,
                    initialPaddingTop + bars.top,
                    initialPaddingRight + bars.right,
                    initialPaddingBottom + Math.max(bars.bottom, ime.bottom)
            );
            view.post(() -> requestFocusedCaretOnScreen(view));
            return windowInsets;
        });
        editorRoot.requestApplyInsets();
    }

    private void requestFocusedCaretOnScreen(View editorRoot) {
        EditText editor = editorRoot.findViewById(R.id.editText1);
        if (editor == null || !editor.isFocused() || editor.getLayout() == null) {
            return;
        }
        int selection = editor.getSelectionStart();
        if (selection < 0) {
            return;
        }
        int line = editor.getLayout().getLineForOffset(selection);
        Rect caret = new Rect(
                editor.getTotalPaddingLeft(),
                editor.getTotalPaddingTop() + editor.getLayout().getLineTop(line),
                Math.max(editor.getTotalPaddingLeft() + 1,
                        editor.getWidth() - editor.getTotalPaddingRight()),
                editor.getTotalPaddingTop() + editor.getLayout().getLineBottom(line)
        );
        editor.requestRectangleOnScreen(caret, false);
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
            public void afterTextChanged(Editable s) {
                if (!suppressRecoveryTracking && changed) {
                    scheduleRecoverySnapshot();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressRecoveryTracking) {
                    return;
                }
                editorGeneration++;
                if (!changed) {
                    changed = true;
                    updateTitle();
                }
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
            } else if (keyCode == KeyEvent.KEYCODE_Z) {
                editUndo();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_Y) {
                editRedo();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    protected void onResume() {
        super.onResume();

        if (hasEnteredForeground && pendingExternalConflict == null) {
            validateOpenDocumentOnForeground();
        }
        hasEnteredForeground = true;

        mText.addTextChangedListener(textWatcher);
        applyStoredSelection();

        if (pendingExternalConflict != null) {
            mText.post(() -> {
                if (pendingExternalConflict != null && !isFinishing()) {
                    showExternalChangeDialog(pendingExternalConflict);
                }
            });
        }

        if (recoveryDecisionPending && !recoveryDialogShowing && pendingRecoveryDraft != null) {
            mText.post(this::showPendingRecoveryDialog);
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
            this.saveFileIfNamed(true);
        }

        mText.removeTextChangedListener(textWatcher);
        selectionStart = mText.getSelectionStart();
        selectionEnd = mText.getSelectionEnd();
        if (changed) {
            flushRecoverySnapshot();
        }
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
        selectionEnd = state.getInt(STATE_CURSOR_END, selectionStart);
        recoveryKey = state.getString(STATE_RECOVERY_KEY);
    }

    private void restoreEditorContent() {
        if (changed && recoveryKey != null) {
            RecoveryDraft draft = recoveryRepository.load(recoveryKey, documentIdentityUri());
            if (draft != null) {
                showRecoveryDialog(draft, this::loadStoredDocumentAfterDiscard);
                return;
            }
        }
        loadStoredDocumentAfterDiscard();
    }

    private void loadStoredDocumentAfterDiscard() {
        if (isFilenameEmpty()) {
            setEditorText(TPStrings.EMPTY, false);
            return;
        }
        if (useAndroidManager()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                openNamedFileDirect(Uri.parse(urlFilename));
            }
        } else {
            openNamedFileLegacyDirect(urlFilename);
        }
    }

    private void offerActiveUntitledRecovery() {
        RecoveryDraft draft = recoveryRepository.loadActive();
        if (draft != null && draft.metadata.untitled) {
            recoveryKey = draft.metadata.recoveryKey;
            showRecoveryDialog(draft, () -> setEditorText(TPStrings.EMPTY, false));
        }
    }

    private void showRecoveryDialog(RecoveryDraft draft, Runnable discardLoader) {
        recoveryDecisionPending = true;
        pendingRecoveryDraft = draft;
        pendingRecoveryDiscardLoader = discardLoader;
        recoveryDialogShowing = true;
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(draft.metadata.draftUpdatedAt));
        String name = draft.metadata.displayName == null || draft.metadata.displayName.isEmpty()
                ? TPStrings.NEW_FILE_TXT
                : draft.metadata.displayName;
        new AlertDialog.Builder(this)
                .setTitle(R.string.Recovery_draft_found)
                .setMessage(getString(R.string.Recovery_draft_message, name, timestamp))
                .setPositiveButton(R.string.Restore, (dialog, which) -> restoreDraft(draft))
                .setNegativeButton(R.string.Discard_draft, (dialog, which) -> {
                    clearPendingRecoveryDecision();
                    recoveryRepository.delete(draft.metadata.recoveryKey);
                    if (draft.metadata.recoveryKey.equals(recoveryKey)) {
                        recoveryKey = null;
                    }
                    discardLoader.run();
                })
                .setCancelable(false)
                .setOnDismissListener(dialog -> recoveryDialogShowing = false)
                .show();
    }

    private void showPendingRecoveryDialog() {
        if (recoveryDecisionPending && !recoveryDialogShowing && pendingRecoveryDraft != null) {
            showRecoveryDialog(pendingRecoveryDraft, pendingRecoveryDiscardLoader);
        }
    }

    private void restoreDraft(RecoveryDraft draft) {
        clearPendingRecoveryDecision();
        recoveryKey = draft.metadata.recoveryKey;
        urlFilename = draft.metadata.documentUri == null ? TPStrings.EMPTY : filenameFromIdentity(draft.metadata.documentUri);
        editorGeneration = draft.metadata.generation;
        selectionStart = draft.metadata.cursorStart;
        selectionEnd = draft.metadata.cursorEnd;
        originalSize = draft.metadata.originalSize;
        originalLastModified = draft.metadata.originalLastModified;
        originalContentSha256 = draft.metadata.originalContentSha256;
        if (draft.metadata.encoding != null && !draft.metadata.encoding.isEmpty()) {
            documentEncoding = FileEncoding.fromCharset(draft.metadata.encoding, draft.metadata.hasBom);
        }
        setEditorText(draft.text, true);
        updateTitle();
        if (!draft.metadata.untitled) {
            mText.post(this::validateRestoredDraft);
        }
    }

    private void clearPendingRecoveryDecision() {
        recoveryDecisionPending = false;
        pendingRecoveryDraft = null;
        pendingRecoveryDiscardLoader = null;
    }

    private void applyStoredSelection() {
        int length = mText.length();
        int start = Math.max(0, Math.min(selectionStart, length));
        int end = Math.max(0, Math.min(selectionEnd, length));
        mText.setSelection(Math.min(start, end), Math.max(start, end));
    }

    private void setEditorText(String text, boolean markChanged) {
        suppressRecoveryTracking = true;
        mText.setText(text);
        editTextUndoRedo.clearHistory();
        suppressRecoveryTracking = false;
        changed = markChanged;
        applyStoredSelection();
    }

    private void scheduleRecoverySnapshot() {
        RecoveryWriter.Snapshot snapshot = createRecoverySnapshot();
        if (snapshot != null) {
            recoveryWriter.schedule(snapshot);
        }
    }

    private void flushRecoverySnapshot() {
        RecoveryWriter.Snapshot snapshot = createRecoverySnapshot();
        if (snapshot != null) {
            recoveryWriter.flushAndWait(snapshot, 2000);
        }
    }

    private RecoveryWriter.Snapshot createRecoverySnapshot() {
        if (!changed || recoveryDecisionPending) {
            return null;
        }
        String identity = documentIdentityUri();
        if (recoveryKey == null) {
            recoveryKey = identity == null
                    ? RecoveryKeys.forUntitledDocument()
                    : RecoveryKeys.forDocumentUri(identity);
        }
        int start = Math.max(0, mText.getSelectionStart());
        int end = Math.max(0, mText.getSelectionEnd());
        RecoveryMetadata metadata = new RecoveryMetadata(
                recoveryKey,
                identity,
                currentDisplayName(),
                identity == null,
                resolveFileEncodingName(),
                documentEncoding != null && documentEncoding.hasBom(),
                originalSize,
                originalLastModified,
                originalContentSha256,
                0,
                0,
                start,
                end,
                editorGeneration
        );
        return new RecoveryWriter.Snapshot(metadata, mText.getText().toString());
    }

    private String documentIdentityUri() {
        if (isFilenameEmpty()) {
            return null;
        }
        if (useAndroidManager()) {
            return urlFilename;
        }
        return Uri.fromFile(new File(urlFilename)).toString();
    }

    private String filenameFromIdentity(String identity) {
        if (useAndroidManager()) {
            return identity;
        }
        String path = Uri.parse(identity).getPath();
        return path == null ? TPStrings.EMPTY : path;
    }

    private String currentDisplayName() {
        if (isFilenameEmpty()) {
            return TPStrings.NEW_FILE_TXT;
        }
        if (useAndroidManager()) {
            return FileNameHelper.getFilenameByUri(this, Uri.parse(urlFilename));
        }
        return new File(urlFilename).getName();
    }

    private void recordLoadedDocument(byte[] originalBytes, Long lastModified) {
        originalSize = (long) originalBytes.length;
        originalLastModified = lastModified;
        originalContentSha256 = DocumentSaveValidator.sha256(originalBytes);
        String identity = documentIdentityUri();
        recoveryKey = identity == null ? null : RecoveryKeys.forDocumentUri(identity);
        editorGeneration = 0;
        selectionStart = 0;
        selectionEnd = 0;
    }

    /**
     * @param outState Bundle
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (changed) {
            flushRecoverySnapshot();
        }
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILENAME, urlFilename);
        outState.putBoolean(STATE_CHANGED, changed);
        outState.putInt(STATE_CURSOR_POSITION, mText.getSelectionStart());
        outState.putInt(STATE_CURSOR_END, mText.getSelectionEnd());
        outState.putString(STATE_RECOVERY_KEY, recoveryKey);
    }

    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        recoveryWriter.shutdown();
        editTextUndoRedo.disconnect();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (this.changed && !exitDialogShown) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.You_have_made_some_changes)
                    .setMessage(R.string.Are_you_sure_to_quit)
                    .setNegativeButton(R.string.Yes, (arg0, arg1) -> {
                        discardCurrentRecovery();
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
        } else {
            offerActiveUntitledRecovery();
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

    private void disableEditorAutowrapping() {
        mText.setHorizontallyScrolling(true);
        mText.setHorizontalScrollBarEnabled(true);
        mText.setMaxLines(Integer.MAX_VALUE);
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
            case (SettingsService.SETTING_MEDIUM_SMALL):
                mText.setTextSize(18.0f);
                break;
            case (SettingsService.SETTING_LARGE):
                mText.setTextSize(24.0f);
                break;
            case (SettingsService.SETTING_HUGE):
                mText.setTextSize(28.0f);
                break;
            case (SettingsService.SETTING_EXTRA_HUGE):
                mText.setTextSize(56.0f);
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
            if (simpleScrolling()) {
                linearLayout.setBackgroundColor(settingsService.getBgColor());
            } else {
                scrollView.setBackgroundColor(settingsService.getBgColor());
            }
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

        MenuItem searchItem = menu.findItem(R.id.menu_document_search);
        if (searchItem.isActionViewExpanded()) {
            searchItem.collapseActionView();
        }


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

        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
        } else if (itemId == R.id.menu_document_go_to) {
            moveCaretPosition();
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
                            (dialog, which) -> {
                                discardCurrentRecovery();
                                openFileByName(filename);
                            }).show();
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
        discardCurrentRecovery();
        editorGeneration = 0;
        originalSize = null;
        originalLastModified = null;
        originalContentSha256 = null;
        documentEncoding = null;
        selectionStart = 0;
        selectionEnd = 0;
        setEditorText(TPStrings.EMPTY, false);
        setFilename(TPStrings.EMPTY);
        initEditor();
        updateTitle();
    }

    private void discardCurrentRecovery() {
        recoveryWriter.cancelAndWait(recoveryKey, editorGeneration, 2000);
        recoveryRepository.delete(recoveryKey);
        recoveryKey = null;
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
                            (dialog, which) -> {
                                discardCurrentRecovery();
                                openNewFile();
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
                            (dialog, which) -> {
                                // Stop the activity
                                next_action = DO_EXIT;
                                EditorActivity.this.saveFile();
                            })
                    .setNegativeButton(R.string.No,
                            (dialog, which) -> {
                                discardCurrentRecovery();
                                System.exitFromApp(EditorActivity.this);
                            }).show();
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
        saveFileIfNamed(false);
    }

    private void saveFileIfNamed(boolean autosave) {
        guardedSaveNamedFile(autosave);
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
                                nextSaveCreatesDocument = false;
                            })
                    .setOnCancelListener(dialog -> nextSaveCreatesDocument = false)
                    .show();
        } else {
            saveFileIfNamed();
        }
    }

    protected boolean fileAlreadyExists() {
        File f = new File(getFilename());
        return f.exists();
    }

    protected void saveNamedFileLegacy() {
        guardedSaveNamedFile(false);
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

            outputStream.write(FileEncoding.encode(s, documentEncoding, settingsService.getFileEncoding()));
        } finally {
            outputStream.close();
        }
    }

    protected void saveNamedFile() {
        guardedSaveNamedFile(false);
    }

    private void guardedSaveNamedFile(boolean autosave) {
        try {
            String persistedText = applyEndings(mText.getText().toString());
            SaveRequest request = new SaveRequest(
                    editorGeneration,
                    recoveryKey,
                    FileEncoding.encode(persistedText, documentEncoding, settingsService.getFileEncoding())
            );
            boolean creatingDocument = nextSaveCreatesDocument || originalContentSha256 == null;
            nextSaveCreatesDocument = false;
            if (creatingDocument) {
                writeAndComplete(request);
                return;
            }

            byte[] currentBytes = readNamedDocumentBytes();
            DocumentSaveValidator.Outcome outcome = DocumentSaveValidator.classify(
                    currentBytes,
                    originalContentSha256,
                    request.bytes
            );
            if (outcome == DocumentSaveValidator.Outcome.BASELINE_MATCH) {
                writeAndComplete(request);
            } else if (outcome == DocumentSaveValidator.Outcome.INTENDED_CONTENT_MATCH) {
                completeEquivalentSave(request, currentBytes);
            } else if (outcome == DocumentSaveValidator.Outcome.CONFLICT) {
                pendingExternalConflict = request;
                flushRecoverySnapshot();
                if (!autosave) {
                    showExternalChangeDialog(request);
                }
            } else {
                showToast(R.string.Can_not_read_file);
            }
        } catch (FileNotFoundException e) {
            this.showToast(R.string.File_not_found);
        } catch (IOException e) {
            this.showToast(R.string.Can_not_write_file);
        } catch (Exception e) {
            this.showToast(R.string.Can_not_write_file);
        }
    }

    private byte[] readNamedDocumentBytes() throws IOException {
        InputStream inputStream;
        if (useAndroidManager()) {
            inputStream = getContentResolver().openInputStream(Uri.parse(getFilename()));
        } else {
            inputStream = new FileInputStream(new File(getFilename()));
        }
        if (inputStream == null) {
            throw new IOException("Document cannot be opened for validation");
        }
        try (InputStream input = inputStream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private void writeNamedDocumentBytes(byte[] bytes) throws IOException {
        if (useAndroidManager()) {
            OutputStream output = getContentResolver().openOutputStream(Uri.parse(getFilename()), "wt");
            if (output == null) {
                throw new IOException("Document cannot be opened for writing");
            }
            try (OutputStream closeable = output) {
                closeable.write(bytes);
            }
            return;
        }

        File file = new File(getFilename());
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Document cannot be created");
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
        }
    }

    private void writeAndComplete(SaveRequest request) throws IOException {
        writeNamedDocumentBytes(request.bytes);
        Long lastModified = useAndroidManager() ? null : new File(getFilename()).lastModified();
        completeSuccessfulSave(request.generation, request.recoveryKey, request.bytes, lastModified);
        pendingExternalConflict = null;
        finishSuccessfulSave(request.generation);
    }

    private void completeEquivalentSave(SaveRequest request, byte[] currentBytes) {
        Long lastModified = useAndroidManager() ? null : new File(getFilename()).lastModified();
        completeSuccessfulSave(request.generation, request.recoveryKey, currentBytes, lastModified);
        pendingExternalConflict = null;
        finishSuccessfulSave(request.generation);
    }

    private void finishSuccessfulSave(long savedGeneration) {
        showToast(R.string.File_Written);
        if (editorGeneration == savedGeneration) {
            initEditor();
        }
        updateTitle();

        if (next_action == DO_OPEN) {
            next_action = DO_NOTHING;
            openNewFile();
        } else if (next_action == DO_NEW) {
            next_action = DO_NOTHING;
            clearFile();
        } else if (next_action == DO_SHOW_SETTINGS) {
            next_action = DO_NOTHING;
            showSettingsActivity();
        } else if (next_action == DO_OPEN_RECENT) {
            next_action = DO_NOTHING;
            openFileByName(next_action_filename);
        } else if (next_action == DO_EXIT) {
            next_action = DO_NOTHING;
            exitApplication();
        }
    }

    private void showExternalChangeDialog(SaveRequest request) {
        if (isFinishing() || request != pendingExternalConflict) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.External_change_detected)
                .setMessage(getString(R.string.External_change_message, currentDisplayName()))
                .setPositiveButton(R.string.Overwrite, (dialog, which) -> overwriteAfterConflict(request))
                .setNegativeButton(R.string.Reload, (dialog, which) -> confirmReloadExternal(request))
                .setNeutralButton(R.string.Save_As, (dialog, which) -> saveAs())
                .setOnCancelListener(dialog -> {
                    pendingExternalConflict = request;
                    flushRecoverySnapshot();
                })
                .show();
    }

    private void overwriteAfterConflict(SaveRequest request) {
        try {
            // Confirm that the target is still readable immediately before the authorized overwrite.
            readNamedDocumentBytes();
            writeAndComplete(request);
        } catch (FileNotFoundException error) {
            showToast(R.string.File_not_found);
        } catch (IOException error) {
            showToast(R.string.Can_not_write_file);
        }
    }

    private void confirmReloadExternal(SaveRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.Reload_external_title)
                .setMessage(R.string.Reload_external_message)
                .setPositiveButton(R.string.Reload, (dialog, which) -> reloadExternalDocument(request))
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                    pendingExternalConflict = request;
                    flushRecoverySnapshot();
                })
                .setOnCancelListener(dialog -> {
                    pendingExternalConflict = request;
                    flushRecoverySnapshot();
                })
                .show();
    }

    private void reloadExternalDocument(SaveRequest request) {
        try {
            byte[] externalBytes = readNamedDocumentBytes();
            recoveryWriter.cancelAndWait(request.recoveryKey, editorGeneration, 2000);
            recoveryRepository.delete(request.recoveryKey);
            pendingExternalConflict = null;
            applyExternalDocument(externalBytes);
        } catch (FileNotFoundException error) {
            showToast(R.string.File_not_found);
            pendingExternalConflict = request;
        } catch (Exception error) {
            showToast(R.string.Can_not_read_file);
            pendingExternalConflict = request;
        }
    }

    private void validateOpenDocumentOnForeground() {
        if (isFilenameEmpty() || originalContentSha256 == null || pendingExternalConflict != null) {
            return;
        }
        try {
            byte[] currentBytes = readNamedDocumentBytes();
            if (originalContentSha256.equals(DocumentSaveValidator.sha256(currentBytes))) {
                return;
            }
            if (!changed) {
                applyExternalDocument(currentBytes);
                showToast(R.string.File_reloaded_after_external_change);
                return;
            }

            byte[] intendedBytes = FileEncoding.encode(
                    applyEndings(mText.getText().toString()),
                    documentEncoding,
                    settingsService.getFileEncoding()
            );
            SaveRequest request = new SaveRequest(editorGeneration, recoveryKey, intendedBytes);
            DocumentSaveValidator.Outcome outcome = DocumentSaveValidator.classify(
                    currentBytes,
                    originalContentSha256,
                    intendedBytes
            );
            if (outcome == DocumentSaveValidator.Outcome.INTENDED_CONTENT_MATCH) {
                completeEquivalentSave(request, currentBytes);
            } else if (outcome == DocumentSaveValidator.Outcome.CONFLICT) {
                pendingExternalConflict = request;
                flushRecoverySnapshot();
                mText.post(() -> showExternalChangeDialog(request));
            }
        } catch (Exception error) {
            // Foreground validation is best effort. Save repeats the mandatory validation.
        }
    }

    private void applyExternalDocument(byte[] externalBytes) throws Exception {
        documentEncoding = FileEncoding.detect(externalBytes);
        String externalText = FileEncoding.decode(externalBytes, documentEncoding, settingsService.getFileEncoding());
        externalText = toUnixEndings(externalText);
        setEditorText(externalText, false);
        initEditor();
        recordLoadedDocument(
                externalBytes,
                useAndroidManager() ? null : new File(getFilename()).lastModified()
        );
        updateTitle();
    }

    private void validateRestoredDraft() {
        if (!changed || isFilenameEmpty() || originalContentSha256 == null) {
            return;
        }
        try {
            byte[] intendedBytes = FileEncoding.encode(
                    applyEndings(mText.getText().toString()),
                    documentEncoding,
                    settingsService.getFileEncoding()
            );
            SaveRequest request = new SaveRequest(editorGeneration, recoveryKey, intendedBytes);
            byte[] currentBytes = readNamedDocumentBytes();
            DocumentSaveValidator.Outcome outcome = DocumentSaveValidator.classify(
                    currentBytes,
                    originalContentSha256,
                    intendedBytes
            );
            if (outcome == DocumentSaveValidator.Outcome.INTENDED_CONTENT_MATCH) {
                completeEquivalentSave(request, currentBytes);
            } else if (outcome == DocumentSaveValidator.Outcome.CONFLICT) {
                pendingExternalConflict = request;
                showExternalChangeDialog(request);
            }
        } catch (Exception error) {
            // Keep the restored local draft; a later Save will retry validation and report failure.
        }
    }

    private void completeSuccessfulSave(
            long savedGeneration,
            String previousRecoveryKey,
            byte[] persistedBytes,
            Long lastModified
    ) {
        String identity = documentIdentityUri();
        String targetKey = identity == null ? null : RecoveryKeys.forDocumentUri(identity);
        originalSize = (long) persistedBytes.length;
        originalLastModified = lastModified;
        originalContentSha256 = DocumentSaveValidator.sha256(persistedBytes);

        if (editorGeneration == savedGeneration) {
            recoveryWriter.cancelAndWait(previousRecoveryKey, savedGeneration, 2000);
            recoveryRepository.delete(previousRecoveryKey);
            if (targetKey != null && !targetKey.equals(previousRecoveryKey)) {
                recoveryRepository.delete(targetKey);
            }
            recoveryKey = targetKey;
            return;
        }

        if (previousRecoveryKey != null && identity != null && !targetKey.equals(previousRecoveryKey)) {
            try {
                recoveryRepository.migrate(previousRecoveryKey, identity, currentDisplayName());
            } catch (Exception ignored) {
                // The previous valid draft remains available if migration fails.
            }
        }
        recoveryKey = targetKey;
        scheduleRecoverySnapshot();
    }

    protected void openNamedFileLegacy(String filename) {
        String identity = Uri.fromFile(new File(filename)).toString();
        RecoveryDraft draft = recoveryRepository.load(RecoveryKeys.forDocumentUri(identity), identity);
        if (draft != null) {
            showRecoveryDialog(draft, () -> openNamedFileLegacyDirect(filename));
        } else {
            openNamedFileLegacyDirect(filename);
        }
    }

    private void openNamedFileLegacyDirect(String filename) {
        try {
            File f = new File(filename);
            FileInputStream fis = new FileInputStream(f);

            long size = f.length();
            DataInputStream dis = new DataInputStream(fis);
            byte[] b = new byte[(int) size];
            int length = dis.read(b, 0, (int) size);

            dis.close();
            fis.close();

            documentEncoding = FileEncoding.detect(b);
            String ttt = FileEncoding.decode(b, documentEncoding, settingsService.getFileEncoding());
            ttt = toUnixEndings(ttt);

            setEditorText(ttt, false);

            showToast(getBaseContext().getResources().getString(R.string.File_opened_, filename));
            initEditor();
            this.setFilename(filename);
            recordLoadedDocument(b, f.lastModified());
            if (!settingsService.getLastFilename().equals(filename)) {
                settingsService.setLastFilename(filename, this.getApplicationContext());
            }
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
        String identity = uri.toString();
        RecoveryDraft draft = recoveryRepository.load(RecoveryKeys.forDocumentUri(identity), identity);
        if (draft != null) {
            showRecoveryDialog(draft, () -> openNamedFileDirect(uri));
        } else {
            openNamedFileDirect(uri);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void openNamedFileDirect(final Uri uri) {
        try {
            ContentResolver contentResolver = getContentResolver();

            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                throw new IOException();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(8192, inputStream.available()));
            byte[] buffer = new byte[8192];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            byte[] b = bytes.toByteArray();

            documentEncoding = FileEncoding.detect(b);
            String ttt = FileEncoding.decode(b, documentEncoding, settingsService.getFileEncoding());
            ttt = toUnixEndings(ttt);

            inputStream.close();

            setEditorText(ttt, false);

            showToast(getBaseContext().getResources().getString(R.string.File_opened_, getFilename()));
            initEditor();
            setFilename(uri.toString());
            recordLoadedDocument(b, null);
            if (!settingsService.getLastFilename().equals(getFilename())) {
                settingsService.setLastFilename(getFilename(), this.getApplicationContext());
            }
            if (lastTriedSystemUri != null) {
                alternativeUrlsService.addAlternativeUrl(lastTriedSystemUri, uri, getApplicationContext());
                lastTriedSystemUri = null;
            }
            updateTitle();
            detectReadOnlyAccess(uri);
        } catch (FileNotFoundException e) {
            if (isAccessDeniedException(e)) {
                showAlternativeFileDialog(uri);
            } else {
                this.showToast(R.string.File_not_found);
            }
        } catch (Exception e) {
            this.showToast(R.string.Can_not_read_file);
        }
    }

    private void detectReadOnlyAccess(final Uri uri) {
        boolean isReadOnly = false;

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
            new Handler().postDelayed(this::showReadOnlyDialog, 1000);
        }

    }

    public void showReadOnlyDialog() {
        Context context = this;
        // Message text with a clickable link
        SpannableString spannableMessage = new SpannableString(getString(R.string.readOnlyDialogMessage) + ' ' + getString(R.string.readOnlyDialogClickHere));

        // Set the clickable part
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                // Open the link in an external browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://simpleditor.org/faq/"));
                context.startActivity(browserIntent);
            }
        };
        // would it work for reverse languages?
        spannableMessage.setSpan(clickableSpan, spannableMessage.length() - getString(R.string.readOnlyDialogClickHere).length(), spannableMessage.length(), 0);
        spannableMessage.setSpan(new UnderlineSpan(), spannableMessage.length() - getString(R.string.readOnlyDialogClickHere).length(), spannableMessage.length(), 0);

        // Creating the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.readOnlyDialogTitle);

        // Including the message and link in the dialog
        builder.setMessage(spannableMessage);
        builder.setCancelable(true);

        // Setting the dialog buttons
        builder.setPositiveButton(R.string.readOnlyDialogButtonOpenAgain, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openFile();
            }
        });

        builder.setNegativeButton(R.string.readOnlyDialogButtonContinue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Make the link clickable
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
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
    String applyEndings(String value) {
        String to = settingsService.getDelimiters();
        value = TextConverter.getInstance().applyEndings(value, to);
        return value;
    }

    /**
     * @param value Value
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
                nextSaveCreatesDocument = true;
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
                    nextSaveCreatesDocument = true;
                    this.saveFileWithConfirmation();
                }
            }
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

    // TwoFingerPanTouchListener
    private class TwoFingerPanTouchListener implements View.OnTouchListener {

        private boolean panningActive = false;
        private float lastFocalX;
        private float lastFocalY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    panningActive = false;
                    return false;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == 2) {
                        panningActive = true;
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        lastFocalX = focalX(event);
                        lastFocalY = focalY(event);
                    }
                    return panningActive;
                case MotionEvent.ACTION_MOVE:
                    if (!panningActive) {
                        return false;
                    }
                    float focalX = focalX(event);
                    float focalY = focalY(event);
                    panContent(
                            Math.round(focalX - lastFocalX),
                            Math.round(focalY - lastFocalY)
                    );
                    lastFocalX = focalX;
                    lastFocalY = focalY;
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                    if (panningActive) {
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return panningActive;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean wasPanning = panningActive;
                    panningActive = false;
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    return wasPanning;
                default:
                    return panningActive;
            }
        }

        private float focalX(MotionEvent event) {
            float sum = 0;
            for (int i = 0; i < event.getPointerCount(); i++) {
                sum += event.getX(i);
            }
            return sum / event.getPointerCount();
        }

        private float focalY(MotionEvent event) {
            float sum = 0;
            for (int i = 0; i < event.getPointerCount(); i++) {
                sum += event.getY(i);
            }
            return sum / event.getPointerCount();
        }
    }

    private void panContent(int deltaX, int deltaY) {
        Layout layout = mText.getLayout();
        int maxX = layout == null ? 0 :
                layout.getWidth() + mText.getTotalPaddingLeft() + mText.getTotalPaddingRight()
                        - mText.getWidth();
        int newX = clampScroll(mText.getScrollX() - deltaX, maxX);

        if (simpleScrolling()) {
            int maxY = layout == null ? 0 :
                    layout.getHeight() + mText.getTotalPaddingTop() + mText.getTotalPaddingBottom()
                            - mText.getHeight();
            int newY = clampScroll(mText.getScrollY() - deltaY, maxY);
            mText.scrollTo(newX, newY);
            return;
        }

        if (scrollView != null) {
            scrollView.scrollBy(0, -deltaY);
        }
        mText.scrollTo(newX, mText.getScrollY());
    }

    private int clampScroll(int value, int max) {
        if (value < 0) {
            return 0;
        }
        if (max > 0 && value > max) {
            return max;
        }
        return value;
    }

    // QueryTextListener
    private class QueryTextListener
            implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {
        private final BackgroundColorSpan span = new BackgroundColorSpan(getSearchSelectionColor());
        private final Editable editable;
        private Matcher matcher;
        private int index;
        private final int height;

        public QueryTextListener() {
            // Use regex search and spannable for highlighting
            if (simpleScrolling()) {
                height = linearLayout.getHeight();
            } else {
                height = scrollView.getHeight();
            }
            editable = mText.getEditableText();
        }

        // onQueryTextChange
        @Override
        public boolean onQueryTextChange(String newText) {
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
        public boolean onQueryTextSubmit(String query) {
            // Find next text
            if (matcher != null) {
                if (matcher.find()) {
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
            if (simpleScrolling()) {
                mText.scrollTo(0, pos - height / 2);
            } else {
                scrollView.smoothScrollTo(0, pos - height / 2);
            }
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
            editable.removeSpan(span);
            mText.requestFocus();
            queryTextListener = null;
            return true;
        }
    }

    private void moveCaretPosition() {
        if (mText.length() != 0) {
            new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_menu_directions)
                .setTitle(R.string.Go_To_Title)
                .setMessage(R.string.Go_To_Description)
                .setPositiveButton(R.string.Go_To_End,
                        (dialog, which) -> {
                            mText.setSelection(mText.length());
                        })
                .setNegativeButton(R.string.Go_To_Beginning,
                        (dialog, which) -> {
                            mText.setSelection(0);
                        }).show();
        }
    }
}
