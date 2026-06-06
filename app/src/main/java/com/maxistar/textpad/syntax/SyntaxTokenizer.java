package com.maxistar.textpad.syntax;

public interface SyntaxTokenizer {
    SyntaxTokenizationResult tokenize(String text, int tokenLimit) throws InterruptedException;
}
