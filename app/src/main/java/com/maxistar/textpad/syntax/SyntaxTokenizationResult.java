package com.maxistar.textpad.syntax;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public final class SyntaxTokenizationResult {
    private final List<SyntaxToken> tokens;
    private final boolean tokenLimitExceeded;

    private SyntaxTokenizationResult(List<SyntaxToken> tokens, boolean tokenLimitExceeded) {
        this.tokens = Collections.unmodifiableList(tokens);
        this.tokenLimitExceeded = tokenLimitExceeded;
    }

    public static SyntaxTokenizationResult success(List<SyntaxToken> tokens) {
        return new SyntaxTokenizationResult(new ArrayList<>(tokens), false);
    }

    public static SyntaxTokenizationResult limitExceeded() {
        return new SyntaxTokenizationResult(Collections.emptyList(), true);
    }

    public List<SyntaxToken> getTokens() {
        return tokens;
    }

    public boolean isTokenLimitExceeded() {
        return tokenLimitExceeded;
    }
}
