package com.maxistar.textpad.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileEncodingTest {

    private static byte[] bom(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    @Test
    public void detectUtf16LeBom() {
        FileEncoding encoding = FileEncoding.detect(new byte[]{(byte) 0xFF, (byte) 0xFE, 0x41, 0x00});
        assertEquals(FileEncoding.UTF_16LE, encoding.getCharsetName());
        assertEquals(2, encoding.getBom().length);
    }

    @Test
    public void detectUtf16BeBom() {
        FileEncoding encoding = FileEncoding.detect(new byte[]{(byte) 0xFE, (byte) 0xFF, 0x00, 0x41});
        assertEquals(FileEncoding.UTF_16BE, encoding.getCharsetName());
    }

    @Test
    public void detectUtf8Bom() {
        FileEncoding encoding = FileEncoding.detect(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0x41});
        assertEquals(FileEncoding.UTF_8, encoding.getCharsetName());
    }

    @Test
    public void detectUtf32LeBom() {
        FileEncoding encoding = FileEncoding.detect(new byte[]{(byte) 0xFF, (byte) 0xFE, 0, 0, 0x41, 0, 0, 0});
        assertEquals(FileEncoding.UTF_32LE, encoding.getCharsetName());
    }

    @Test
    public void detectUtf32BeBom() {
        FileEncoding encoding = FileEncoding.detect(new byte[]{0, 0, (byte) 0xFE, (byte) 0xFF, 0, 0, 0, 0x41});
        assertEquals(FileEncoding.UTF_32BE, encoding.getCharsetName());
    }

    @Test
    public void detectNoBom() {
        assertNull(FileEncoding.detect("hello".getBytes(StandardCharsets.UTF_8)));
        assertNull(FileEncoding.detect(new byte[0]));
        assertNull(FileEncoding.detect(null));
    }

    @Test
    public void decodeStripsBom() {
        byte[] bytes = bom(new byte[]{(byte) 0xFF, (byte) 0xFE},
                "\u041f\u0440\u0438\u0432\u0435\u0442".getBytes(StandardCharsets.UTF_16LE));
        FileEncoding encoding = FileEncoding.detect(bytes);
        String text = FileEncoding.decode(bytes, encoding, FileEncoding.UTF_8);
        assertEquals("\u041f\u0440\u0438\u0432\u0435\u0442", text);
    }

    @Test
    public void decodeWithoutBomUsesFallback() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        String text = FileEncoding.decode(bytes, null, FileEncoding.UTF_8);
        assertEquals("hello", text);
    }

    @Test
    public void encodeRestoresBom() {
        byte[] bytes = bom(new byte[]{(byte) 0xFF, (byte) 0xFE},
                "\u041f\u0440\u0438\u0432\u0435\u0442".getBytes(StandardCharsets.UTF_16LE));
        FileEncoding encoding = FileEncoding.detect(bytes);
        byte[] encoded = FileEncoding.encode("\u041f\u0440\u0438\u0432\u0435\u0442", encoding, FileEncoding.UTF_8);
        assertArrayEquals(bytes, encoded);
    }

    @Test
    public void encodeRoundTrip() {
        byte[] bytes = bom(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                "test".getBytes(StandardCharsets.UTF_8));
        FileEncoding encoding = FileEncoding.detect(bytes);
        String text = FileEncoding.decode(bytes, encoding, FileEncoding.UTF_8);
        byte[] encoded = FileEncoding.encode(text, encoding, FileEncoding.UTF_8);
        assertArrayEquals(bytes, encoded);
    }

    @Test
    public void fromCharsetReconstructsBomForEachSupportedCharset() {
        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                FileEncoding.fromCharset(FileEncoding.UTF_8, true).getBom());
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFE},
                FileEncoding.fromCharset(FileEncoding.UTF_16LE, true).getBom());
        assertArrayEquals(new byte[]{(byte) 0xFE, (byte) 0xFF},
                FileEncoding.fromCharset(FileEncoding.UTF_16BE, true).getBom());
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFE, 0, 0},
                FileEncoding.fromCharset(FileEncoding.UTF_32LE, true).getBom());
        assertArrayEquals(new byte[]{0, 0, (byte) 0xFE, (byte) 0xFF},
                FileEncoding.fromCharset(FileEncoding.UTF_32BE, true).getBom());
        assertTrue(FileEncoding.fromCharset(FileEncoding.UTF_16LE, true).hasBom());
    }

    @Test
    public void fromCharsetWithoutBomReturnsNullBom() {
        FileEncoding encoding = FileEncoding.fromCharset(FileEncoding.UTF_16LE, false);
        assertEquals(FileEncoding.UTF_16LE, encoding.getCharsetName());
        assertNull(encoding.getBom());
        assertFalse(encoding.hasBom());
    }

    @Test
    public void fromCharsetRejectsNullOrEmptyName() {
        assertNull(FileEncoding.fromCharset(null, true));
        assertNull(FileEncoding.fromCharset("", false));
    }

    @Test
    public void fromCharsetUnknownCharsetWithBomHasNoBom() {
        FileEncoding encoding = FileEncoding.fromCharset("NO_SUCH_CHARSET", true);
        assertEquals("NO_SUCH_CHARSET", encoding.getCharsetName());
        assertNull(encoding.getBom());
        assertFalse(encoding.hasBom());
    }

    @Test
    public void detectIgnoresTruncatedBom() {
        assertNull(FileEncoding.detect(new byte[]{(byte) 0xFF}));
        assertNull(FileEncoding.detect(new byte[]{(byte) 0xEF, (byte) 0xBB}));
        assertNull(FileEncoding.detect(new byte[]{(byte) 0xFE}));
        assertNull(FileEncoding.detect(new byte[]{0, 0, (byte) 0xFE}));
        assertEquals(FileEncoding.UTF_16LE,
                FileEncoding.detect(new byte[]{(byte) 0xFF, (byte) 0xFE, 0}).getCharsetName());
    }

    @Test
    public void decodeBomOnlyFileReturnsEmpty() {
        byte[] utf16LeBomOnly = new byte[]{(byte) 0xFF, (byte) 0xFE};
        FileEncoding encoding = FileEncoding.detect(utf16LeBomOnly);
        assertEquals(FileEncoding.UTF_16LE, encoding.getCharsetName());
        assertEquals("", FileEncoding.decode(utf16LeBomOnly, encoding, FileEncoding.UTF_8));

        byte[] utf8BomOnly = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        assertEquals("", FileEncoding.decode(utf8BomOnly, FileEncoding.detect(utf8BomOnly),
                FileEncoding.UTF_8));
    }

    @Test
    public void encodeWithNullEncodingUsesFallback() {
        byte[] encoded = FileEncoding.encode("hello", null, FileEncoding.UTF_8);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), encoded);
    }

    @Test
    public void encodeWithoutBomDoesNotPrefixSignature() {
        FileEncoding encoding = FileEncoding.fromCharset(FileEncoding.UTF_16LE, false);
        byte[] encoded = FileEncoding.encode("hi", encoding, FileEncoding.UTF_8);
        assertArrayEquals("hi".getBytes(Charset.forName(FileEncoding.UTF_16LE)), encoded);
    }

    @Test
    public void encodeRoundTripForAllSupportedEncodings() {
        String text = "\u041f\u0440\u0438\u0432\u0435\u0442 \u043c\u0438\u0440";
        assertRoundTrip(FileEncoding.UTF_8, text);
        assertRoundTrip(FileEncoding.UTF_16LE, text);
        assertRoundTrip(FileEncoding.UTF_16BE, text);
        assertRoundTrip(FileEncoding.UTF_32LE, text);
        assertRoundTrip(FileEncoding.UTF_32BE, text);
    }

    private void assertRoundTrip(String charsetName, String text) {
        FileEncoding encoding = FileEncoding.fromCharset(charsetName, true);
        byte[] file = FileEncoding.encode(text, encoding, FileEncoding.UTF_8);

        FileEncoding detected = FileEncoding.detect(file);
        assertEquals(charsetName, detected.getCharsetName());

        String decoded = FileEncoding.decode(file, detected, FileEncoding.UTF_8);
        assertEquals(text, decoded);

        assertArrayEquals(file, FileEncoding.encode(decoded, detected, FileEncoding.UTF_8));
    }
}