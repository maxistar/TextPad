package com.maxistar.textpad.syntax;

import android.text.NoCopySpan;
import android.text.style.ForegroundColorSpan;

public final class SyntaxSpan extends ForegroundColorSpan implements NoCopySpan {
    private final long generation;

    public SyntaxSpan(int color, long generation) {
        super(color);
        this.generation = generation;
    }

    public long getGeneration() {
        return generation;
    }
}
