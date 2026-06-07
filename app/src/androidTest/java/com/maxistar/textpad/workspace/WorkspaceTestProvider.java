package com.maxistar.textpad.workspace;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.DocumentsContract;

public final class WorkspaceTestProvider extends ContentProvider {
    private static boolean fail;
    private static TrackingCursor lastCursor;

    public static void reset() {
        fail = false;
        lastCursor = null;
    }

    public static void setFail(boolean value) {
        fail = value;
    }

    public static boolean wasLastCursorClosed() {
        return lastCursor != null && lastCursor.closed;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        if (fail) {
            throw new SecurityException("denied");
        }
        if (uri.toString().contains("/children")) {
            TrackingCursor cursor = new TrackingCursor(new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            });
            cursor.addRow(new Object[]{"child", "child"});
            lastCursor = cursor;
            return cursor;
        }
        return new MatrixCursor(new String[]{
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
        });
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs
    ) {
        return 0;
    }

    private static final class TrackingCursor extends MatrixCursor {
        private boolean closed;

        private TrackingCursor(String[] columns) {
            super(columns);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }
}
