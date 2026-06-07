package com.maxistar.textpad.workspace;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;

public final class WorkspacePermissionManager {
    private final ContentResolver contentResolver;

    public WorkspacePermissionManager(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public WorkspaceRoot takePermission(
            Intent result,
            String displayName
    ) {
        Uri uri = result.getData();
        if (uri == null) {
            return null;
        }
        int flags = result.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        boolean readable = (flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0;
        boolean writable = (flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0;
        if (readable && writable) {
            contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else if (readable) {
            contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (writable) {
            contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        return new WorkspaceRoot(
                uri.toString(), displayName, readable, readable, writable);
    }

    public WorkspaceRoot restored(WorkspaceRoot root) {
        for (UriPermission permission : contentResolver.getPersistedUriPermissions()) {
            if (permission.getUri().toString().equals(root.getUri())) {
                boolean readable = permission.isReadPermission();
                return root.withAccess(
                        readable, readable, permission.isWritePermission());
            }
        }
        return root.withAccess(false, false, false);
    }

    public void release(WorkspaceRoot root) {
        int flags = 0;
        if (root.isReadable()) {
            flags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;
        }
        if (root.isWritable()) {
            flags |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        }
        if (flags == 0) {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        }
        try {
            contentResolver.releasePersistableUriPermission(
                    Uri.parse(root.getUri()), flags);
        } catch (SecurityException ignored) {
            // The grant may already have been revoked by the provider or system.
        }
    }
}
