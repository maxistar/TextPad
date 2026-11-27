package com.maxistar.textpad.recovery;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Validates stored draft bytes without allocating another document-sized buffer. */
public final class RecoveryContentValidator {
    private RecoveryContentValidator() { }

    public static boolean matches(InputStream input, long expectedSize, String expectedSha256)
            throws IOException {
        if (expectedSize < 0 || expectedSha256 == null) {
            return false;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
        byte[] buffer = new byte[8192];
        long size = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count > expectedSize - size) {
                return false;
            }
            size += count;
            digest.update(buffer, 0, count);
        }
        if (size != expectedSize) {
            return false;
        }
        StringBuilder hash = new StringBuilder(64);
        for (byte item : digest.digest()) {
            hash.append(Character.forDigit((item & 0xff) >>> 4, 16));
            hash.append(Character.forDigit(item & 0xf, 16));
        }
        return expectedSha256.equals(hash.toString());
    }
}
