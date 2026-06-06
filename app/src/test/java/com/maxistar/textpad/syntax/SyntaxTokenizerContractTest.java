package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SyntaxTokenizerContractTest {
    @Test
    void tokenizerHonorsThreadInterruption() {
        Thread.currentThread().interrupt();
        try {
            String source = new String(new char[1024]).replace('\0', ' ');
            assertThrows(InterruptedException.class,
                    () -> new JsonSyntaxTokenizer().tokenize(source, 100));
        } finally {
            Thread.interrupted();
        }
    }
}
