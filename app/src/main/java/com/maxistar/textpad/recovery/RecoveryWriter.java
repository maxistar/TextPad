package com.maxistar.textpad.recovery;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class RecoveryWriter {
    private static final String LOG_TAG = "EditorRecovery";
    private static final long DEBOUNCE_MILLIS = 600;

    private final RecoveryRepository repository;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "textpad-recovery");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> pending;
    private long newestGeneration;

    public RecoveryWriter(RecoveryRepository repository) {
        this.repository = repository;
    }

    public synchronized void schedule(Snapshot snapshot) {
        newestGeneration = Math.max(newestGeneration, snapshot.metadata.generation);
        if (pending != null) {
            pending.cancel(false);
        }
        pending = executor.schedule(() -> writeIfCurrent(snapshot), DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
    }

    public boolean flushAndWait(Snapshot snapshot, long timeoutMillis) {
        synchronized (this) {
            newestGeneration = Math.max(newestGeneration, snapshot.metadata.generation);
            if (pending != null) {
                pending.cancel(false);
                pending = null;
            }
        }
        Future<?> future = executor.submit(() -> writeIfCurrent(snapshot));
        try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception error) {
            Log.w(LOG_TAG, "Recovery flush did not complete", error);
            return false;
        }
    }

    public synchronized void cancelPending() {
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
    }

    public boolean cancelAndWait(long generation, long timeoutMillis) {
        synchronized (this) {
            newestGeneration = Math.max(newestGeneration, generation);
            if (pending != null) {
                pending.cancel(false);
                pending = null;
            }
        }
        Future<?> barrier = executor.submit(() -> { });
        try {
            barrier.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception error) {
            Log.w(LOG_TAG, "Recovery writer did not become idle", error);
            return false;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void writeIfCurrent(Snapshot snapshot) {
        synchronized (this) {
            if (snapshot.metadata.generation < newestGeneration) {
                return;
            }
        }
        try {
            repository.write(snapshot.metadata, snapshot.text);
        } catch (Exception error) {
            Log.e(LOG_TAG, "Unable to write recovery draft", error);
        }
    }

    public static final class Snapshot {
        public final RecoveryMetadata metadata;
        public final String text;

        public Snapshot(RecoveryMetadata metadata, String text) {
            this.metadata = metadata;
            this.text = text;
        }
    }
}
