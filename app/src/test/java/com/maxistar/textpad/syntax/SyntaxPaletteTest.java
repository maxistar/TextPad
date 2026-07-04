package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SyntaxPaletteTest {
    @Test
    void choosesPaletteFromBackgroundLuminance() {
        assertSame(SyntaxPalette.light(), SyntaxPalette.forBackground(0xffffffff));
        assertSame(SyntaxPalette.dark(), SyntaxPalette.forBackground(0xff000000));
    }
}
