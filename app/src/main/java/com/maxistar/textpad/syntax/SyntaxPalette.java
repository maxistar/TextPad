package com.maxistar.textpad.syntax;

import java.util.EnumMap;
import java.util.Map;

public final class SyntaxPalette {
    private static final SyntaxPalette LIGHT = new SyntaxPalette(
            0xff6a737d, 0xff0b6e4f, 0xff9c3d10, 0xff7b2cbf, 0xff005cc5,
            0xff8a2c0d, 0xff5b3cc4, 0xff7b2cbf, 0xff005cc5, 0xff8a2c0d,
            0xff6f42c1, 0xff24292e
    );
    private static final SyntaxPalette DARK = new SyntaxPalette(
            0xff8b949e, 0xff7ee787, 0xffffa657, 0xffd2a8ff, 0xff79c0ff,
            0xffffa198, 0xffa5d6ff, 0xffd2a8ff, 0xff79c0ff, 0xffffa198,
            0xffd2a8ff, 0xffc9d1d9
    );

    private final Map<SyntaxTokenType, Integer> colors =
            new EnumMap<>(SyntaxTokenType.class);

    private SyntaxPalette(
            int comment,
            int string,
            int number,
            int keyword,
            int literal,
            int property,
            int punctuation,
            int heading,
            int link,
            int emphasis,
            int code,
            int identifier
    ) {
        colors.put(SyntaxTokenType.COMMENT, comment);
        colors.put(SyntaxTokenType.STRING, string);
        colors.put(SyntaxTokenType.NUMBER, number);
        colors.put(SyntaxTokenType.KEYWORD, keyword);
        colors.put(SyntaxTokenType.LITERAL, literal);
        colors.put(SyntaxTokenType.PROPERTY, property);
        colors.put(SyntaxTokenType.PUNCTUATION, punctuation);
        colors.put(SyntaxTokenType.HEADING, heading);
        colors.put(SyntaxTokenType.LINK, link);
        colors.put(SyntaxTokenType.EMPHASIS, emphasis);
        colors.put(SyntaxTokenType.CODE, code);
        colors.put(SyntaxTokenType.IDENTIFIER, identifier);
    }

    public static SyntaxPalette light() {
        return LIGHT;
    }

    public static SyntaxPalette dark() {
        return DARK;
    }

    public static SyntaxPalette forBackground(int color) {
        double red = linear((color >> 16) & 0xff);
        double green = linear((color >> 8) & 0xff);
        double blue = linear(color & 0xff);
        double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        return luminance > 0.179 ? LIGHT : DARK;
    }

    public int colorFor(SyntaxTokenType type) {
        Integer color = colors.get(type);
        return color == null ? colors.get(SyntaxTokenType.IDENTIFIER) : color;
    }

    private static double linear(int component) {
        double value = component / 255.0;
        return value <= 0.03928
                ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
