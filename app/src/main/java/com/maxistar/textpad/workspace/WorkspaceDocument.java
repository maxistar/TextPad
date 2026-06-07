package com.maxistar.textpad.workspace;

import java.util.Objects;

public final class WorkspaceDocument {
    private final String uri;
    private final String displayName;
    private final String mimeType;
    private final boolean directory;
    private final Long size;
    private final Long lastModified;
    private final boolean writable;

    public WorkspaceDocument(
            String uri,
            String displayName,
            String mimeType,
            boolean directory,
            Long size,
            Long lastModified,
            boolean writable
    ) {
        this.uri = Objects.requireNonNull(uri);
        this.displayName = displayName == null ? "" : displayName;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.directory = directory;
        this.size = size;
        this.lastModified = lastModified;
        this.writable = writable;
    }

    public String getUri() {
        return uri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isDirectory() {
        return directory;
    }

    public Long getSize() {
        return size;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public boolean isWritable() {
        return writable;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkspaceDocument)) {
            return false;
        }
        WorkspaceDocument document = (WorkspaceDocument) other;
        return directory == document.directory
                && writable == document.writable
                && uri.equals(document.uri)
                && displayName.equals(document.displayName)
                && mimeType.equals(document.mimeType)
                && Objects.equals(size, document.size)
                && Objects.equals(lastModified, document.lastModified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                uri, displayName, mimeType, directory, size, lastModified, writable);
    }
}
