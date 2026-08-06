package com.maxistar.textpad.test.recovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.maxistar.textpad.recovery.RecoveryDraft;
import com.maxistar.textpad.recovery.RecoveryKeys;
import com.maxistar.textpad.recovery.RecoveryMetadata;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.recovery.RecoveryWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class RecoveryWriterTest {
    private RecoveryRepository repository;
    private RecoveryWriter writer;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("editor_recovery", Context.MODE_PRIVATE).edit().clear().commit();
        repository = new RecoveryRepository(context);
        deleteRecursively(repository.getDirectoryForTests());
        writer = new RecoveryWriter(repository);
    }

    @After
    public void tearDown() {
        writer.shutdown();
        deleteRecursively(repository.getDirectoryForTests());
    }

    @Test
    public void newerGenerationSupersedesDebouncedSnapshot() {
        String key = RecoveryKeys.forUntitledDocument();
        writer.schedule(snapshot(key, 1, "old"));

        writer.flushAndWait(snapshot(key, 2, "new"), 2000);

        RecoveryDraft draft = repository.load(key, null);
        assertNotNull(draft);
        assertEquals("new", draft.text);
        assertEquals(2, draft.metadata.generation);
    }

    @Test
    public void staleGenerationCannotReplaceNewerSnapshot() {
        String key = RecoveryKeys.forUntitledDocument();
        writer.flushAndWait(snapshot(key, 5, "newest"), 2000);
        writer.flushAndWait(snapshot(key, 4, "stale"), 2000);

        RecoveryDraft draft = repository.load(key, null);
        assertNotNull(draft);
        assertEquals("newest", draft.text);
        assertEquals(5, draft.metadata.generation);
    }

    @Test
    public void failedWritePreservesPreviousValidGeneration() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(snapshot(key, 1, "valid").metadata, "valid");
        RecoveryRepository failingRepository = new RecoveryRepository(ApplicationProvider.getApplicationContext()) {
            @Override
            public synchronized RecoveryMetadata write(RecoveryMetadata metadata, String text) throws Exception {
                throw new java.io.IOException("expected test failure");
            }
        };
        RecoveryWriter failingWriter = new RecoveryWriter(failingRepository);
        try {
            failingWriter.flushAndWait(snapshot(key, 2, "failed"), 2000);
        } finally {
            failingWriter.shutdown();
        }

        RecoveryDraft draft = repository.load(key, null);
        assertNotNull(draft);
        assertEquals("valid", draft.text);
        assertEquals(1, draft.metadata.generation);
    }

    @Test
    public void cancellationBarrierPreventsQueuedDraftFromReappearingAfterDelete() {
        String key = RecoveryKeys.forUntitledDocument();
        writer.schedule(snapshot(key, 3, "queued"));
        writer.cancelAndWait(3, 2000);
        repository.delete(key);

        try {
            Thread.sleep(800);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
        assertEquals(null, repository.load(key, null));
    }

    private RecoveryWriter.Snapshot snapshot(String key, long generation, String text) {
        RecoveryMetadata metadata = new RecoveryMetadata(
                key, null, "newfile.txt", true, "UTF-8", false,
                null, null, null, 0, 0, 0, 0, generation
        );
        return new RecoveryWriter.Snapshot(metadata, text);
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
