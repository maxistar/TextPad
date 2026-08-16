package com.maxistar.textpad.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.ContentValues;
import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.recovery.RecoveryKeys;
import com.maxistar.textpad.recovery.RecoveryMetadata;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.service.SettingsService;
import com.maxistar.textpad.utils.DocumentSaveValidator;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class EditorExternalChangeTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        clearRecovery();
        setPreference(SettingsService.SETTING_LEGASY_FILE_PICKER, false);
        setPreference(SettingsService.SETTING_AUTO_SAVE_CURRENT_FILE, false);
        setPreference(SettingsService.SETTING_USE_SIMPLE_SCROLLING, false);
    }

    @After
    public void tearDown() {
        clearRecovery();
        setPreference(SettingsService.SETTING_LEGASY_FILE_PICKER, false);
        setPreference(SettingsService.SETTING_AUTO_SAVE_CURRENT_FILE, false);
        setPreference(SettingsService.SETTING_USE_SIMPLE_SCROLLING, false);
    }

    @Test
    public void explicitSaveConflictCanBeCancelledWithoutLosingEitherVersion() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "local edit");
            writeDocument(uri, "external edit");

            invokeSave(scenario);
            onView(withText(R.string.External_change_detected)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            pressBack();

            assertEquals("external edit", readDocument(uri));
            assertEquals("local edit", editorText(scenario));
            assertNotNull(new RecoveryRepository(context).load(
                    RecoveryKeys.forDocumentUri(uri.toString()), uri.toString()));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void overwriteConflictWritesLocalVersionInSimpleLayout() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        setPreference(SettingsService.SETTING_USE_SIMPLE_SCROLLING, true);
        Uri uri = createDocument("original");
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "local edit");
            writeDocument(uri, "external edit");
            invokeSave(scenario);

            onView(withText(R.string.External_change_detected)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
            assertEquals("local edit", readDocument(uri));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void reloadConflictKeepsDraftUntilExternalReadSucceeds() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        String key = RecoveryKeys.forDocumentUri(uri.toString());
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "local edit");
            writeDocument(uri, "external edit");
            invokeSave(scenario);

            onView(withText(R.string.External_change_detected)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click());
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
            assertEquals("external edit", editorText(scenario));
            org.junit.Assert.assertNull(new RecoveryRepository(context).load(key, uri.toString()));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void conflictSaveAsLaunchesCreateDocumentAndPreservesOriginal() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        Intents.init();
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
                    .respondWith(new ActivityResult(Activity.RESULT_CANCELED, null));
            edit(scenario, "local edit");
            writeDocument(uri, "external edit");
            invokeSave(scenario);

            onView(withId(android.R.id.button3)).inRoot(isDialog()).perform(click());
            intended(hasAction(Intent.ACTION_CREATE_DOCUMENT));
            assertEquals("external edit", readDocument(uri));
        } finally {
            Intents.release();
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void autosaveDefersConflictAndRetainsRecovery() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        setPreference(SettingsService.SETTING_AUTO_SAVE_CURRENT_FILE, true);
        Uri uri = createDocument("original");
        String key = RecoveryKeys.forDocumentUri(uri.toString());
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "local edit");
            writeDocument(uri, "external edit");

            scenario.moveToState(Lifecycle.State.CREATED);
            assertEquals("external edit", readDocument(uri));
            assertNotNull(new RecoveryRepository(context).load(key, uri.toString()));

            scenario.moveToState(Lifecycle.State.RESUMED);
            onView(withText(R.string.External_change_detected)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            pressBack();
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void guardedLegacySaveDetectsExternalChange() throws Exception {
        setPreference(SettingsService.SETTING_LEGASY_FILE_PICKER, true);
        File file = new File(context.getCacheDir(), "external-change-" + java.lang.System.nanoTime() + ".txt");
        writeFile(file, "original");
        Intent intent = new Intent(context, EditorActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.fromFile(file));
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(intent)) {
            edit(scenario, "local edit");
            writeFile(file, "external edit");
            invokeSave(scenario);

            onView(withText(R.string.External_change_detected)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            pressBack();
            assertEquals("external edit", readFile(file));
        } finally {
            file.delete();
        }
    }

    @Test
    public void restoringDraftDetectsChangeMadeBeforeActivityCreation() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        String key = RecoveryKeys.forDocumentUri(uri.toString());
        RecoveryMetadata metadata = new RecoveryMetadata(
                key, uri.toString(), "notes.txt", false, "UTF-8", false,
                8L, null, DocumentSaveValidator.sha256("original".getBytes(StandardCharsets.UTF_8)),
                0, 0, 0, 0, 1
        );
        new RecoveryRepository(context).write(metadata, "local edit");
        writeDocument(uri, "external edit");

        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            onView(withText(R.string.Restore)).perform(click());
            onView(withText(R.string.External_change_detected)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            pressBack();
            assertEquals("local edit", editorText(scenario));
            assertEquals("external edit", readDocument(uri));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void failedReloadRetainsLocalEditorAndRecovery() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        String key = RecoveryKeys.forDocumentUri(uri.toString());
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "local edit");
            writeDocument(uri, "external edit");
            invokeSave(scenario);

            onView(withText(R.string.External_change_detected)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click());
            context.getContentResolver().delete(uri, null, null);
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());

            assertEquals("local edit", editorText(scenario));
            assertNotNull(new RecoveryRepository(context).load(key, uri.toString()));
        }
    }

    @Test
    public void equivalentExternalContentCompletesWithoutRewriting() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        String key = RecoveryKeys.forDocumentUri(uri.toString());
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "same result");
            writeDocument(uri, "same result");
            invokeSave(scenario);

            assertEquals("same result", readDocument(uri));
            org.junit.Assert.assertNull(new RecoveryRepository(context).load(key, uri.toString()));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void olderSaveCompletionRetainsRecoveryForNewerEditorGeneration() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        String key = RecoveryKeys.forDocumentUri(uri.toString());
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            long[] olderGeneration = new long[1];
            edit(scenario, "older local edit");
            scenario.onActivity(activity -> olderGeneration[0] = longField(activity, "editorGeneration"));
            edit(scenario, "newer local edit");
            writeDocument(uri, "older local edit");

            scenario.onActivity(activity -> invokeSaveCompletion(
                    activity,
                    olderGeneration[0],
                    key,
                    "older local edit".getBytes(StandardCharsets.UTF_8)
            ));
            scenario.moveToState(Lifecycle.State.CREATED);

            assertEquals("newer local edit", new RecoveryRepository(context).load(key, uri.toString()).text);
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void cleanSafDocumentReloadsWhenReturningToForeground() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            scenario.moveToState(Lifecycle.State.CREATED);
            writeDocument(uri, "external edit");
            scenario.moveToState(Lifecycle.State.RESUMED);

            assertEquals("external edit", editorText(scenario));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void dirtySimpleLayoutShowsConflictImmediatelyOnForeground() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        setPreference(SettingsService.SETTING_USE_SIMPLE_SCROLLING, true);
        Uri uri = createDocument("original");
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "local edit");
            scenario.moveToState(Lifecycle.State.CREATED);
            writeDocument(uri, "external edit");
            scenario.moveToState(Lifecycle.State.RESUMED);

            onView(withText(R.string.External_change_detected)).inRoot(isDialog())
                    .check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            pressBack();
            assertEquals("local edit", editorText(scenario));
            assertEquals("external edit", readDocument(uri));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void equivalentContentIsCompletedWhenReturningToForeground() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Uri uri = createDocument("original");
        String key = RecoveryKeys.forDocumentUri(uri.toString());
        try (ActivityScenario<EditorActivity> scenario = launch(uri)) {
            edit(scenario, "same result");
            scenario.moveToState(Lifecycle.State.CREATED);
            writeDocument(uri, "same result");
            scenario.moveToState(Lifecycle.State.RESUMED);

            assertEquals("same result", editorText(scenario));
            org.junit.Assert.assertNull(new RecoveryRepository(context).load(key, uri.toString()));
        } finally {
            context.getContentResolver().delete(uri, null, null);
        }
    }

    @Test
    public void unreadableLegacyDocumentLeavesForegroundEditorUnchanged() throws Exception {
        setPreference(SettingsService.SETTING_LEGASY_FILE_PICKER, true);
        File file = new File(context.getCacheDir(), "foreground-unreadable-" + java.lang.System.nanoTime() + ".txt");
        writeFile(file, "original");
        Intent intent = new Intent(context, EditorActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.fromFile(file));
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.CREATED);
            file.delete();
            scenario.moveToState(Lifecycle.State.RESUMED);

            assertEquals("original", editorText(scenario));
        } finally {
            file.delete();
        }
    }

    @Test
    public void cleanLegacyDocumentReloadsWhenReturningToForeground() throws Exception {
        setPreference(SettingsService.SETTING_LEGASY_FILE_PICKER, true);
        File file = new File(context.getCacheDir(), "foreground-reload-" + java.lang.System.nanoTime() + ".txt");
        writeFile(file, "original");
        Intent intent = new Intent(context, EditorActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.fromFile(file));
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.CREATED);
            writeFile(file, "external edit");
            scenario.moveToState(Lifecycle.State.RESUMED);

            assertEquals("external edit", editorText(scenario));
        } finally {
            file.delete();
        }
    }

    private ActivityScenario<EditorActivity> launch(Uri uri) {
        return ActivityScenario.launch(new Intent(context, EditorActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(uri));
    }

    private void edit(ActivityScenario<EditorActivity> scenario, String text) {
        scenario.onActivity(activity -> ((EditText) activity.findViewById(R.id.editText1)).setText(text));
    }

    private String editorText(ActivityScenario<EditorActivity> scenario) {
        String[] value = new String[1];
        scenario.onActivity(activity -> value[0] = ((EditText) activity.findViewById(R.id.editText1)).getText().toString());
        return value[0];
    }

    private void invokeSave(ActivityScenario<EditorActivity> scenario) {
        scenario.onActivity(activity -> {
            try {
                Method method = EditorActivity.class.getDeclaredMethod("saveFileIfNamed");
                method.setAccessible(true);
                method.invoke(activity);
            } catch (Exception error) {
                throw new AssertionError(error);
            }
        });
    }

    private static long longField(EditorActivity activity, String name) {
        try {
            Field field = EditorActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getLong(activity);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static void invokeSaveCompletion(
            EditorActivity activity,
            long generation,
            String recoveryKey,
            byte[] bytes
    ) {
        try {
            Method method = EditorActivity.class.getDeclaredMethod(
                    "completeSuccessfulSave", long.class, String.class, byte[].class, Long.class);
            method.setAccessible(true);
            method.invoke(activity, generation, recoveryKey, bytes, null);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private Uri createDocument(String content) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "textpad-external-" + java.lang.System.nanoTime() + ".txt");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/TextPadTests");
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IllegalStateException("Unable to create test document");
        }
        writeDocument(uri, content);
        return uri;
    }

    private void writeDocument(Uri uri, String content) throws Exception {
        try (java.io.OutputStream output = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IllegalStateException("Unable to write test document");
            }
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readDocument(Uri uri) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("Unable to read test document");
            }
            return readAll(input);
        }
    }

    private void writeFile(File file, String content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readFile(File file) throws Exception {
        try (InputStream input = new java.io.FileInputStream(file)) {
            return readAll(input);
        }
    }

    private String readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private void setPreference(String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
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
