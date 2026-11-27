package com.maxistar.textpad.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Debug;
import android.os.Looper;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.EditText;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.recovery.RecoveryDraft;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.recovery.RecoveryWriter;
import com.maxistar.textpad.service.SettingsService;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Opt-in comparison workload: pass instrumentation argument profileRecovery=true. */
@RunWith(AndroidJUnit4.class)
public class RecoveryMemoryProfileTest {
    @Rule
    public GrantPermissionRule permissions = GrantPermissionRule.grant(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private final AtomicInteger captures = new AtomicInteger();
    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        cleanup();
        configure(false);
    }

    @After
    public void tearDown() {
        cleanup();
        configure(false);
    }

    @Test
    public void burstDoesNotCopyDocumentBeforeQuietPeriod() {
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                EditText editor = prepare(activity, document(32768, false));
                captures.set(0);
                for (int i = 0; i < 24; i++) {
                    editor.append("x");
                }
                // Stay in the same main-thread turn: no delayed callback can have run.
                assertEquals("Edit events must not eagerly copy the document", 0, captures.get());
            });
        }
    }

    @Test
    public void profileRecoveryAllocations() throws Exception {
        Assume.assumeTrue("Opt-in memory comparison workload", "true".equals(
                InstrumentationRegistry.getArguments().getString("profileRecovery")));
        for (boolean simple : new boolean[]{false, true}) {
            configure(simple);
            for (int size : new int[]{32768, 2 * 1024 * 1024}) {
                for (boolean unicode : new boolean[]{false, true}) {
                    profile(simple, size, unicode);
                }
            }
        }
    }

    private void profile(boolean simple, int size, boolean unicode) throws Exception {
        cleanup();
        String content = document(size, unicode);
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> prepare(activity, content));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            java.lang.System.gc();
            Thread.sleep(100);
            captures.set(0);
            String label = (simple ? "simple" : "standard") + "-" + size + "-"
                    + (unicode ? "unicode" : "ascii");
            File trace = new File(context.getCacheDir(), "recovery-" + label + ".trace");
            Debug.startMethodTracingSampling(trace.getAbsolutePath(), 8 * 1024 * 1024, 1000);
            try (HeapSample sample = new HeapSample()) {
                scenario.onActivity(activity -> {
                    EditText editor = activity.findViewById(R.id.editText1);
                    for (int i = 0; i < 24; i++) {
                        editor.append("x");
                    }
                });
                // Only this optional measurement uses real time; scheduling assertions use a fake clock.
                Thread.sleep(1000);
                scenario.onActivity(activity -> awaitWriter(activity));
                sample.report(label + "-quiet", captures.get());
            } finally {
                Debug.stopMethodTracing();
            }
            captures.set(0);
            try (HeapSample sample = new HeapSample()) {
                scenario.onActivity(activity -> {
                    ((EditText) activity.findViewById(R.id.editText1)).append("!");
                    invoke(activity, "flushRecoverySnapshot");
                });
                sample.report(label + "-flush", captures.get());
            }
            RecoveryDraft restored = new RecoveryRepository(context).loadActive();
            assertNotNull(restored);
            assertEquals(content + "xxxxxxxxxxxxxxxxxxxxxxxx!", restored.text);
        }
    }

    private EditText prepare(EditorActivity activity, String content) {
        EditText editor = activity.findViewById(R.id.editText1);
        editor.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        editor.setEditableFactory(new Editable.Factory() {
            @Override
            public Editable newEditable(CharSequence source) {
                return new SpannableStringBuilder(source) {
                    @Override
                    public String toString() {
                        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                            if (frame.getMethodName().equals("createRecoverySnapshot")) {
                                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                                captures.incrementAndGet();
                                break;
                            }
                        }
                        return super.toString();
                    }
                };
            }
        });
        editor.setText(content);
        invoke(activity, "flushRecoverySnapshot");
        return editor;
    }

    private void configure(boolean simple) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(SettingsService.SETTING_USE_SIMPLE_SCROLLING, simple)
                .putBoolean(SettingsService.SETTING_OPEN_LAST_FILE, false)
                .putBoolean(SettingsService.SETTING_AUTO_SAVE_CURRENT_FILE, false)
                .commit();
        ServiceLocator.getInstance().getSettingsService(context).reloadSettings(context);
    }

    private void cleanup() {
        context.getSharedPreferences("editor_recovery", Context.MODE_PRIVATE).edit().clear().commit();
        delete(new RecoveryRepository(context).getDirectoryForTests());
    }

    private static String document(int length, boolean unicode) {
        String row = unicode ? "Привет 世界 🌍\n" : "0123456789abcdef\n";
        StringBuilder text = new StringBuilder(length + row.length());
        while (text.length() < length) {
            text.append(row);
        }
        return text.substring(0, length);
    }

    private static void invoke(EditorActivity activity, String name) {
        try {
            Method method = EditorActivity.class.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static void awaitWriter(EditorActivity activity) {
        try {
            Field field = EditorActivity.class.getDeclaredField("recoveryWriter");
            field.setAccessible(true);
            assertTrue(((RecoveryWriter) field.get(activity)).cancelAndWait(null, 0, 10000));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
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

    private static final class HeapSample implements AutoCloseable {
        private final long allocated = stat("art.gc.bytes-allocated");
        private final long collections = stat("art.gc.gc-count");
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicLong peak = new AtomicLong(usedHeap());
        private final Thread sampler = new Thread(() -> {
            while (running.get()) {
                peak.accumulateAndGet(usedHeap(), Math::max);
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        }, "recovery-heap-sampler");

        HeapSample() {
            sampler.start();
        }

        void report(String label, int captures) {
            peak.accumulateAndGet(usedHeap(), Math::max);
            String result = label + " captures=" + captures
                    + " allocated=" + (stat("art.gc.bytes-allocated") - allocated)
                    + " peakJavaHeap=" + peak.get()
                    + " gc=" + (stat("art.gc.gc-count") - collections);
            Log.i("RecoveryProfile", result);
            Bundle status = new Bundle();
            status.putString("stream", result + "\n");
            InstrumentationRegistry.getInstrumentation().sendStatus(0, status);
        }

        @Override
        public void close() throws InterruptedException {
            running.set(false);
            sampler.join();
        }

        private static long usedHeap() {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        }

        private static long stat(String name) {
            String value = Debug.getRuntimeStat(name);
            return value == null ? 0 : Long.parseLong(value);
        }
    }
}
