package com.maxistar.textpad.recovery;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

public class RecoveryContentValidatorTest {
    @Test
    public void acceptsEmptyAndUnicodeContent() throws Exception {
        for (String text : new String[]{"", "Привет 世界 🌍\n"}) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            assertTrue(RecoveryContentValidator.matches(new ByteArrayInputStream(bytes),
                    bytes.length, sha256(bytes)));
        }
    }

    @Test
    public void rejectsTruncationExcessAndSameLengthCorruption() throws Exception {
        byte[] bytes = "valid".getBytes(StandardCharsets.UTF_8);
        String hash = sha256(bytes);
        assertFalse(RecoveryContentValidator.matches(new ByteArrayInputStream(bytes), 6, hash));
        assertFalse(RecoveryContentValidator.matches(new ByteArrayInputStream(bytes), 4, hash));
        assertFalse(RecoveryContentValidator.matches(new ByteArrayInputStream(
                "other".getBytes(StandardCharsets.UTF_8)), 5, hash));
        assertFalse(RecoveryContentValidator.matches(new ByteArrayInputStream(bytes), -1, hash));
        assertFalse(RecoveryContentValidator.matches(new ByteArrayInputStream(bytes), 5, null));
    }

    @Test
    public void streamsLargeContentWithBoundedReadRequests() throws Exception {
        int length = 8 * 1024 * 1024;
        MessageDigest reference = MessageDigest.getInstance("SHA-256");
        byte[] block = new byte[1024];
        for (int remaining = length; remaining > 0; remaining -= block.length) {
            reference.update(block);
        }
        final int[] maxRequest = {0};
        InputStream generated = new InputStream() {
            private int remaining = length;

            @Override
            public int read() {
                throw new AssertionError("Expected bulk reads");
            }

            @Override
            public int read(byte[] bytes, int offset, int count) {
                maxRequest[0] = Math.max(maxRequest[0], count);
                if (remaining == 0) {
                    return -1;
                }
                int size = Math.min(count, remaining);
                java.util.Arrays.fill(bytes, offset, offset + size, (byte) 0);
                remaining -= size;
                return size;
            }
        };
        assertTrue(RecoveryContentValidator.matches(generated, length, hex(reference.digest())));
        assertTrue(maxRequest[0] <= 8192);
    }

    @Test
    public void propagatesReadFailure() {
        InputStream failed = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("expected read failure");
            }
        };
        assertThrows(IOException.class, () -> RecoveryContentValidator.matches(failed, 1, "hash"));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        }
        return result.toString();
    }
}
