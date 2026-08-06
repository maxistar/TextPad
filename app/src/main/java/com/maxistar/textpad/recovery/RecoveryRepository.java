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
import java.util.Locale;

public class RecoveryRepository {
    private static final String DIRECTORY = "recovery";
    private static final String PREFERENCES = "editor_recovery";
    private static final String ACTIVE_KEY = "active_key";

    private final File directory;
    private final SharedPreferences preferences;

    public RecoveryRepository(Context context) {
        Context applicationContext = context.getApplicationContext();
        directory = new File(applicationContext.getFilesDir(), DIRECTORY);
        preferences = applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public synchronized RecoveryMetadata write(RecoveryMetadata metadata, String text) throws Exception {
        requireValidKey(metadata.recoveryKey);
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create recovery directory");
        }

        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        RecoveryMetadata published = metadata.published(
                content.length,
                sha256(content),
                java.lang.System.currentTimeMillis()
        );
        writeAtomic(draftFile(metadata.recoveryKey), content);
        writeAtomic(metadataFile(metadata.recoveryKey), published.toJson().toString().getBytes(StandardCharsets.UTF_8));
        setActiveKey(metadata.recoveryKey);
        return published;
    }

    public synchronized RecoveryDraft load(String recoveryKey, String expectedDocumentUri) {
        if (!RecoveryKeys.isValid(recoveryKey)) {
            return null;
        }
        File draft = draftFile(recoveryKey);
        File sidecar = metadataFile(recoveryKey);
        if (!draft.isFile() || !sidecar.isFile()) {
            return null;
        }
        try {
            RecoveryMetadata metadata = RecoveryMetadata.fromJson(
                    new JSONObject(new String(readFully(sidecar), StandardCharsets.UTF_8))
            );
            if (!recoveryKey.equals(metadata.recoveryKey)) {
                return null;
            }
            if (expectedDocumentUri != null && !expectedDocumentUri.equals(metadata.documentUri)) {
                return null;
            }
            byte[] content = readFully(draft);
            if (content.length != metadata.draftSize) {
                return null;
            }
            if (!sha256(content).equals(metadata.draftContentSha256)) {
                return null;
            }
            return new RecoveryDraft(metadata, new String(content, StandardCharsets.UTF_8));
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
        new AtomicFile(draftFile(recoveryKey)).delete();
        new AtomicFile(metadataFile(recoveryKey)).delete();
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
            if (name.endsWith(".bak") || name.endsWith(".new")) {
                String baseName = name.substring(0, name.length() - 4);
                File base = new File(directory, baseName);
                if (base.exists()) {
                    file.delete();
                }
            }
        }
    }

    public File getDirectoryForTests() {
        return directory;
    }

    private void setActiveKey(String recoveryKey) {
        preferences.edit().putString(ACTIVE_KEY, recoveryKey).commit();
    }

    private void clearActiveKey(String recoveryKey) {
        if (recoveryKey != null && recoveryKey.equals(preferences.getString(ACTIVE_KEY, null))) {
            preferences.edit().remove(ACTIVE_KEY).commit();
        }
    }

    private File draftFile(String key) {
        return new File(directory, key + ".draft");
    }

    private File metadataFile(String key) {
        return new File(directory, key + ".json");
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
            atomicFile.finishWrite(stream);
        } catch (IOException error) {
            atomicFile.failWrite(stream);
            throw error;
        }
    }

    private static byte[] readFully(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(file.length(), 1024 * 1024))) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value);
            StringBuilder result = new StringBuilder();
            for (byte item : hash) {
                result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
