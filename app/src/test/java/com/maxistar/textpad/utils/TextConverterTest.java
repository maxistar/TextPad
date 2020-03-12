package com.maxistar.textpad.utils;

import com.maxistar.textpad.TPStrings;

import org.junit.Test;

import static org.junit.Assert.*;

public class TextConverterTest {

    @Test
    public void getInstance() {
        TextConverter converter = TextConverter.getInstance();
        assertTrue("Should return instance",converter != null);
    }

    @Test
    public void applyDefaultEndings() {
        TextConverter converter = TextConverter.getInstance();
        String result = converter.applyEndings("\n\r\n", TPStrings.DEFAULT);
        assertEquals("\n\r\n", result);
    }

    @Test
    public void applyWindowsEndings() {
        TextConverter converter = TextConverter.getInstance();
        String result = converter.applyEndings("\n\n", TextConverter.WINDOWS);
        assertEquals("\r\n\r\n", result);
    }

    @Test
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