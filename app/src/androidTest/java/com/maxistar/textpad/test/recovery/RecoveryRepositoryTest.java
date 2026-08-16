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
        File generation = publishedGeneration(key);
        assertTrue(new File(generation, "draft.draft").isFile());
        assertTrue(new File(generation, "metadata.json").isFile());
    }

    @Test
    public void invalidLengthAndMissingPairAreRejected() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 1), "valid");
        File draft = new File(publishedGeneration(key), "draft.draft");
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
        File draft = new File(publishedGeneration(key), "draft.draft");
        try (FileOutputStream output = new FileOutputStream(draft, false)) {
            output.write("other".getBytes(StandardCharsets.UTF_8));
        }

        assertNull(repository.load(key, null));
    }

    @Test
    public void staleActivePointerIsCleared() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 1), "draft");
        deleteRecursively(publishedGeneration(key));

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
        assertFalse(new File(repository.getDirectoryForTests(), key + ".current").exists());
        assertFalse(new File(repository.getDirectoryForTests(), key + ".generations").exists());
    }

    @Test
    public void incompleteUnpublishedGenerationDoesNotReplaceCurrentDraft() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        repository.write(metadata(key, null, 1), "previous");
        File generations = new File(repository.getDirectoryForTests(), key + ".generations");
        File incomplete = new File(generations, "generation-interrupted.tmp");
        assertTrue(incomplete.mkdir());
        try (FileOutputStream output = new FileOutputStream(new File(incomplete, "draft.draft"))) {
            output.write("newer".getBytes(StandardCharsets.UTF_8));
        }

        repository.cleanupIncompleteArtifacts();

        RecoveryDraft draft = repository.loadActive();
        assertNotNull(draft);
        assertEquals("previous", draft.text);
        assertFalse(incomplete.exists());
    }

    @Test
    public void cleanupRestoresLegacyBackupBeforeLoading() throws Exception {
        String key = RecoveryKeys.forUntitledDocument();
        RecoveryMetadata published = metadata(key, null, 1).published(
                "valid".getBytes(StandardCharsets.UTF_8).length,
                sha256("valid".getBytes(StandardCharsets.UTF_8)),
                123L
        );
        File directory = repository.getDirectoryForTests();
        assertTrue(directory.mkdirs());
        write(new File(directory, key + ".draft"), "broken");
        write(new File(directory, key + ".draft.bak"), "valid");
        write(new File(directory, key + ".json"), published.toJson().toString());

        repository.cleanupIncompleteArtifacts();

        RecoveryDraft draft = repository.load(key, null);
        assertNotNull(draft);
        assertEquals("valid", draft.text);
        assertFalse(new File(directory, key + ".draft.bak").exists());
    }

    private RecoveryMetadata metadata(String key, String uri, long generation) {
        return new RecoveryMetadata(
                key, uri, "notes.txt", uri == null, "UTF-8", false,
                12L, null, "fingerprint", 0, 0, 2, 4, generation
        );
    }

    private File publishedGeneration(String key) {
        File generations = new File(repository.getDirectoryForTests(), key + ".generations");
        File[] children = generations.listFiles(file -> file.isDirectory() && !file.getName().endsWith(".tmp"));
        assertNotNull(children);
        assertEquals(1, children.length);
        return children[0];
    }

    private static void write(File file, String value) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String sha256(byte[] value) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        StringBuilder result = new StringBuilder();
        for (byte item : digest.digest(value)) {
            result.append(String.format(java.util.Locale.ROOT, "%02x", item & 0xff));
        }
        return result.toString();
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
