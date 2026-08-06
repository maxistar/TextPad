package com.maxistar.textpad.recovery;

import org.json.JSONException;
import org.json.JSONObject;

public final class RecoveryMetadata {
    public static final int CURRENT_FORMAT_VERSION = 1;

    public final int formatVersion;
    public final String recoveryKey;
    public final String documentUri;
    public final String displayName;
    public final boolean untitled;
    public final String encoding;
    public final boolean hasBom;
    public final Long originalSize;
    public final Long originalLastModified;
    public final String originalContentSha256;
    public final long draftSize;
    public final String draftContentSha256;
    public final long draftUpdatedAt;
    public final int cursorStart;
    public final int cursorEnd;
    public final long generation;

    public RecoveryMetadata(
            String recoveryKey,
            String documentUri,
            String displayName,
            boolean untitled,
            String encoding,
            boolean hasBom,
            Long originalSize,
            Long originalLastModified,
            String originalContentSha256,
            long draftSize,
            long draftUpdatedAt,
            int cursorStart,
            int cursorEnd,
            long generation
    ) {
        this(
                CURRENT_FORMAT_VERSION,
                recoveryKey,
                documentUri,
                displayName,
                untitled,
                encoding,
                hasBom,
                originalSize,
                originalLastModified,
                originalContentSha256,
                draftSize,
                null,
                draftUpdatedAt,
                cursorStart,
                cursorEnd,
                generation
        );
    }

    private RecoveryMetadata(
            int formatVersion,
            String recoveryKey,
            String documentUri,
            String displayName,
            boolean untitled,
            String encoding,
            boolean hasBom,
            Long originalSize,
            Long originalLastModified,
            String originalContentSha256,
            long draftSize,
            String draftContentSha256,
            long draftUpdatedAt,
            int cursorStart,
            int cursorEnd,
            long generation
    ) {
        this.formatVersion = formatVersion;
        this.recoveryKey = recoveryKey;
        this.documentUri = documentUri;
        this.displayName = displayName;
        this.untitled = untitled;
        this.encoding = encoding;
        this.hasBom = hasBom;
        this.originalSize = originalSize;
        this.originalLastModified = originalLastModified;
        this.originalContentSha256 = originalContentSha256;
        this.draftSize = draftSize;
        this.draftContentSha256 = draftContentSha256;
        this.draftUpdatedAt = draftUpdatedAt;
        this.cursorStart = cursorStart;
        this.cursorEnd = cursorEnd;
        this.generation = generation;
    }

    public RecoveryMetadata published(long size, String contentSha256, long updatedAt) {
        return new RecoveryMetadata(
                formatVersion,
                recoveryKey,
                documentUri,
                displayName,
                untitled,
                encoding,
                hasBom,
                originalSize,
                originalLastModified,
                originalContentSha256,
                size,
                contentSha256,
                updatedAt,
                cursorStart,
                cursorEnd,
                generation
        );
    }

    public RecoveryMetadata withIdentity(String newKey, String newUri, String newDisplayName) {
        return new RecoveryMetadata(
                formatVersion,
                newKey,
                newUri,
                newDisplayName,
                false,
                encoding,
                hasBom,
                originalSize,
                originalLastModified,
                originalContentSha256,
                draftSize,
                draftContentSha256,
                draftUpdatedAt,
                cursorStart,
                cursorEnd,
                generation
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject value = new JSONObject();
        value.put("formatVersion", formatVersion);
        value.put("recoveryKey", recoveryKey);
        value.put("documentUri", documentUri == null ? JSONObject.NULL : documentUri);
        value.put("displayName", displayName);
        value.put("isUntitled", untitled);
        value.put("encoding", encoding);
        value.put("hasBom", hasBom);
        value.put("originalSize", originalSize == null ? JSONObject.NULL : originalSize);
        value.put("originalLastModified", originalLastModified == null ? JSONObject.NULL : originalLastModified);
        value.put("originalContentSha256", originalContentSha256 == null ? JSONObject.NULL : originalContentSha256);
        value.put("draftSize", draftSize);
        value.put("draftContentSha256", draftContentSha256);
        value.put("draftUpdatedAt", draftUpdatedAt);
        value.put("cursorStart", cursorStart);
        value.put("cursorEnd", cursorEnd);
        value.put("generation", generation);
        return value;
    }

    public static RecoveryMetadata fromJson(JSONObject value) throws JSONException {
        int version = value.getInt("formatVersion");
        if (version != CURRENT_FORMAT_VERSION) {
            throw new JSONException("Unsupported recovery metadata version: " + version);
        }
        return new RecoveryMetadata(
                version,
                value.getString("recoveryKey"),
                nullableString(value, "documentUri"),
                value.optString("displayName", ""),
                value.getBoolean("isUntitled"),
                value.optString("encoding", "UTF-8"),
                value.optBoolean("hasBom", false),
                nullableLong(value, "originalSize"),
                nullableLong(value, "originalLastModified"),
                nullableString(value, "originalContentSha256"),
                value.getLong("draftSize"),
                value.getString("draftContentSha256"),
                value.getLong("draftUpdatedAt"),
                value.optInt("cursorStart", 0),
                value.optInt("cursorEnd", value.optInt("cursorStart", 0)),
                value.optLong("generation", 0)
        );
    }

    private static String nullableString(JSONObject value, String name) throws JSONException {
        return value.isNull(name) ? null : value.getString(name);
    }

    private static Long nullableLong(JSONObject value, String name) throws JSONException {
        return value.isNull(name) ? null : value.getLong(name);
    }
}
