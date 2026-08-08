package com.maxistar.textpad.recovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

public final class RecoveryKeys {
    private static final String URI_PREFIX = "uri-";
    private static final String NEW_PREFIX = "new-";

    private RecoveryKeys() {
    }

    public static String forDocumentUri(String documentUri) {
        if (documentUri == null || documentUri.isEmpty()) {
            throw new IllegalArgumentException("documentUri must not be empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(documentUri.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(URI_PREFIX);
            for (byte value : hash) {
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public static String forUntitledDocument() {
        return NEW_PREFIX + UUID.randomUUID();
    }

    public static boolean isValid(String recoveryKey) {
        return recoveryKey != null && (
                recoveryKey.matches("uri-[0-9a-f]{64}")
                        || recoveryKey.matches("new-[0-9a-fA-F-]{36}")
        );
    }
}
