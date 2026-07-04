package com.maxistar.textpad.syntax;

public final class SyntaxGeneration {
    private long current;

    public synchronized long next() {
        return ++current;
    }

    public synchronized long get() {
        return current;
    }

    public synchronized boolean isCurrent(long generation) {
        return current == generation;
    }
}
