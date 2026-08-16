package com.maxistar.textpad.recovery;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public class RecoveryRepository {
    private static final String DIRECTORY = "recovery";
    private static final String PREFERENCES = "editor_recovery";
    private static final String ACTIVE_KEY = "active_key";
    private static final String GENERATIONS_SUFFIX = ".generations";
    private static final String CURRENT_SUFFIX = ".current";
    private static final String TEMPORARY_SUFFIX = ".tmp";
    private static final String DRAFT_NAME = "draft.draft";
    private static final String METADATA_NAME = "metadata.json";

    private final File directory;
    private final SharedPreferences preferences;

    public RecoveryRepository(Context context) {
        Context applicationContext = context.getApplicationContext();
        directory = new File(applicationContext.getFilesDir(), DIRECTORY);
        preferences = applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public synchronized RecoveryMetadata write(RecoveryMetadata metadata, String text) throws Exception {
        requireValidKey(metadata.recoveryKey);
        ensureDirectory(directory);

        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        RecoveryMetadata published = metadata.published(
                content.length,
                sha256(content),
                java.lang.System.currentTimeMillis()
        );
        String previousGeneration = readCurrentGeneration(metadata.recoveryKey);
        File generations = generationsDirectory(metadata.recoveryKey);
        ensureDirectory(generations);

        String generation = "generation-" + UUID.randomUUID();
        File temporary = new File(generations, generation + TEMPORARY_SUFFIX);
        File complete = new File(generations, generation);
        if (!temporary.mkdir()) {
            throw new IOException("Cannot create recovery generation");
        }
        try {
            writeFile(new File(temporary, DRAFT_NAME), content);
            writeFile(new File(temporary, METADATA_NAME), published.toJson().toString().getBytes(StandardCharsets.UTF_8));
            if (loadPair(metadata.recoveryKey, null, temporary) == null) {
                throw new IOException("Recovery generation validation failed");
            }
            if (!temporary.renameTo(complete)) {
                throw new IOException("Cannot publish recovery generation");
            }
            writeAtomic(currentFile(metadata.recoveryKey), generation.getBytes(StandardCharsets.UTF_8));
            setActiveKey(metadata.recoveryKey);
            deleteLegacyArtifacts(metadata.recoveryKey);
            cleanupGenerations(metadata.recoveryKey, generation, previousGeneration);
            return published;
        } finally {
            if (temporary.exists()) {
                deleteRecursively(temporary);
            }
        }
    }

    public synchronized RecoveryDraft load(String recoveryKey, String expectedDocumentUri) {
        if (!RecoveryKeys.isValid(recoveryKey)) {
            return null;
        }
        try {
            String generation = readCurrentGeneration(recoveryKey);
            if (generation != null) {
                RecoveryDraft draft = loadPair(recoveryKey, expectedDocumentUri,
                        new File(generationsDirectory(recoveryKey), generation));
                if (draft != null) {
                    return draft;
                }
            }
            return loadLegacyPair(recoveryKey, expectedDocumentUri);
        } catch (Exception ignored) {
            return null;
        }
    }

    public synchronized RecoveryDraft loadActive() {
        String key = preferences.getString(ACTIVE_KEY, null);
        RecoveryDraft draft = load(key, null);
        if (key != null && draft == null) {
            clearActiveKey(key);
        }
        return draft;
    }

    public synchronized RecoveryDraft migrate(String oldKey, String documentUri, String displayName) throws Exception {
        RecoveryDraft oldDraft = load(oldKey, null);
        if (oldDraft == null) {
            return null;
        }
        String newKey = RecoveryKeys.forDocumentUri(documentUri);
        RecoveryMetadata migrated = oldDraft.metadata.withIdentity(newKey, documentUri, displayName);
        RecoveryMetadata published = write(migrated, oldDraft.text);
        delete(oldKey);
        return new RecoveryDraft(published, oldDraft.text);
    }

    public synchronized void delete(String recoveryKey) {
        if (!RecoveryKeys.isValid(recoveryKey)) {
            return;
        }
        new AtomicFile(currentFile(recoveryKey)).delete();
        deleteRecursively(generationsDirectory(recoveryKey));
        deleteLegacyArtifacts(recoveryKey);
        clearActiveKey(recoveryKey);
    }

    public synchronized void cleanupIncompleteArtifacts() {
        if (!directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".bak")) {
                restoreBackup(file, new File(directory, name.substring(0, name.length() - 4)));
            } else if (name.endsWith(".new")) {
                file.delete();
            } else if (name.endsWith(GENERATIONS_SUFFIX) && file.isDirectory()) {
                cleanupTemporaryGenerations(file);
            }
        }
    }

    public File getDirectoryForTests() {
        return directory;
    }

    private RecoveryDraft loadLegacyPair(String recoveryKey, String expectedDocumentUri) throws Exception {
        File draft = draftFile(recoveryKey);
        File sidecar = metadataFile(recoveryKey);
        if (!draft.isFile() || !sidecar.isFile()) {
            return null;
        }
        return loadPair(recoveryKey, expectedDocumentUri, draft, sidecar, true);
    }

    private RecoveryDraft loadPair(String recoveryKey, String expectedDocumentUri, File generation) throws Exception {
        return loadPair(recoveryKey, expectedDocumentUri,
                new File(generation, DRAFT_NAME), new File(generation, METADATA_NAME), false);
    }

    private RecoveryDraft loadPair(
            String recoveryKey,
            String expectedDocumentUri,
            File draft,
            File sidecar,
            boolean atomicLegacyFiles
    ) throws Exception {
        if (!draft.isFile() || !sidecar.isFile()) {
            return null;
        }
        byte[] sidecarBytes = atomicLegacyFiles ? readAtomic(sidecar) : readFully(sidecar);
        RecoveryMetadata metadata = RecoveryMetadata.fromJson(
                new JSONObject(new String(sidecarBytes, StandardCharsets.UTF_8))
        );
        if (!recoveryKey.equals(metadata.recoveryKey)) {
            return null;
        }
        if (expectedDocumentUri != null && !expectedDocumentUri.equals(metadata.documentUri)) {
            return null;
        }
        byte[] content = atomicLegacyFiles ? readAtomic(draft) : readFully(draft);
        if (content.length != metadata.draftSize || !sha256(content).equals(metadata.draftContentSha256)) {
            return null;
        }
        return new RecoveryDraft(metadata, new String(content, StandardCharsets.UTF_8));
    }

    private String readCurrentGeneration(String recoveryKey) throws IOException {
        File marker = currentFile(recoveryKey);
        if (!marker.exists() && !new File(marker.getPath() + ".bak").exists()) {
            return null;
        }
        String generation = new String(readAtomic(marker), StandardCharsets.UTF_8).trim();
        return generation.startsWith("generation-") && !generation.contains("/") ? generation : null;
    }

    private void cleanupGenerations(String key, String currentGeneration, String previousGeneration) {
        File[] generations = generationsDirectory(key).listFiles();
        if (generations == null) {
            return;
        }
        for (File generation : generations) {
            String name = generation.getName();
            if (name.endsWith(TEMPORARY_SUFFIX)
                    || (!name.equals(currentGeneration) && !name.equals(previousGeneration))) {
                deleteRecursively(generation);
            }
        }
    }

    private static void cleanupTemporaryGenerations(File generations) {
        File[] children = generations.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.getName().endsWith(TEMPORARY_SUFFIX)) {
                deleteRecursively(child);
            }
        }
    }

    private static void restoreBackup(File backup, File base) {
        if (base.exists() && !base.delete()) {
            return;
        }
        backup.renameTo(base);
    }

    private void deleteLegacyArtifacts(String recoveryKey) {
        new AtomicFile(draftFile(recoveryKey)).delete();
        new AtomicFile(metadataFile(recoveryKey)).delete();
    }

    private void setActiveKey(String recoveryKey) {
        preferences.edit().putString(ACTIVE_KEY, recoveryKey).commit();
    }

    private void clearActiveKey(String recoveryKey) {
        if (recoveryKey != null && recoveryKey.equals(preferences.getString(ACTIVE_KEY, null))) {
            preferences.edit().remove(ACTIVE_KEY).commit();
        }
    }

    private File generationsDirectory(String key) {
        return new File(directory, key + GENERATIONS_SUFFIX);
    }

    private File currentFile(String key) {
        return new File(directory, key + CURRENT_SUFFIX);
    }

    private File draftFile(String key) {
        return new File(directory, key + ".draft");
    }

    private File metadataFile(String key) {
        return new File(directory, key + ".json");
    }

    private static void ensureDirectory(File path) throws IOException {
        if (!path.exists() && !path.mkdirs()) {
            throw new IOException("Cannot create recovery directory");
        }
        if (!path.isDirectory()) {
            throw new IOException("Recovery path is not a directory");
        }
    }

    private static void requireValidKey(String key) {
        if (!RecoveryKeys.isValid(key)) {
            throw new IllegalArgumentException("Invalid recovery key");
        }
    }

    private static void writeAtomic(File file, byte[] content) throws IOException {
        AtomicFile atomicFile = new AtomicFile(file);
        FileOutputStream stream = atomicFile.startWrite();
        try {
            stream.write(content);
            stream.flush();
            stream.getFD().sync();
            atomicFile.finishWrite(stream);
        } catch (IOException error) {
            atomicFile.failWrite(stream);
            throw error;
        }
    }

    private static void writeFile(File file, byte[] content) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content);
            output.flush();
            output.getFD().sync();
        }
    }

    private static byte[] readAtomic(File file) throws IOException {
        try (FileInputStream input = new AtomicFile(file).openRead()) {
            return readFully(input, file.length());
        }
    }

    private static byte[] readFully(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            return readFully(input, file.length());
        }
    }

    private static byte[] readFully(FileInputStream input, long length) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(length, 1024 * 1024))) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
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

    private static String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value);
            StringBuilder result = new StringBuilder();
            for (byte item : hash) {
                result.append(String.format(java.util.Locale.ROOT, "%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
