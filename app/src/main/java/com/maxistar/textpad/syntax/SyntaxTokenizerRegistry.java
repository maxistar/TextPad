package com.maxistar.textpad.syntax;

import java.util.EnumMap;
import java.util.Map;

public final class SyntaxTokenizerRegistry {
    private final Map<LanguageMode, SyntaxTokenizer> tokenizers =
            new EnumMap<>(LanguageMode.class);

    public SyntaxTokenizerRegistry() {
        tokenizers.put(LanguageMode.PLAIN_TEXT, new PlainTextTokenizer());
        tokenizers.put(LanguageMode.JSON, new JsonSyntaxTokenizer());
        tokenizers.put(LanguageMode.MARKDOWN, new MarkdownSyntaxTokenizer());
        tokenizers.put(LanguageMode.JAVASCRIPT, new JavaScriptSyntaxTokenizer());
    }

    public SyntaxTokenizer get(LanguageMode mode) {
        SyntaxTokenizer tokenizer = tokenizers.get(mode);
        return tokenizer == null ? tokenizers.get(LanguageMode.PLAIN_TEXT) : tokenizer;
    }

    void register(LanguageMode mode, SyntaxTokenizer tokenizer) {
        tokenizers.put(mode, tokenizer);
    }
}
