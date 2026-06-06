package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownSyntaxTokenizerTest {
    private final MarkdownSyntaxTokenizer tokenizer = new MarkdownSyntaxTokenizer();

    @Test
    void tokenizesCoreMarkdownStructures() throws Exception {
        String source = "# Heading\n**bold** [link](https://example.com) `code`\n```\nblock\n```";
        SyntaxTokenizationResult result = tokenizer.tokenize(source, 100);

        TokenizerAssertions.assertValid(source, result.getTokens());
        assertTrue(has(result, SyntaxTokenType.HEADING));
        assertTrue(has(result, SyntaxTokenType.EMPHASIS));
        assertTrue(has(result, SyntaxTokenType.LINK));
        assertTrue(has(result, SyntaxTokenType.CODE));
    }

    @Test
    void acceptsIncompleteMultilineConstructs() throws Exception {
        String source = "# Heading\n```\nunfinished\n*also unfinished";
        SyntaxTokenizationResult result = tokenizer.tokenize(source, 100);
        TokenizerAssertions.assertValid(source, result.getTokens());
        assertTrue(has(result, SyntaxTokenType.CODE));
    }

    private static boolean has(SyntaxTokenizationResult result, SyntaxTokenType type) {
        return result.getTokens().stream().anyMatch(token -> token.getType() == type);
    }
}
