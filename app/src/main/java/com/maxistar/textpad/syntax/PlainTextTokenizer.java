package com.maxistar.textpad.syntax;

import java.util.Collections;

public final class PlainTextTokenizer implements SyntaxTokenizer {
    @Override
    public SyntaxTokenizationResult tokenize(String text, int tokenLimit) {
        return SyntaxTokenizationResult.success(Collections.emptyList());
    }
}
