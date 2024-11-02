package com.maxistar.textpad.utils;

import com.maxistar.textpad.TPStrings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Supplier;

public class TextConverterTest {

    @org.junit.jupiter.api.Test
    public void getInstance() {
        TextConverter converter = TextConverter.getInstance();
        assertNotNull((Object) "Should return instance", (Supplier<String>) converter);
    }

    @org.junit.jupiter.api.Test
    public void applyDefaultEndings() {
        TextConverter converter = TextConverter.getInstance();
        String result = converter.applyEndings("\n\r\n", TPStrings.DEFAULT);
        assertEquals("\n\r\n", result);
    }

    @org.junit.jupiter.api.Test
    public void applyWindowsEndings() {
        TextConverter converter = TextConverter.getInstance();
        String result = converter.applyEndings("\n\n", TextConverter.WINDOWS);
        assertEquals("\r\n\r\n", result);
    }

    @org.junit.jupiter.api.Test
    public void applyUnixEndings() {
        TextConverter converter = TextConverter.getInstance();
        String result = converter.applyEndings("\r\n\r\n", TextConverter.UNIX);
        assertEquals("\n\n", result);
    }

    @Test
    public void applyMacEndings() {
        TextConverter converter = TextConverter.getInstance();
        String result = converter.applyEndings("\r\n\r\n", TextConverter.MACOS);
        assertEquals("\r\r", result);
    }
}