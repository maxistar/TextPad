package com.maxistar.textpad.utils;

import java.nio.charset.Charset;

/**
 * Detects a text file encoding from its byte order mark (BOM) and decodes or
 * encodes the content keeping the original encoding.
 */
public class FileEncoding {

    public static final String UTF_8 = "UTF-8";
    public static final String UTF_16LE = "UTF-16LE";
    public static final String UTF_16BE = "UTF-16BE";
    public static final String UTF_32LE = "UTF-32LE";
    public static final String UTF_32BE = "UTF-32BE";

    private static final byte[] BOM_UTF_32LE = {(byte) 0xFF, (byte) 0xFE, 0, 0};
    private static final byte[] BOM_UTF_32BE = {0, 0, (byte) 0xFE, (byte) 0xFF};
    private static final byte[] BOM_UTF_8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] BOM_UTF_16BE = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] BOM_UTF_16LE = {(byte) 0xFF, (byte) 0xFE};

    private final String charsetName;
    private final byte[] bom;

    private FileEncoding(String charsetName, byte[] bom) {
        this.charsetName = charsetName;
        this.bom = bom;
    }

    public String getCharsetName() {
        return charsetName;
    }

    public byte[] getBom() {
        return bom;
    }

    public boolean hasBom() {
        return bom != null;
    }

    public static FileEncoding detect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (startsWith(bytes, BOM_UTF_32LE)) {
            return new FileEncoding(UTF_32LE, BOM_UTF_32LE);
        }
        if (startsWith(bytes, BOM_UTF_32BE)) {
            return new FileEncoding(UTF_32BE, BOM_UTF_32BE);
        }
        if (startsWith(bytes, BOM_UTF_8)) {
            return new FileEncoding(UTF_8, BOM_UTF_8);
        }
        if (startsWith(bytes, BOM_UTF_16BE)) {
            return new FileEncoding(UTF_16BE, BOM_UTF_16BE);
        }
        if (startsWith(bytes, BOM_UTF_16LE)) {
            return new FileEncoding(UTF_16LE, BOM_UTF_16LE);
        }
        return null;
    }

    public static FileEncoding fromCharset(String charsetName, boolean hasBom) {
        if (charsetName == null || charsetName.isEmpty()) {
            return null;
        }
        if (!hasBom) {
            return new FileEncoding(charsetName, null);
        }
        byte[] bom = bomForCharset(charsetName);
        return new FileEncoding(charsetName, bom);
    }

    private static byte[] bomForCharset(String charsetName) {
        if (UTF_32LE.equals(charsetName)) return BOM_UTF_32LE;
        if (UTF_32BE.equals(charsetName)) return BOM_UTF_32BE;
        if (UTF_8.equals(charsetName)) return BOM_UTF_8;
        if (UTF_16BE.equals(charsetName)) return BOM_UTF_16BE;
        if (UTF_16LE.equals(charsetName)) return BOM_UTF_16LE;
        return null;
    }

    public static String decode(byte[] bytes, FileEncoding encoding, String fallbackCharsetName) {
        if (bytes == null) {
            return "";
        }
        int offset = 0;
        if (encoding != null && encoding.hasBom() && bytes.length >= encoding.getBom().length) {
            offset = encoding.getBom().length;
        }
        String charsetName = encoding != null ? encoding.getCharsetName() : fallbackCharsetName;
        try {
            return new String(bytes, offset, bytes.length - offset, charsetForName(charsetName));
        } catch (Exception e) {
            return new String(bytes);
        }
    }

    public static byte[] encode(String text, FileEncoding encoding, String fallbackCharsetName) {
        String charsetName = encoding != null ? encoding.getCharsetName() : fallbackCharsetName;
        byte[] body = text.getBytes(charsetForName(charsetName));
        if (encoding != null && encoding.hasBom()) {
            byte[] result = new byte[encoding.getBom().length + body.length];
            java.lang.System.arraycopy(encoding.getBom(), 0, result, 0, encoding.getBom().length);
            java.lang.System.arraycopy(body, 0, result, encoding.getBom().length, body.length);
            return result;
        }
        return body;
    }

    private static Charset charsetForName(String charsetName) {
        if (charsetName == null) {
            return Charset.defaultCharset();
        }
        try {
            return Charset.forName(charsetName);
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}