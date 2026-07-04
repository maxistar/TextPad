package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaScriptSyntaxTokenizerTest {
    private final JavaScriptSyntaxTokenizer tokenizer = new JavaScriptSyntaxTokenizer();

    @Test
    void tokenizesCoreJavaScriptLexemes() throws Exception {
        String source = "const answer = 42;\n// note\nfunction run(value) { return `x ${value}`; }";
        SyntaxTokenizationResult result = tokenizer.tokenize(source, 100);

        TokenizerAssertions.assertValid(source, result.getTokens());
        assertTrue(has(result, SyntaxTokenType.KEYWORD));
        assertTrue(has(result, SyntaxTokenType.IDENTIFIER));
        assertTrue(has(result, SyntaxTokenType.NUMBER));
        assertTrue(has(result, SyntaxTokenType.COMMENT));
        assertTrue(has(result, SyntaxTokenType.STRING));
    }

    @Test
    void leavesRegexLiteralAndTemplateInterpolationUnparsed() throws Exception {
        String source = "const pattern = /[a-z]+\\/x/gi; "
                + "function find() { return /word+/; } "
                + "const value = `hello ${name}`;";
        SyntaxTokenizationResult result = tokenizer.tokenize(source, 100);

        int regexStart = source.indexOf('/');
        int regexEnd = source.indexOf(';', regexStart);
        assertFalse(result.getTokens().stream().anyMatch(
                token -> token.getStart() >= regexStart && token.getEnd() <= regexEnd));
        int returnedRegexStart = source.indexOf("/word+/");
        int returnedRegexEnd = returnedRegexStart + "/word+/".length();
        assertFalse(result.getTokens().stream().anyMatch(
                token -> token.getStart() >= returnedRegexStart
                        && token.getEnd() <= returnedRegexEnd));

        int interpolation = source.indexOf("${name}");
        assertTrue(result.getTokens().stream().anyMatch(token ->
                token.getType() == SyntaxTokenType.STRING
                        && token.getStart() < interpolation
                        && token.getEnd() > interpolation));
    }

    @Test
    void acceptsUnterminatedStringsAndComments() throws Exception {
        String source = "/* unfinished\nconst value = \"unfinished";
        SyntaxTokenizationResult result = tokenizer.tokenize(source, 100);
        TokenizerAssertions.assertValid(source, result.getTokens());
    }

    private static boolean has(SyntaxTokenizationResult result, SyntaxTokenType type) {
        return result.getTokens().stream().anyMatch(token -> token.getType() == type);
    }
}
