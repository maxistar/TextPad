package com.maxistar.textpad.syntax;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spanned;
import android.widget.EditText;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class SyntaxHighlightController {
    public interface Listener {
        void onDocumentTooLarge();
    }

    public static final int DOCUMENT_CHARACTER_LIMIT = 256_000;
    static final int DEFAULT_TOKEN_LIMIT = 20_000;
    static final int DEFAULT_BATCH_SIZE = 200;
    static final long DEFAULT_DEBOUNCE_MILLIS = 250;

    private final EditText editor;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final SyntaxTokenizerRegistry registry;
    private final LanguageDetector detector;
    private final Listener listener;
    private final SyntaxGeneration generations = new SyntaxGeneration();
    private final int tokenLimit;
    private final int batchSize;
    private final long debounceMillis;

    private boolean enabled;
    private boolean active;
    private boolean limitReported;
    private LanguageMode selectedMode = LanguageMode.AUTO;
    private String displayName;
    private SyntaxPalette palette = SyntaxPalette.light();
    private Runnable pendingDebounce;
    private Future<?> pendingTokenization;

    public SyntaxHighlightController(EditText editor, Listener listener) {
        this(
                editor,
                listener,
                new Handler(Looper.getMainLooper()),
                Executors.newSingleThreadExecutor(),
                new SyntaxTokenizerRegistry(),
                new LanguageDetector(),
                DEFAULT_DEBOUNCE_MILLIS,
                DEFAULT_TOKEN_LIMIT,
                DEFAULT_BATCH_SIZE
        );
    }

    SyntaxHighlightController(
            EditText editor,
            Listener listener,
            Handler mainHandler,
            ExecutorService executor,
            SyntaxTokenizerRegistry registry,
            LanguageDetector detector,
            long debounceMillis,
            int tokenLimit,
            int batchSize
    ) {
        this.editor = editor;
        this.listener = listener;
        this.mainHandler = mainHandler;
        this.executor = executor;
        this.registry = registry;
        this.detector = detector;
        this.debounceMillis = debounceMillis;
        this.tokenLimit = tokenLimit;
        this.batchSize = batchSize;
    }

    public void start() {
        active = true;
        requestHighlight();
    }

    public void stop() {
        active = false;
        cancelPendingWork();
    }

    public void destroy() {
        stop();
        executor.shutdownNow();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        invalidateAndClear();
        if (enabled) {
            requestHighlight();
        }
    }

    public void setLanguageMode(LanguageMode mode) {
        LanguageMode nextMode = mode == null ? LanguageMode.AUTO : mode;
        if (selectedMode == nextMode) {
            return;
        }
        selectedMode = nextMode;
        invalidateAndClear();
        requestHighlight();
    }

    public LanguageMode getLanguageMode() {
        return selectedMode;
    }

    public LanguageMode getResolvedLanguageMode() {
        return detector.resolve(selectedMode, displayName);
    }

    public void setDisplayName(String displayName) {
        String nextName = displayName == null ? "" : displayName;
        if (nextName.equals(this.displayName)) {
            return;
        }
        this.displayName = nextName;
        limitReported = false;
        invalidateAndClear();
        requestHighlight();
    }

    public void resetDocument(String displayName) {
        selectedMode = LanguageMode.AUTO;
        limitReported = false;
        this.displayName = displayName == null ? "" : displayName;
        invalidateAndClear();
        requestHighlight();
    }

    public void setPalette(SyntaxPalette palette) {
        if (palette == null || this.palette == palette) {
            return;
        }
        this.palette = palette;
        invalidateAndClear();
        requestHighlight();
    }

    public void onTextChanged() {
        invalidateAndClear();
        requestHighlight();
    }

    public void requestHighlight() {
        if (!active || !enabled) {
            return;
        }
        cancelPendingWork();
        final long generation = generations.next();
        removeSyntaxSpans(editor.getText(), null);

        final String snapshot = editor.getText().toString();
        if (snapshot.length() > DOCUMENT_CHARACTER_LIMIT) {
            reportLimitOnce();
            return;
        }
        final LanguageMode resolvedMode = detector.resolve(selectedMode, displayName);
        if (resolvedMode == LanguageMode.PLAIN_TEXT) {
            return;
        }

        pendingDebounce = () -> {
            if (!isCurrent(generation)) {
                return;
            }
            pendingTokenization = executor.submit(
                    () -> tokenize(snapshot, resolvedMode, generation));
        };
        mainHandler.postDelayed(pendingDebounce, debounceMillis);
    }

    private void tokenize(String snapshot, LanguageMode mode, long generation) {
        try {
            SyntaxTokenizationResult result = registry.get(mode).tokenize(snapshot, tokenLimit);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            mainHandler.post(() -> render(result, snapshot.length(), generation));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException failure) {
            mainHandler.post(() -> clearFailedGeneration(generation));
        }
    }

    private void render(
            SyntaxTokenizationResult result,
            int snapshotLength,
            long generation
    ) {
        if (!isCurrent(generation)) {
            removeSyntaxSpans(editor.getText(), generation);
            return;
        }
        Editable editable = editor.getText();
        if (result.isTokenLimitExceeded() || editable.length() != snapshotLength) {
            removeSyntaxSpans(editable, null);
            return;
        }
        removeSyntaxSpans(editable, null);
        renderBatch(result.getTokens(), 0, generation, snapshotLength);
    }

    private void renderBatch(
            List<SyntaxToken> tokens,
            int startIndex,
            long generation,
            int snapshotLength
    ) {
        if (!isCurrent(generation) || editor.getText().length() != snapshotLength) {
            removeSyntaxSpans(editor.getText(), generation);
            return;
        }
        Editable editable = editor.getText();
        int endIndex = Math.min(startIndex + batchSize, tokens.size());
        try {
            for (int index = startIndex; index < endIndex; index++) {
                SyntaxToken token = tokens.get(index);
                if (token.getEnd() <= editable.length()) {
                    editable.setSpan(
                            new SyntaxSpan(palette.colorFor(token.getType()), generation),
                            token.getStart(),
                            token.getEnd(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }
        } catch (RuntimeException renderingFailure) {
            removeSyntaxSpans(editable, generation);
            return;
        }
        if (endIndex < tokens.size()) {
            mainHandler.post(
                    () -> renderBatch(tokens, endIndex, generation, snapshotLength));
        }
    }

    private void clearFailedGeneration(long generation) {
        if (generations.isCurrent(generation)) {
            removeSyntaxSpans(editor.getText(), null);
        } else {
            removeSyntaxSpans(editor.getText(), generation);
        }
    }

    private boolean isCurrent(long generation) {
        return active && enabled && generations.isCurrent(generation);
    }

    private void reportLimitOnce() {
        if (!limitReported && listener != null) {
            limitReported = true;
            listener.onDocumentTooLarge();
        }
    }

    private void invalidateAndClear() {
        generations.next();
        cancelPendingWork();
        removeSyntaxSpans(editor.getText(), null);
    }

    private void cancelPendingWork() {
        if (pendingDebounce != null) {
            mainHandler.removeCallbacks(pendingDebounce);
            pendingDebounce = null;
        }
        if (pendingTokenization != null) {
            pendingTokenization.cancel(true);
            pendingTokenization = null;
        }
    }

    static void removeSyntaxSpans(Editable editable, Long generation) {
        SyntaxSpan[] spans = editable.getSpans(0, editable.length(), SyntaxSpan.class);
        for (SyntaxSpan span : spans) {
            if (generation == null || span.getGeneration() == generation) {
                editable.removeSpan(span);
            }
        }
    }
}
