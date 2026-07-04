package com.maxistar.textpad.syntax;

import java.util.List;

final class TokenizerSupport {
    private TokenizerSupport() {
    }

    static boolean add(
            List<SyntaxToken> tokens,
            int start,
            int end,
            SyntaxTokenType type,
            int tokenLimit
    ) {
        if (start >= end) {
            return true;
        }
        if (tokens.size() >= tokenLimit) {
            return false;
        }
        tokens.add(new SyntaxToken(start, end, type));
        return true;
    }

    static int scanQuoted(String text, int start, char quote) throws InterruptedException {
        int index = start + 1;
        boolean escaped = false;
        while (index < text.length()) {
            checkInterrupted(index);
            char current = text.charAt(index++);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == quote) {
                break;
            }
        }
        return index;
    }

    static int scanNumber(String text, int start) {
        int index = start;
        if (index < text.length() && (text.charAt(index) == '-' || text.charAt(index) == '+')) {
            index++;
        }
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        if (index < text.length() && text.charAt(index) == '.') {
            index++;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
        }
        if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
            index++;
            if (index < text.length() && (text.charAt(index) == '-' || text.charAt(index) == '+')) {
                index++;
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
        }
        return index;
    }

    static void checkInterrupted(int index) throws InterruptedException {
        if ((index & 255) == 0 && Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Syntax tokenization interrupted");
        }
    }
}
