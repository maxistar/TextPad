package com.maxistar.textpad.syntax;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownSyntaxTokenizer implements SyntaxTokenizer {
    @Override
    public SyntaxTokenizationResult tokenize(String text, int tokenLimit)
            throws InterruptedException {
        List<SyntaxToken> tokens = new ArrayList<>();
        int index = 0;
        boolean lineStart = true;
        while (index < text.length()) {
            TokenizerSupport.checkInterrupted(index);
            if (text.startsWith("```", index)) {
                int end = text.indexOf("```", index + 3);
                end = end < 0 ? text.length() : end + 3;
                if (!TokenizerSupport.add(tokens, index, end, SyntaxTokenType.CODE, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                lineStart = end > 0 && text.charAt(end - 1) == '\n';
                index = end;
            } else if (lineStart && text.charAt(index) == '#') {
                int end = index;
                while (end < text.length() && text.charAt(end) == '#') {
                    end++;
                }
                if (end < text.length() && text.charAt(end) == ' ') {
                    end = lineEnd(text, end);
                    if (!TokenizerSupport.add(
                            tokens, index, end, SyntaxTokenType.HEADING, tokenLimit)) {
                        return SyntaxTokenizationResult.limitExceeded();
                    }
                    index = end;
                } else {
                    index++;
                }
                lineStart = false;
            } else if (text.charAt(index) == '`') {
                int end = text.indexOf('`', index + 1);
                end = end < 0 ? text.length() : end + 1;
                if (!TokenizerSupport.add(tokens, index, end, SyntaxTokenType.CODE, tokenLimit)) {
                    return SyntaxTokenizationResult.limitExceeded();
                }
                index = end;
                lineStart = false;
            } else if (text.charAt(index) == '[') {
                int labelEnd = text.indexOf(']', index + 1);
                int linkEnd = labelEnd >= 0 && labelEnd + 1 < text.length()
                        && text.charAt(labelEnd + 1) == '('
                        ? text.indexOf(')', labelEnd + 2) : -1;
                if (linkEnd >= 0) {
                    if (!TokenizerSupport.add(
                            tokens, index, linkEnd + 1, SyntaxTokenType.LINK, tokenLimit)) {
                        return SyntaxTokenizationResult.limitExceeded();
                    }
                    index = linkEnd + 1;
                    lineStart = false;
                } else {
                    index++;
                }
            } else if (text.charAt(index) == '*' || text.charAt(index) == '_') {
                char marker = text.charAt(index);
                int markerLength = index + 1 < text.length()
                        && text.charAt(index + 1) == marker ? 2 : 1;
                String closing = markerLength == 2
                        ? new String(new char[]{marker, marker}) : String.valueOf(marker);
                int end = text.indexOf(closing, index + markerLength);
                if (end >= 0) {
                    end += markerLength;
                    if (!TokenizerSupport.add(
                            tokens, index, end, SyntaxTokenType.EMPHASIS, tokenLimit)) {
                        return SyntaxTokenizationResult.limitExceeded();
                    }
                    index = end;
                    lineStart = false;
                } else {
                    index++;
                }
            } else {
                lineStart = text.charAt(index) == '\n';
                index++;
            }
        }
        return SyntaxTokenizationResult.success(tokens);
    }

    private static int lineEnd(String text, int start) {
        int end = text.indexOf('\n', start);
        return end < 0 ? text.length() : end;
    }
}
