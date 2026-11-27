package com.maxistar.textpad.recovery;

/** Debounces capture itself; all methods and callbacks belong to the editor's UI thread. */
public final class RecoveryCaptureScheduler {
    private static final long QUIET_PERIOD_MILLIS = 600;

    public interface Scheduler {
        void postDelayed(Runnable callback, long delayMillis);
        void removeCallbacks(Runnable callback);
    }

    private final Scheduler scheduler;
    private Runnable capture;
    private Runnable pending;
    private long request;

    public RecoveryCaptureScheduler(Scheduler scheduler, Runnable capture) {
        this.scheduler = scheduler;
        this.capture = capture;
    }

    public void schedule() {
        cancel();
        if (capture == null) {
            return;
        }
        long currentRequest = request;
        pending = () -> {
            if (capture == null || currentRequest != request) {
                return;
            }
            pending = null;
            capture.run();
        };
        scheduler.postDelayed(pending, QUIET_PERIOD_MILLIS);
    }

    /** Also invalidates a callback already obtained by an event loop or a test scheduler. */
    public void cancel() {
        request++;
        if (pending != null) {
            scheduler.removeCallbacks(pending);
            pending = null;
        }
    }

    public void close() {
        cancel();
        capture = null;
    }
}
