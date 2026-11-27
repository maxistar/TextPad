package com.maxistar.textpad.recovery;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecoveryCaptureSchedulerTest {
    @Test
    public void burstCapturesLatestStateOnlyAfterQuietPeriod() {
        FakeScheduler clock = new FakeScheduler();
        AtomicInteger generation = new AtomicInteger();
        List<Integer> captured = new ArrayList<>();
        RecoveryCaptureScheduler capture = new RecoveryCaptureScheduler(clock,
                () -> captured.add(generation.get()));
        for (int i = 0; i < 24; i++) {
            generation.incrementAndGet();
            capture.schedule();
            clock.advance(10);
            assertTrue(captured.isEmpty());
        }
        clock.advance(589);
        assertTrue(captured.isEmpty());
        clock.advance(1);
        assertEquals(java.util.Collections.singletonList(24), captured);
        assertTrue(clock.tasks.isEmpty());
    }

    @Test
    public void cancelledCallbackCannotCaptureAReplacementDocument() {
        FakeScheduler clock = new FakeScheduler();
        AtomicInteger captured = new AtomicInteger();
        RecoveryCaptureScheduler capture = new RecoveryCaptureScheduler(clock, captured::incrementAndGet);
        capture.schedule();
        Runnable stale = clock.tasks.get(0).callback;
        capture.cancel();
        capture.schedule();
        stale.run();
        assertEquals(0, captured.get());
        clock.advance(600);
        assertEquals(1, captured.get());
    }

    @Test
    public void flushCanCancelDelayAndCaptureImmediatelyWithoutASecondCapture() {
        FakeScheduler clock = new FakeScheduler();
        AtomicInteger captured = new AtomicInteger();
        RecoveryCaptureScheduler capture = new RecoveryCaptureScheduler(clock, captured::incrementAndGet);
        capture.schedule();
        clock.advance(100);
        capture.cancel();
        captured.incrementAndGet();
        clock.advance(1000);
        assertEquals(1, captured.get());
    }

    @Test
    public void closingRemovesCallbacksAndDisablesFutureRequests() {
        FakeScheduler clock = new FakeScheduler();
        AtomicInteger captured = new AtomicInteger();
        RecoveryCaptureScheduler capture = new RecoveryCaptureScheduler(clock, captured::incrementAndGet);
        capture.schedule();
        Runnable stale = clock.tasks.get(0).callback;
        capture.close();
        stale.run();
        capture.schedule();
        clock.advance(1000);
        assertTrue(clock.tasks.isEmpty());
        assertEquals(0, captured.get());
    }

    private static final class FakeScheduler implements RecoveryCaptureScheduler.Scheduler {
        private long now;
        private final List<Task> tasks = new ArrayList<>();

        @Override
        public void postDelayed(Runnable callback, long delayMillis) {
            tasks.add(new Task(callback, now + delayMillis));
        }

        @Override
        public void removeCallbacks(Runnable callback) {
            tasks.removeIf(task -> task.callback == callback);
        }

        void advance(long millis) {
            now += millis;
            for (Task task : new ArrayList<>(tasks)) {
                if (task.deadline <= now && tasks.remove(task)) {
                    task.callback.run();
                }
            }
        }
    }

    private static final class Task {
        final Runnable callback;
        final long deadline;

        Task(Runnable callback, long deadline) {
            this.callback = callback;
            this.deadline = deadline;
        }
    }
}
