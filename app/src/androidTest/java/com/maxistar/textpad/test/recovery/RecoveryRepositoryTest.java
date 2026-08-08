package com.maxistar.textpad.test.recovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.maxistar.textpad.recovery.RecoveryDraft;
import com.maxistar.textpad.recovery.RecoveryKeys;
import com.maxistar.textpad.recovery.RecoveryMetadata;
import com.maxistar.textpad.recovery.RecoveryRepository;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class RecoveryRepositoryTest {
    private RecoveryRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("editor_recovery", Context.MODE_PRIVATE).edit().clear().commit();
        repository = new RecoveryRepository(context);
        deleteRecursively(repository.getDirectoryForTests());
    }

    @After
    public void tearDown() {
        deleteRecursively(repository.getDirectoryForTests());
    }

    @Test
    public void metadataRoundTripIgnoresUnknownFields() throws Exception {
        RecoveryMetadata source = metadata(RecoveryKeys.forUntitledDocument(), null, 7)
                .published(5, "0123456789abcdef", 123456L);
        JSONObject json = source.toJson();
        json.put("futureField", "ignored");

        RecoveryMetadata restored = RecoveryMetadata.fromJson(json);

        assertEquals(source.recoveryKey, restored.recoveryKey);
        assertEquals(source.encoding, restored.encoding);
        assertEquals(source.cursorStart, restored.cursorStart);
        assertEquals(source.generation, restored.generation);
    }

    @Test(expected = Exception.class)
    public void futureMetadataVersionIsRejected() throws Exception {
        JSONObject json = metadata(RecoveryKeys.forUntitledDocument(), null, 1).toJson();
        json.put("formatVersion", RecoveryMetadata.CURRENT_FORMAT_VERSION + 1);
        RecoveryMetadata.fromJson(json);
    }

    @Test
    public void writesSeparateDraftAndMetadataAndLoadsThem() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 3), "recover me");

        RecoveryDraft loaded = repository.load(key, null);

        assertNotNull(loaded);
        assertEquals("recover me", loaded.text);
        assertEquals(3, loaded.metadata.generation);
        assertTrue(new File(repository.getDirectoryForTests(), key + ".draft").isFile());
        assertTrue(new File(repository.getDirectoryForTests(), key + ".json").isFile());
    }

    @Test
    public void invalidLengthAndMissingPairAreRejected() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 1), "valid");
        File draft = new File(repository.getDirectoryForTests(), key + ".draft");
        try (FileOutputStream output = new FileOutputStream(draft, true)) {
            output.write('x');
        }
        assertNull(repository.load(key, null));

        draft.delete();
        assertNull(repository.load(key, null));
    }

    @Test
    public void sameLengthContentMismatchIsRejected() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 1), "first");
        File draft = new File(repository.getDirectoryForTests(), key + ".draft");
        try (FileOutputStream output = new FileOutputStream(draft, false)) {
            output.write("other".getBytes(StandardCharsets.UTF_8));
        }

        assertNull(repository.load(key, null));
    }

    @Test
    public void staleActivePointerIsCleared() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 1), "draft");
        new File(repository.getDirectoryForTests(), key + ".draft").delete();

        assertNull(repository.loadActive());
        assertNull(ApplicationProvider.<Context>getApplicationContext()
                .getSharedPreferences("editor_recovery", Context.MODE_PRIVATE)
                .getString("active_key", null));
    }

    @Test
    public void untitledDraftMigratesToUriAndDeletesOldPair() throws Exception {
        String oldKey = RecoveryKeys.forUntitledDocument();
        String uri = "content://provider/document/notes.txt";
        repository.write(metadata(oldKey, null, 9), "draft");

        RecoveryDraft migrated = repository.migrate(oldKey, uri, "notes.txt");

        assertNotNull(migrated);
        assertNotEquals(oldKey, migrated.metadata.recoveryKey);
        assertEquals(uri, migrated.metadata.documentUri);
        assertNull(repository.load(oldKey, null));
        assertNotNull(repository.load(RecoveryKeys.forDocumentUri(uri), uri));
    }

    @Test
    public void deleteRemovesPairAndActivePointer() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 1), "draft");
        repository.delete(key);

        assertNull(repository.load(key, null));
        assertFalse(new File(repository.getDirectoryForTests(), key + ".draft").exists());
        assertFalse(new File(repository.getDirectoryForTests(), key + ".json").exists());
    }

    private RecoveryMetadata metadata(String key, String uri, long generation) {
        return new RecoveryMetadata(
                key, uri, "notes.txt", uri == null, "UTF-8", false,
                12L, null, "fingerprint", 0, 0, 2, 4, generation
        );
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
