package com.maxistar.textpad.utils;

import java.security.MessageDigest;
import java.util.Locale;

/** Pure byte-level classification used before replacing an open document. */
public final class DocumentSaveValidator {
    public enum Outcome {
        BASELINE_MATCH,
        INTENDED_CONTENT_MATCH,
        CONFLICT,
        UNREADABLE
    }

    private DocumentSaveValidator() {
    }

    public static Outcome classify(byte[] currentBytes, String baselineSha256, byte[] intendedBytes) {
        if (currentBytes == null || baselineSha256 == null || intendedBytes == null) {
            return Outcome.UNREADABLE;
        }
        String currentSha256 = sha256(currentBytes);
        if (baselineSha256.equals(currentSha256)) {
            return Outcome.BASELINE_MATCH;
        }
        if (sha256(intendedBytes).equals(currentSha256)) {
            return Outcome.INTENDED_CONTENT_MATCH;
        }
        return Outcome.CONFLICT;
    }

    public static String sha256(byte[] value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value);
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
