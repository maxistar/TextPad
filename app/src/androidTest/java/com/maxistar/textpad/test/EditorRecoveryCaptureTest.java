package com.maxistar.textpad.test;

import static org.junit.Assert.*;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.recovery.RecoveryCaptureScheduler;
import com.maxistar.textpad.recovery.RecoveryMetadata;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.recovery.RecoveryWriter;
import com.maxistar.textpad.service.SettingsService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class EditorRecoveryCaptureTest {
    @Rule
    public GrantPermissionRule permissions = GrantPermissionRule.grant(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        cleanup();
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(SettingsService.SETTING_OPEN_LAST_FILE, false)
                .putBoolean(SettingsService.SETTING_AUTO_SAVE_CURRENT_FILE, false).commit();
        ServiceLocator.getInstance().getSettingsService(context).reloadSettings(context);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void burstCapturesLatestTextSelectionAndGenerationOnMainThread() {
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                Fixture fixture = new Fixture(activity);
                long initialGeneration = (Long) get(activity, "editorGeneration");
                for (int i = 0; i < 24; i++) {
                    fixture.editor.append("x");
                    fixture.clock.advance(10);
                }
                fixture.editor.setSelection(1, 3);
                fixture.clock.advance(589);
                assertEquals(0, fixture.captures);
                assertTrue(fixture.snapshots.isEmpty());
                fixture.clock.advance(1);
                fixture.await();
                assertEquals(1, fixture.captures);
                assertEquals(1, fixture.snapshots.size());
                RecoveryWriter.Snapshot snapshot = fixture.snapshots.get(0);
                assertEquals("basexxxxxxxxxxxxxxxxxxxxxxxx", snapshot.text);
                assertEquals(1, snapshot.metadata.cursorStart);
                assertEquals(3, snapshot.metadata.cursorEnd);
                assertEquals(initialGeneration + 24, snapshot.metadata.generation);
            });
        }
    }

    @Test
    public void lifecycleFlushCapturesImmediatelyAndCancelsDelayedCapture() {
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            Fixture[] fixture = new Fixture[1];
            scenario.onActivity(activity -> {
                fixture[0] = new Fixture(activity);
                fixture[0].editor.append("latest");
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            int writes = fixture[0].snapshots.size();
            assertTrue(writes > 0);
            assertEquals("baselatest", fixture[0].snapshots.get(writes - 1).text);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> fixture[0].clock.advance(600));
            assertEquals(writes, fixture[0].snapshots.size());
            assertEquals(0, fixture[0].captures);
        }
    }

    @Test
    public void clearCancelsOldSessionAndAllowsLowerGenerationForNewDocument() {
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                Fixture fixture = new Fixture(activity);
                fixture.editor.append("old");
                Runnable stale = fixture.clock.pending;
                String previousKey = (String) get(activity, "recoveryKey");
                activity.clearFile();
                fixture.editor.append("new");
                stale.run();
                assertEquals(0, fixture.captures);
                fixture.clock.advance(600);
                fixture.await();
                assertEquals(1, fixture.snapshots.size());
                RecoveryWriter.Snapshot snapshot = fixture.snapshots.get(0);
                assertEquals("new", snapshot.text);
                assertEquals(1, snapshot.metadata.generation);
                assertNotEquals(previousKey, snapshot.metadata.recoveryKey);
            });
        }
    }

    @Test
    public void replacementAndDiscardInvalidateDelayedCallbacks() {
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                Fixture fixture = new Fixture(activity);
                fixture.editor.append("old");
                Runnable stale = fixture.clock.pending;
                invoke(activity, "setEditorText", new Class<?>[]{String.class, boolean.class}, "replacement", false);
                stale.run();
                assertEquals(0, fixture.captures);
                fixture.editor.append("edit");
                invoke(activity, "discardCurrentRecovery");
                fixture.clock.advance(600);
                fixture.await();
                assertTrue(fixture.snapshots.isEmpty());
                assertNull(new RecoveryRepository(context).loadActive());
            });
        }
    }

    @Test
    public void successfulSaveCancelsCaptureForTheSameNamedDocument() {
        File file = new File(context.getCacheDir(), "recovery-capture-save.txt");
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                Fixture fixture = new Fixture(activity);
                invoke(activity, "setFilename", new Class<?>[]{String.class}, Uri.fromFile(file).toString());
                invoke(activity, "saveNamedFile");
                fixture.editor.append("saved");
                Runnable stale = fixture.clock.pending;
                invoke(activity, "saveNamedFile");
                assertEquals(false, get(activity, "changed"));
                stale.run();
                fixture.clock.advance(600);
                fixture.await();
                assertEquals(0, fixture.captures);
                assertTrue(fixture.snapshots.isEmpty());
                assertNull(new RecoveryRepository(context).loadActive());
            });
        } finally {
            file.delete();
        }
    }

    @Test
    public void pendingRecoveryDecisionPreventsCapturingUnrelatedText() {
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                Fixture fixture = new Fixture(activity);
                fixture.editor.append("unrelated");
                set(activity, "recoveryDecisionPending", true);
                fixture.clock.advance(600);
                fixture.await();
                assertTrue(fixture.snapshots.isEmpty());
            });
        }
    }

    @Test
    public void destroyedActivityCannotBeCapturedByStaleCallback() {
        Fixture[] fixture = new Fixture[1];
        Runnable[] stale = new Runnable[1];
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                fixture[0] = new Fixture(activity);
                fixture[0].editor.append("latest");
                stale[0] = fixture[0].clock.pending;
            });
        }
        int writes = fixture[0].snapshots.size();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            stale[0].run();
            fixture[0].clock.advance(600);
        });
        assertEquals(writes, fixture[0].snapshots.size());
        assertEquals(0, fixture[0].captures);
        assertNull(fixture[0].clock.pending);
    }

    private final class Fixture {
        final EditText editor;
        final FakeScheduler clock = new FakeScheduler();
        final List<RecoveryWriter.Snapshot> snapshots = java.util.Collections.synchronizedList(new ArrayList<>());
        final RecoveryWriter writer;
        int captures;

        Fixture(EditorActivity activity) {
            ((RecoveryCaptureScheduler) get(activity, "recoveryCapture")).close();
            RecoveryWriter oldWriter = (RecoveryWriter) get(activity, "recoveryWriter");
            assertTrue(oldWriter.cancelAndWait(null, 0, 2000));
            oldWriter.shutdown();
            RecoveryRepository repository = new RecoveryRepository(context) {
                @Override
                public synchronized RecoveryMetadata write(RecoveryMetadata metadata, String text) throws Exception {
                    snapshots.add(new RecoveryWriter.Snapshot(metadata, text));
                    return super.write(metadata, text);
                }
            };
            writer = new RecoveryWriter(repository);
            set(activity, "recoveryWriter", writer);
            set(activity, "recoveryRepository", repository);
            set(activity, "recoveryCapture", new RecoveryCaptureScheduler(clock, () -> {
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                captures++;
                invoke(activity, "captureScheduledRecoverySnapshot");
            }));
            editor = activity.findViewById(R.id.editText1);
            editor.setText("base");
            invoke(activity, "flushRecoverySnapshot");
            snapshots.clear();
        }

        void await() {
            assertTrue(writer.cancelAndWait(null, 0, 2000));
        }
    }

    private static final class FakeScheduler implements RecoveryCaptureScheduler.Scheduler {
        private long now;
        private long deadline;
        private Runnable pending;

        @Override
        public void postDelayed(Runnable callback, long delayMillis) {
            pending = callback;
            deadline = now + delayMillis;
        }

        @Override
        public void removeCallbacks(Runnable callback) {
            if (pending == callback) {
                pending = null;
            }
        }

        void advance(long millis) {
            now += millis;
            if (pending != null && now >= deadline) {
                Runnable callback = pending;
                pending = null;
                callback.run();
            }
        }
    }

    private static Object get(EditorActivity activity, String name) {
        try {
            Field field = EditorActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(activity);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static void set(EditorActivity activity, String name, Object value) {
        try {
            Field field = EditorActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(activity, value);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static void invoke(EditorActivity activity, String name) {
        invoke(activity, name, new Class<?>[0]);
    }

    private static void invoke(EditorActivity activity, String name, Class<?>[] types, Object... args) {
        try {
            Method method = EditorActivity.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(activity, args);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private void cleanup() {
        context.getSharedPreferences("editor_recovery", Context.MODE_PRIVATE).edit().clear().commit();
        delete(new RecoveryRepository(context).getDirectoryForTests());
    }

    private static void delete(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        file.delete();
    }
}
