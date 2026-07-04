package com.maxistar.textpad.syntax;

import java.util.Locale;

public final class LanguageDetector {
    public LanguageMode detect(String displayName) {
        if (displayName == null) {
            return LanguageMode.PLAIN_TEXT;
        }
        String name = displayName.trim().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return LanguageMode.JSON;
        }
        if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return LanguageMode.MARKDOWN;
        }
        if (name.endsWith(".js") || name.endsWith(".mjs") || name.endsWith(".cjs")) {
            return LanguageMode.JAVASCRIPT;
        }
        return LanguageMode.PLAIN_TEXT;
    }

    public LanguageMode resolve(LanguageMode selectedMode, String displayName) {
        return selectedMode == null || selectedMode == LanguageMode.AUTO
                ? detect(displayName) : selectedMode;
    }
}
