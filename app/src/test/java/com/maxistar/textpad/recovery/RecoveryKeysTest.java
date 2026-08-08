package com.maxistar.textpad.recovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecoveryKeysTest {
    @Test
    public void uriKeyIsStableSha256() {
        String uri = "content://provider/document/notes.txt";
        assertEquals(RecoveryKeys.forDocumentUri(uri), RecoveryKeys.forDocumentUri(uri));
        assertTrue(RecoveryKeys.forDocumentUri(uri).matches("uri-[0-9a-f]{64}"));
    }

    @Test
    public void differentUrisWithSameDisplayNameHaveDifferentKeys() {
        assertNotEquals(
                RecoveryKeys.forDocumentUri("content://one/folder/notes.txt"),
                RecoveryKeys.forDocumentUri("content://two/folder/notes.txt")
        );
    }

    @Test
    public void untitledKeysAreUniqueAndValid() {
        String first = RecoveryKeys.forUntitledDocument();
        String second = RecoveryKeys.forUntitledDocument();
        assertNotEquals(first, second);
        assertTrue(RecoveryKeys.isValid(first));
        assertTrue(RecoveryKeys.isValid(second));
    }
}
