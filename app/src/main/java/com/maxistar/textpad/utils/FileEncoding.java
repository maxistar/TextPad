package com.maxistar.textpad.utils;

import java.nio.charset.Charset;

/**
 * Detects a text file encoding from its byte order mark (BOM) and decodes or
 * encodes the content keeping the original encoding.
 *
 * <p>The BOM is always tied to the charset that produced it: if the runtime
 * cannot handle that charset, the BOM is ignored instead of being written in
 * front of bytes encoded with a different charset.</p>
 */
public final class FileEncoding {

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
    private final Charset charset;

    private FileEncoding(String charsetName, byte[] bom, Charset charset) {
        this.charsetName = charsetName;
        this.bom = bom == null ? null : bom.clone();
        this.charset = charset;
    }

    public String getCharsetName() {
        return charsetName;
    }

    /**
     * Returns a copy of the byte order mark, or null when the file has none.
     */
    public byte[] getBom() {
        return bom == null ? null : bom.clone();
    }

    public boolean hasBom() {
        return bom != null;
    }

    public static FileEncoding detect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (startsWith(bytes, BOM_UTF_32LE)) {
            return create(UTF_32LE, BOM_UTF_32LE);
        }
        if (startsWith(bytes, BOM_UTF_32BE)) {
            return create(UTF_32BE, BOM_UTF_32BE);
        }
        if (startsWith(bytes, BOM_UTF_8)) {
            return create(UTF_8, BOM_UTF_8);
        }
        if (startsWith(bytes, BOM_UTF_16BE)) {
            return create(UTF_16BE, BOM_UTF_16BE);
        }
        if (startsWith(bytes, BOM_UTF_16LE)) {
            return create(UTF_16LE, BOM_UTF_16LE);
        }
        return null;
    }

    /**
     * Rebuilds an encoding from a charset name persisted elsewhere (for example
     * in the recovery metadata). Returns null for an empty name. When the
     * runtime does not support the charset the BOM is dropped, so a later save
     * cannot mix it with bytes of another charset.
     */
    public static FileEncoding fromCharset(String charsetName, boolean hasBom) {
        if (charsetName == null || charsetName.isEmpty()) {
            return null;
        }
        return create(charsetName, hasBom ? bomForCharset(charsetName) : null);
    }

    public static String decode(byte[] bytes, FileEncoding encoding, String fallbackCharsetName) {
        if (bytes == null) {
            return "";
        }
        int offset = 0;
        if (encoding != null && encoding.bom != null && bytes.length >= encoding.bom.length) {
            offset = encoding.bom.length;
        }
        Charset target = encoding == null ? null : encoding.charset;
        if (target == null) {
            target = lookupCharset(fallbackCharsetName);
        }
        if (target == null) {
            target = Charset.defaultCharset();
        }
        return new String(bytes, offset, bytes.length - offset, target);
    }

    public static byte[] encode(String text, FileEncoding encoding, String fallbackCharsetName) {
        Charset target = encoding == null ? null : encoding.charset;
        byte[] prefix = target == null ? null : encoding.bom;
        if (target == null) {
            target = lookupCharset(fallbackCharsetName);
        }
        if (target == null) {
            target = Charset.defaultCharset();
        }

        byte[] body = text.getBytes(target);
        if (prefix == null) {
            return body;
        }
        byte[] result = new byte[prefix.length + body.length];
        java.lang.System.arraycopy(prefix, 0, result, 0, prefix.length);
        java.lang.System.arraycopy(body, 0, result, prefix.length, body.length);
        return result;
    }

    private static FileEncoding create(String charsetName, byte[] bom) {
        Charset charset = lookupCharset(charsetName);
        return new FileEncoding(charsetName, charset == null ? null : bom, charset);
    }

    private static byte[] bomForCharset(String charsetName) {
        if (UTF_32LE.equals(charsetName)) return BOM_UTF_32LE;
        if (UTF_32BE.equals(charsetName)) return BOM_UTF_32BE;
        if (UTF_8.equals(charsetName)) return BOM_UTF_8;
        if (UTF_16BE.equals(charsetName)) return BOM_UTF_16BE;
        if (UTF_16LE.equals(charsetName)) return BOM_UTF_16LE;
        return null;
    }

    private static Charset lookupCharset(String charsetName) {
        if (charsetName == null || charsetName.isEmpty()) {
            return null;
        }
        try {
            return Charset.forName(charsetName);
        } catch (Exception e) {
            return null;
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
