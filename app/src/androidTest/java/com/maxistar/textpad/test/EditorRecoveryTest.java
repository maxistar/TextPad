package com.maxistar.textpad.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.activities.SettingsActivity;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.recovery.RecoveryKeys;
import com.maxistar.textpad.recovery.RecoveryMetadata;
import com.maxistar.textpad.service.SettingsService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class EditorRecoveryTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        clearRecovery();
        setSimpleScrolling(false);
    }

    @After
    public void tearDown() {
        clearRecovery();
        setSimpleScrolling(false);
    }

    @Test
    public void largeDocumentStateIsBinderSafeInStandardLayout() {
        assertLargeDocumentStateIsBinderSafe(false);
    }

    @Test
    public void largeDocumentStateIsBinderSafeInSimpleLayout() {
        assertLargeDocumentStateIsBinderSafe(true);
    }

    @Test
    public void largeDocumentCanOpenSettingsWithoutProcessDeath() {
        String content = generatedDocument(1_050_000);
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.editText1)).setText(content);
                activity.startActivity(new Intent(activity, SettingsActivity.class));
            });
            onView(withText(R.string.Main_Settings)).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (android.app.Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                        .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                    if (activity instanceof SettingsActivity) {
                        activity.finish();
                    }
                }
            });
            onView(withId(R.id.editText1)).check((view, noViewFoundException) -> assertEquals(
                    content.length(),
                    ((EditText) view).length()
            ));
        }
    }

    @Test
    public void changedUntitledDocumentCanBeRestoredAfterRecreation() {
        String content = generatedDocument(220_000);
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                editor.setText(content);
                editor.setSelection(1234);
            });
            android.os.SystemClock.sleep(800);

            scenario.recreate();
            onView(withText(R.string.Restore)).perform(click());
            onView(withId(R.id.editText1)).check(matches(withText(content)));
            scenario.onActivity(activity -> assertEquals(
                    1234,
                    ((EditText) activity.findViewById(R.id.editText1)).getSelectionStart()
            ));
        }
    }

    @Test
    public void coldUntitledDraftCanBeDiscarded() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        new RecoveryRepository(context).write(metadata(key), "recoverable");

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            onView(withText(R.string.Discard_draft)).perform(click());
            onView(withId(R.id.editText1)).check(matches(withText("")));
            assertTrue(new RecoveryRepository(context).load(key, null) == null);
        }
    }

    @Test
    public void dismissingRecoveryKeepsDraft() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        new RecoveryRepository(context).write(metadata(key), "recoverable");

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            pressBack();
            assertTrue(new RecoveryRepository(context).load(key, null) != null);
        }
    }

    @Test
    public void pendingRecoveryDecisionCannotOverwriteDraftDuringRecreation() throws Exception {
        String content = "recoverable after recreation";
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> ((EditText) activity.findViewById(R.id.editText1)).setText(content));
            Thread.sleep(800);
            scenario.recreate();
            onView(withText(R.string.Restore)).inRoot(isDialog())
                    .check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            pressBack();
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals(content, new RecoveryRepository(context).loadActive().text);
            onView(withText(R.string.Restore)).inRoot(isDialog()).perform(click());
            onView(withId(R.id.editText1)).check(matches(withText(content)));
        }
    }

    @Test
    public void namedDraftIsDetectedByExactDocumentUri() throws Exception {
        String documentUri = "content://recovery-test/document/notes.txt";
        String key = RecoveryKeys.forDocumentUri(documentUri);
        new RecoveryRepository(context).write(metadata(key, documentUri), "named recovery");
        Intent intent = new Intent(context, EditorActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(documentUri));

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withText(R.string.Restore)).perform(click());
            onView(withId(R.id.editText1)).check(matches(withText("named recovery")));
        }
    }

    @Test
    public void successfulNamedSaveDeletesMatchingRecovery() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri documentUri = createTestDocument("original");
        String key = RecoveryKeys.forDocumentUri(documentUri.toString());
        new RecoveryRepository(context).write(metadata(key, documentUri.toString()), "recovered and saved");
        Intent intent = new Intent(context, EditorActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(documentUri);

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withText(R.string.Restore)).perform(click());
            scenario.onActivity(activity -> invokeNoArgument(activity, "saveNamedFile"));
            assertTrue(new RecoveryRepository(context).load(key, documentUri.toString()) == null);
            assertEquals("recovered and saved", readDocument(documentUri));
        } finally {
            context.getContentResolver().delete(documentUri, null, null);
        }
    }

    @Test
    public void successfulSaveAsRemovesUntitledRecoveryAndUsesNamedIdentity() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri documentUri = createTestDocument("");
        String untitledKey = RecoveryKeys.forUntitledDocument();
        new RecoveryRepository(context).write(metadata(untitledKey), "saved as named");

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            onView(withText(R.string.Restore)).perform(click());
            scenario.onActivity(activity -> {
                invokeOneStringArgument(activity, "setFilename", documentUri.toString());
                invokeNoArgument(activity, "saveNamedFile");
            });
            assertTrue(new RecoveryRepository(context).load(untitledKey, null) == null);
            assertEquals("saved as named", readDocument(documentUri));
        } finally {
            context.getContentResolver().delete(documentUri, null, null);
        }
    }

    private void assertLargeDocumentStateIsBinderSafe(boolean simpleScrolling) {
        setSimpleScrolling(simpleScrolling);
        String content = generatedDocument(1_050_000);
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> ((EditText) activity.findViewById(R.id.editText1)).setText(content));
            scenario.onActivity(activity -> {
                Bundle state = new Bundle();
                activity.onSaveInstanceState(state);
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeBundle(state);
                    assertTrue("Saved state must remain compact", parcel.dataSize() < 100_000);
                } finally {
                    parcel.recycle();
                }
            });

            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertEquals(
                    content.length(),
                    ((EditText) activity.findViewById(R.id.editText1)).length()
            ));
        }
    }

    private static String generatedDocument(int length) {
        StringBuilder value = new StringBuilder(length);
        while (value.length() < length) {
            value.append("0123456789abcdef\n");
        }
        return value.substring(0, length);
    }

    private RecoveryMetadata metadata(String key) {
        return metadata(key, null);
    }

    private RecoveryMetadata metadata(String key, String documentUri) {
        return new RecoveryMetadata(
                key, documentUri, documentUri == null ? "newfile.txt" : "notes.txt",
                documentUri == null, "UTF-8", false,
                null, null, null, 0, 0, 0, 0, 1
        );
    }

    private Uri createTestDocument(String content) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "textpad-recovery-" + java.lang.System.nanoTime() + ".txt");
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
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return uri;
    }

    private String readDocument(Uri uri) {
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
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) {
            throw new AssertionError(error);
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

    private static void invokeOneStringArgument(EditorActivity activity, String methodName, String value) {
        try {
            Method method = EditorActivity.class.getDeclaredMethod(methodName, String.class);
            method.setAccessible(true);
            method.invoke(activity, value);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private void setSimpleScrolling(boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(SettingsService.SETTING_USE_SIMPLE_SCROLLING, enabled)
                .putBoolean(SettingsService.SETTING_OPEN_LAST_FILE, false)
                .commit();
        ServiceLocator.getInstance().getSettingsService(context).reloadSettings(context);
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
