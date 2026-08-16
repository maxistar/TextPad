package com.maxistar.textpad.recovery;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
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
    private String pendingRecoveryKey;
    private final Map<String, Long> newestGenerations = new HashMap<>();

    public RecoveryWriter(RecoveryRepository repository) {
        this.repository = repository;
    }

    public synchronized void schedule(Snapshot snapshot) {
        recordGeneration(snapshot.metadata.recoveryKey, snapshot.metadata.generation);
        if (pending != null) {
            pending.cancel(false);
        }
        pendingRecoveryKey = snapshot.metadata.recoveryKey;
        pending = executor.schedule(() -> writeIfCurrent(snapshot), DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
    }

    public boolean flushAndWait(Snapshot snapshot, long timeoutMillis) {
        synchronized (this) {
            recordGeneration(snapshot.metadata.recoveryKey, snapshot.metadata.generation);
            if (pending != null && snapshot.metadata.recoveryKey.equals(pendingRecoveryKey)) {
                pending.cancel(false);
                pending = null;
                pendingRecoveryKey = null;
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
            pendingRecoveryKey = null;
        }
    }

    public boolean cancelAndWait(String recoveryKey, long generation, long timeoutMillis) {
        synchronized (this) {
            recordGeneration(recoveryKey, generation);
            if (pending != null && recoveryKey != null && recoveryKey.equals(pendingRecoveryKey)) {
                pending.cancel(false);
                pending = null;
                pendingRecoveryKey = null;
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
            Long newestGeneration = newestGenerations.get(snapshot.metadata.recoveryKey);
            if (newestGeneration != null && snapshot.metadata.generation < newestGeneration) {
                return;
            }
        }
        try {
            repository.write(snapshot.metadata, snapshot.text);
        } catch (Exception error) {
            Log.e(LOG_TAG, "Unable to write recovery draft", error);
        }
    }

    private void recordGeneration(String recoveryKey, long generation) {
        if (recoveryKey == null) {
            return;
        }
        Long newest = newestGenerations.get(recoveryKey);
        newestGenerations.put(recoveryKey, newest == null ? generation : Math.max(newest, generation));
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
