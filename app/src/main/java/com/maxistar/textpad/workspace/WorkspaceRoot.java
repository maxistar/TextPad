package com.maxistar.textpad.workspace;

import java.util.Objects;

public final class WorkspaceRoot {
    private final String uri;
    private final String displayName;
    private final boolean available;
    private final boolean readable;
    private final boolean writable;

    public WorkspaceRoot(
            String uri,
            String displayName,
            boolean available,
            boolean readable,
            boolean writable
    ) {
        this.uri = Objects.requireNonNull(uri);
        this.displayName = displayName == null ? "" : displayName;
        this.available = available;
        this.readable = readable;
        this.writable = writable;
    }

    public String getUri() {
        return uri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public WorkspaceRoot withAccess(boolean available, boolean readable, boolean writable) {
        return new WorkspaceRoot(uri, displayName, available, readable, writable);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkspaceRoot)) {
            return false;
        }
        WorkspaceRoot root = (WorkspaceRoot) other;
        return available == root.available
                && readable == root.readable
                && writable == root.writable
                && uri.equals(root.uri)
                && displayName.equals(root.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, displayName, available, readable, writable);
    }
}
