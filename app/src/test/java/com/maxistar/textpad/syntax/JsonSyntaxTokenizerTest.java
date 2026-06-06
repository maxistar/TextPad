package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonSyntaxTokenizerTest {
    private final JsonSyntaxTokenizer tokenizer = new JsonSyntaxTokenizer();

    @Test
    void tokenizesPropertiesStringsNumbersLiteralsAndPunctuation() throws Exception {
        String source = "{\"name\":\"a\\\"b\",\"count\":-1.5e2,\"enabled\":true,\"value\":null}";
        SyntaxTokenizationResult result = tokenizer.tokenize(source, 100);

        TokenizerAssertions.assertValid(source, result.getTokens());
        assertTrue(result.getTokens().stream().anyMatch(t -> t.getType() == SyntaxTokenType.PROPERTY));
        assertTrue(result.getTokens().stream().anyMatch(t -> t.getType() == SyntaxTokenType.STRING));
        assertTrue(result.getTokens().stream().anyMatch(t -> t.getType() == SyntaxTokenType.NUMBER));
        assertTrue(result.getTokens().stream().anyMatch(t -> t.getType() == SyntaxTokenType.LITERAL));
    }

    @Test
    void acceptsIncompleteAndUnexpectedInput() throws Exception {
        String source = "{\"name\":\"unfinished\n???";
        SyntaxTokenizationResult result = tokenizer.tokenize(source, 100);
        TokenizerAssertions.assertValid(source, result.getTokens());
    }

    @Test
    void discardsAllTokensAtLimit() throws Exception {
        SyntaxTokenizationResult result = tokenizer.tokenize("[1,2,3]", 2);
        assertTrue(result.isTokenLimitExceeded());
        assertEquals(0, result.getTokens().size());
    }
}
