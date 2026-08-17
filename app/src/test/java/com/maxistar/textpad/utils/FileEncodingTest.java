package com.maxistar.textpad.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}