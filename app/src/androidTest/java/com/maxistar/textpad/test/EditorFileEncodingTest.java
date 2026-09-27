package com.maxistar.textpad.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertArrayEquals;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.service.SettingsService;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Opens documents written in different encodings and checks the editor shows
 * the correct text and saves the file back in its original encoding with the
 * original byte order mark.
 */
@RunWith(AndroidJUnit4.class)
public class EditorFileEncodingTest {

    private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF_16LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF_16BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF_32LE_BOM = {(byte) 0xFF, (byte) 0xFE, 0, 0};

    /** Cyrillic "Hello world" written as unicode escapes to stay encoding independent. */
    private static final String SAMPLE = "\u041f\u0440\u0438\u0432\u0435\u0442 \u043c\u0438\u0440";

    private Context context;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        clearRecovery();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(SettingsService.SETTING_FILE_ENCODING, "UTF-8")
                .putBoolean(SettingsService.SETTING_OPEN_LAST_FILE, false)
                .commit();
        ServiceLocator.getInstance().getSettingsService(context).reloadSettings(context);
    }

    @After
    public void tearDown() {
        clearRecovery();
    }

    @Test
    public void utf8BomFileOpensWithoutBomCharAndSavesWithBom() throws Exception {
        assertOpenEditSaveRoundTrip(SAMPLE, "UTF-8", UTF_8_BOM);
    }

    @Test
    public void utf16LeBomFileOpensAndRoundTrips() throws Exception {
        assertOpenEditSaveRoundTrip(SAMPLE, "UTF-16LE", UTF_16LE_BOM);
    }

    @Test
    public void utf16BeBomFileOpensAndRoundTrips() throws Exception {
        assertOpenEditSaveRoundTrip(SAMPLE, "UTF-16BE", UTF_16BE_BOM);
    }

    @Test
    public void utf32LeBomFileOpensAndRoundTrips() throws Exception {
        Assume.assumeTrue(Charset.isSupported("UTF-32LE"));
        assertOpenEditSaveRoundTrip(SAMPLE, "UTF-32LE", UTF_32LE_BOM);
    }

    @Test
    public void fileWithoutBomUsesConfiguredDefaultEncoding() throws Exception {
        assertOpenEditSaveRoundTrip(SAMPLE, "UTF-8", new byte[0]);
    }

    /**
     * Creates a raw document, opens it in the editor, checks the decoded text
     * (no stray BOM character), appends a character and saves, then checks the
     * bytes on disk kept their encoding and byte order mark.
     */
    private void assertOpenEditSaveRoundTrip(String content, String charsetName, byte[] expectedBom)
            throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Charset charset = Charset.forName(charsetName);
        Uri documentUri = createTestDocumentRaw(concat(expectedBom, content.getBytes(charset)));
        try {
            Intent intent = new Intent(context, EditorActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(documentUri);

            try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(intent)) {
                onView(withId(R.id.editText1)).check(matches(withText(content)));

                scenario.onActivity(activity -> {
                    EditText editor = activity.findViewById(R.id.editText1);
                    editor.setSelection(editor.getText().length());
                    editor.getText().append("!");
                });
                scenario.onActivity(activity -> invokeNoArgument(activity, "saveNamedFile"));
            }

            byte[] savedBytes = readDocumentRawBytes(documentUri);
            assertArrayEquals(concat(expectedBom, (content + "!").getBytes(charset)), savedBytes);
        } finally {
            context.getContentResolver().delete(documentUri, null, null);
        }
    }

    private static byte[] concat(byte[] prefix, byte[] suffix) {
        byte[] result = new byte[prefix.length + suffix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
        return result;
    }

    private Uri createTestDocumentRaw(byte[] rawContent) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "textpad-encoding-" + java.lang.System.nanoTime() + ".txt");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/TextPadTests");
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IllegalStateException("Unable to create test document");
        }
        try (java.io.OutputStream output = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IllegalStateException("Unable to write test document");
            }
            output.write(rawContent);
        }
        return uri;
    }

    private byte[] readDocumentRawBytes(Uri uri) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) {
                throw new IllegalStateException("Unable to read test document");
            }
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static void invokeNoArgument(EditorActivity activity, String methodName) {
        try {
            Method method = EditorActivity.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private void clearRecovery() {
        context.getSharedPreferences("editor_recovery", Context.MODE_PRIVATE).edit().clear().commit();
        deleteRecursively(new RecoveryRepository(context).getDirectoryForTests());
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
