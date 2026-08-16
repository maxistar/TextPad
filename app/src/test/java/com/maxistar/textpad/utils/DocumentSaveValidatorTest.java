package com.maxistar.textpad.utils;

import static com.maxistar.textpad.utils.DocumentSaveValidator.Outcome.BASELINE_MATCH;
import static com.maxistar.textpad.utils.DocumentSaveValidator.Outcome.CONFLICT;
import static com.maxistar.textpad.utils.DocumentSaveValidator.Outcome.INTENDED_CONTENT_MATCH;
import static com.maxistar.textpad.utils.DocumentSaveValidator.Outcome.UNREADABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class DocumentSaveValidatorTest {
    @Test
    public void unchangedStoredBytesMatchBaseline() {
        byte[] original = bytes("original");
        assertEquals(BASELINE_MATCH, DocumentSaveValidator.classify(
                original,
                DocumentSaveValidator.sha256(original),
                bytes("local edit")
        ));
    }

    @Test
    public void externallyWrittenIntendedBytesAreEquivalent() {
        byte[] original = bytes("original");
        byte[] intended = bytes("local edit");
        assertEquals(INTENDED_CONTENT_MATCH, DocumentSaveValidator.classify(
                intended,
                DocumentSaveValidator.sha256(original),
                intended
        ));
    }

    @Test
    public void independentlyChangedVersionsConflict() {
        assertEquals(CONFLICT, DocumentSaveValidator.classify(
                bytes("external edit"),
                DocumentSaveValidator.sha256(bytes("original")),
                bytes("local edit")
        ));
    }

    @Test
    public void missingBaselineOrCurrentBytesAreUnreadable() {
        assertEquals(UNREADABLE, DocumentSaveValidator.classify(bytes("current"), null, bytes("local")));
        assertEquals(UNREADABLE, DocumentSaveValidator.classify(null, "hash", bytes("local")));
    }

    @Test
    public void validationDoesNotRequireProviderMetadata() {
        byte[] original = bytes("same content");
        assertEquals(BASELINE_MATCH, DocumentSaveValidator.classify(
                original,
                DocumentSaveValidator.sha256(original),
                bytes("new content")
        ));
    }

    @Test
    public void bomAndEncodingChangesAreByteChanges() {
        byte[] plain = bytes("note");
        byte[] withBom = new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'n', 'o', 't', 'e'};
        assertNotEquals(DocumentSaveValidator.sha256(plain), DocumentSaveValidator.sha256(withBom));
        assertEquals(CONFLICT, DocumentSaveValidator.classify(
                withBom,
                DocumentSaveValidator.sha256(plain),
                bytes("local edit")
        ));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
