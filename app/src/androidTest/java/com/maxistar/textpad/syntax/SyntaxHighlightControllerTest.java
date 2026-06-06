package com.maxistar.textpad.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.Looper;
import android.text.NoCopySpan;
import android.text.TextWatcher;
import android.text.Spanned;
import android.text.Editable;
import android.text.style.BackgroundColorSpan;
import android.view.inputmethod.BaseInputConnection;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class SyntaxHighlightControllerTest {
    private EditText editor;
    private SyntaxHighlightController controller;
    private ExecutorService executor;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> editor = new EditText(ApplicationProvider.getApplicationContext()));
    }

    @After
    public void tearDown() {
        if (controller != null) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(controller::destroy);
        } else if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void rendersOffMainThreadAndPreservesEditorState() throws Exception {
        AtomicBoolean tokenizedOnMain = new AtomicBoolean(true);
        AtomicInteger textChanges = new AtomicInteger();
        SyntaxTokenizerRegistry registry = new SyntaxTokenizerRegistry();
        registry.register(LanguageMode.JSON, (text, limit) -> {
            tokenizedOnMain.set(Looper.myLooper() == Looper.getMainLooper());
            return SyntaxTokenizationResult.success(Collections.singletonList(
                    new SyntaxToken(0, text.length(), SyntaxTokenType.STRING)));
        });
        createController(registry, 10);

        BackgroundColorSpan searchSpan = new BackgroundColorSpan(0xffffff00);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            editor.setText("\"value\"");
            editor.setSelection(2, 5);
            BaseInputConnection.setComposingSpans(editor.getText());
            editor.getText().setSpan(
                    searchSpan, 1, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            editor.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(
                        CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(
                        CharSequence s, int start, int before, int count) {
                    textChanges.incrementAndGet();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            controller.setEnabled(true);
            controller.setLanguageMode(LanguageMode.JSON);
            controller.start();
        });

        waitForSpanCount(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertFalse(tokenizedOnMain.get());
            assertEquals(2, editor.getSelectionStart());
            assertEquals(5, editor.getSelectionEnd());
            assertEquals("\"value\"", editor.getText().toString());
            assertEquals(0, textChanges.get());
            assertEquals(0, BaseInputConnection.getComposingSpanStart(editor.getText()));
            assertEquals(editor.length(),
                    BaseInputConnection.getComposingSpanEnd(editor.getText()));
            assertEquals(1, editor.getText().getSpans(
                    0, editor.length(), BackgroundColorSpan.class).length);
            SyntaxSpan syntaxSpan = editor.getText().getSpans(
                    0, editor.length(), SyntaxSpan.class)[0];
            assertTrue(syntaxSpan instanceof NoCopySpan);
        });
    }

    @Test
    public void latestGenerationWinsAndStaleSpansAreRemoved() throws Exception {
        createController(new SyntaxTokenizerRegistry(), 1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            editor.setText("{\"old\":1}");
            controller.setEnabled(true);
            controller.setLanguageMode(LanguageMode.JSON);
            controller.start();
            editor.setText("{\"latest\":2}");
            controller.onTextChanged();
        });

        waitForSpanCount(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (SyntaxSpan span : editor.getText().getSpans(
                    0, editor.length(), SyntaxSpan.class)) {
                assertTrue(editor.getText().getSpanEnd(span) <= editor.length());
            }
        });
    }

    @Test
    public void autoDetectionAndLifecycleReentryScheduleOnlyEligibleWork() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        SyntaxTokenizerRegistry registry = new SyntaxTokenizerRegistry();
        registry.register(LanguageMode.MARKDOWN, (text, limit) -> {
            calls.incrementAndGet();
            return SyntaxTokenizationResult.success(Collections.singletonList(
                    new SyntaxToken(0, text.length(), SyntaxTokenType.HEADING)));
        });
        createController(registry, 10);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            editor.setText("# Heading");
            controller.setEnabled(true);
            controller.setDisplayName("notes.txt");
            controller.start();
        });
        Thread.sleep(100);
        assertEquals(0, calls.get());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            controller.setDisplayName("notes.md");
            controller.stop();
        });
        Thread.sleep(100);
        assertEquals(0, calls.get());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(controller::start);
        waitForSpanCount(1);
        assertEquals(1, calls.get());
    }

    @Test
    public void disabledOversizedFailureAndTokenLimitFallBackToPlainText() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger limitReports = new AtomicInteger();
        SyntaxTokenizerRegistry registry = new SyntaxTokenizerRegistry();
        registry.register(LanguageMode.JSON, (text, limit) -> {
            calls.incrementAndGet();
            if ("failure".equals(text)) {
                throw new IllegalStateException("test");
            }
            return SyntaxTokenizationResult.limitExceeded();
        });
        executor = Executors.newSingleThreadExecutor();
        controller = new SyntaxHighlightController(
                editor,
                limitReports::incrementAndGet,
                new Handler(Looper.getMainLooper()),
                executor,
                registry,
                new LanguageDetector(),
                0,
                1,
                1
        );

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            editor.setText("{}");
            controller.setLanguageMode(LanguageMode.JSON);
            controller.start();
        });
        Thread.sleep(100);
        assertEquals(0, calls.get());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            controller.setEnabled(true);
            editor.setText(new char[SyntaxHighlightController.DOCUMENT_CHARACTER_LIMIT + 1],
                    0,
                    SyntaxHighlightController.DOCUMENT_CHARACTER_LIMIT + 1);
            controller.onTextChanged();
            controller.onTextChanged();
        });
        assertEquals(1, limitReports.get());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            controller.resetDocument("test.json");
            editor.setText("failure");
            controller.onTextChanged();
        });
        Thread.sleep(150);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(0, spanCount());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            editor.setText("{}");
            controller.onTextChanged();
        });
        Thread.sleep(150);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(0, spanCount());
    }

    private void createController(SyntaxTokenizerRegistry registry, int batchSize) {
        executor = Executors.newSingleThreadExecutor();
        controller = new SyntaxHighlightController(
                editor,
                () -> { },
                new Handler(Looper.getMainLooper()),
                executor,
                registry,
                new LanguageDetector(),
                0,
                100,
                batchSize
        );
    }

    private void waitForSpanCount(int minimum) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            Thread.sleep(25);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            if (spanCount() >= minimum) {
                return;
            }
        }
        assertTrue("Expected syntax spans", spanCount() >= minimum);
    }

    private int spanCount() {
        AtomicInteger count = new AtomicInteger();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> count.set(
                editor.getText().getSpans(0, editor.length(), SyntaxSpan.class).length));
        return count.get();
    }
}
