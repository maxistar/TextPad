package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

final class TokenizerAssertions {
    private TokenizerAssertions() {
    }

    static void assertValid(String source, List<SyntaxToken> tokens) {
        int previousEnd = 0;
        for (SyntaxToken token : tokens) {
            assertTrue(token.getStart() >= previousEnd, token.toString());
            assertTrue(token.getEnd() > token.getStart(), token.toString());
            assertTrue(token.getEnd() <= source.length(), token.toString());
            previousEnd = token.getEnd();
        }
    }
}
