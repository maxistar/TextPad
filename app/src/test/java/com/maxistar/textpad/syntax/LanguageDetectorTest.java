package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LanguageDetectorTest {
    private final LanguageDetector detector = new LanguageDetector();

    @Test
    void detectsSupportedExtensionsCaseInsensitively() {
        assertEquals(LanguageMode.JSON, detector.detect("settings.JSON"));
        assertEquals(LanguageMode.MARKDOWN, detector.detect("/notes/readme.MarkDown"));
        assertEquals(LanguageMode.JAVASCRIPT, detector.detect("module.MJS"));
        assertEquals(LanguageMode.JAVASCRIPT, detector.detect("script.cjs"));
    }

    @Test
    void unknownOrMissingNamesUsePlainText() {
        assertEquals(LanguageMode.PLAIN_TEXT, detector.detect("notes.txt"));
        assertEquals(LanguageMode.PLAIN_TEXT, detector.detect(""));
        assertEquals(LanguageMode.PLAIN_TEXT, detector.detect(null));
    }

    @Test
    void manualModeOverridesDetection() {
        assertEquals(LanguageMode.MARKDOWN,
                detector.resolve(LanguageMode.MARKDOWN, "settings.json"));
        assertEquals(LanguageMode.JSON, detector.resolve(LanguageMode.AUTO, "settings.json"));
        assertEquals(LanguageMode.JSON, detector.resolve(null, "settings.json"));
    }
}
