package com.maxistar.textpad.syntax;

import java.util.Objects;

public final class SyntaxToken {
    private final int start;
    private final int end;
    private final SyntaxTokenType type;

    public SyntaxToken(int start, int end, SyntaxTokenType type) {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid token range");
        }
        this.start = start;
        this.end = end;
        this.type = Objects.requireNonNull(type);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public SyntaxTokenType getType() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SyntaxToken)) {
            return false;
        }
        SyntaxToken token = (SyntaxToken) other;
        return start == token.start && end == token.end && type == token.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, type);
    }

    @Override
    public String toString() {
        return type + "[" + start + "," + end + ")";
    }
}
