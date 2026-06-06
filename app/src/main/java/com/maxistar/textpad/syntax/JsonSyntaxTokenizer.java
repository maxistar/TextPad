package com.maxistar.textpad.syntax;

import java.util.ArrayList;
import java.util.List;

public final class JsonSyntaxTokenizer implements SyntaxTokenizer {
    @Override
    public SyntaxTokenizationResult tokenize(String text, int tokenLimit)
            throws InterruptedException {
        List<SyntaxToken> tokens = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            TokenizerSupport.checkInterrupted(index);
            char current = text.charAt(index);
            if (current == '"') {
                int end = TokenizerSupport.scanQuoted(text, index, '"');
                int next = skipWhitespace(text, end);
                SyntaxTokenType type = next < text.length() && text.charAt(next) == ':'
                        ? SyntaxTokenType.PROPERTY : SyntaxTokenType.STRING;
                if (!TokenizerSupport.add(tokens, index, end, type, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if (current == '-' || Character.isDigit(current)) {
                int end = TokenizerSupport.scanNumber(text, index);
                if (!TokenizerSupport.add(tokens, index, end, SyntaxTokenType.NUMBER, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if (startsWord(text, index, "true")
                    || startsWord(text, index, "false")
                    || startsWord(text, index, "null")) {
                int end = scanIdentifier(text, index);
                if (!TokenizerSupport.add(tokens, index, end, SyntaxTokenType.LITERAL, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if ("{}[],:".indexOf(current) >= 0) {
                if (!TokenizerSupport.add(
                        tokens, index, index + 1, SyntaxTokenType.PUNCTUATION, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index++;
            } else {
                index++;
            }
        }
        return SyntaxTokenizationResult.success(tokens);
    }

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean startsWord(String text, int start, String word) {
        int end = start + word.length();
        return end <= text.length()
                && text.regionMatches(start, word, 0, word.length())
                && (end == text.length() || !Character.isJavaIdentifierPart(text.charAt(end)));
    }

    private static int scanIdentifier(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isJavaIdentifierPart(text.charAt(index))) {
            index++;
        }
        return index;
    }
}
