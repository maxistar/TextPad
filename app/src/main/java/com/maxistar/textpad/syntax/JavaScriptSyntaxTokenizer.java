package com.maxistar.textpad.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JavaScriptSyntaxTokenizer implements SyntaxTokenizer {
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "break", "case", "catch", "class", "const", "continue", "debugger",
            "default", "delete", "do", "else", "export", "extends", "finally",
            "for", "function", "if", "import", "in", "instanceof", "let", "new",
            "return", "static", "super", "switch", "this", "throw", "try", "typeof",
            "var", "void", "while", "with", "yield", "async", "await"
    ));
    private static final Set<String> LITERALS = new HashSet<>(Arrays.asList(
            "true", "false", "null", "undefined", "NaN", "Infinity"
    ));

    @Override
    public SyntaxTokenizationResult tokenize(String text, int tokenLimit)
            throws InterruptedException {
        List<SyntaxToken> tokens = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            TokenizerSupport.checkInterrupted(index);
            char current = text.charAt(index);
            if (current == '/' && index + 1 < text.length()
                    && text.charAt(index + 1) == '/') {
                int end = text.indexOf('\n', index + 2);
                end = end < 0 ? text.length() : end;
                if (!add(tokens, index, end, SyntaxTokenType.COMMENT, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if (current == '/' && index + 1 < text.length()
                    && text.charAt(index + 1) == '*') {
                int end = text.indexOf("*/", index + 2);
                end = end < 0 ? text.length() : end + 2;
                if (!add(tokens, index, end, SyntaxTokenType.COMMENT, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if (current == '\'' || current == '"' || current == '`') {
                int end = TokenizerSupport.scanQuoted(text, index, current);
                if (!add(tokens, index, end, SyntaxTokenType.STRING, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if (current == '/' && looksLikeRegexStart(text, index)) {
                index = scanRegexLiteral(text, index);
            } else if (Character.isDigit(current)) {
                int end = TokenizerSupport.scanNumber(text, index);
                if (!add(tokens, index, end, SyntaxTokenType.NUMBER, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if (Character.isJavaIdentifierStart(current) || current == '$') {
                int end = index + 1;
                while (end < text.length()
                        && (Character.isJavaIdentifierPart(text.charAt(end))
                        || text.charAt(end) == '$')) {
                    end++;
                }
                String word = text.substring(index, end);
                SyntaxTokenType type = KEYWORDS.contains(word) ? SyntaxTokenType.KEYWORD
                        : LITERALS.contains(word) ? SyntaxTokenType.LITERAL
                        : SyntaxTokenType.IDENTIFIER;
                if (!add(tokens, index, end, type, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
            } else if ("{}[]();,.:=+-*%!<>?&|".indexOf(current) >= 0) {
                if (!add(tokens, index, index + 1, SyntaxTokenType.PUNCTUATION, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index++;
            } else {
                index++;
            }
        }
        return SyntaxTokenizationResult.success(tokens);
    }

    private static boolean looksLikeRegexStart(String text, int slashIndex) {
        int previous = slashIndex - 1;
        while (previous >= 0 && Character.isWhitespace(text.charAt(previous))) {
            previous--;
        }
        if (previous < 0) {
            return true;
        }
        if ("=([{,:;!&|?+-*%<>".indexOf(text.charAt(previous)) >= 0) {
            return true;
        }
        if (!Character.isJavaIdentifierPart(text.charAt(previous))) {
            return false;
        }
        int wordStart = previous;
        while (wordStart > 0 && Character.isJavaIdentifierPart(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        String previousWord = text.substring(wordStart, previous + 1);
        return "return".equals(previousWord)
                || "throw".equals(previousWord)
                || "case".equals(previousWord)
                || "delete".equals(previousWord)
                || "typeof".equals(previousWord)
                || "void".equals(previousWord)
                || "yield".equals(previousWord)
                || "await".equals(previousWord);
    }

    private static int scanRegexLiteral(String text, int start) throws InterruptedException {
        int index = start + 1;
        boolean escaped = false;
        boolean characterClass = false;
        while (index < text.length()) {
            TokenizerSupport.checkInterrupted(index);
            char current = text.charAt(index++);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '[') {
                characterClass = true;
            } else if (current == ']') {
                characterClass = false;
            } else if (current == '/' && !characterClass) {
                while (index < text.length()
                        && Character.isJavaIdentifierPart(text.charAt(index))) {
                    index++;
                }
                break;
            } else if (current == '\n' || current == '\r') {
                break;
            }
        }
        return index;
    }

    private static boolean add(
            List<SyntaxToken> tokens,
            int start,
            int end,
            SyntaxTokenType type,
            int tokenLimit
    ) {
        return TokenizerSupport.add(tokens, start, end, type, tokenLimit);
    }
}
