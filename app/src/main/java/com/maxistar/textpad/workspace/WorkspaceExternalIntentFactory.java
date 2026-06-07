package com.maxistar.textpad.workspace;

import android.content.Intent;
import android.net.Uri;

public final class WorkspaceExternalIntentFactory {
    public Intent create(WorkspaceDocument document) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(document.getUri());
        String mimeType = document.getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "*/*";
        }
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (document.isWritable()) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        return intent;
    }
}
