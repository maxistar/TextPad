package com.maxistar.textpad.workspace;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AndroidWorkspaceDocumentProvider
        implements WorkspaceDocumentProvider {
    private static final String[] CHILD_PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
    };

    private final ContentResolver contentResolver;

    public AndroidWorkspaceDocumentProvider(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    @Override
    public List<WorkspaceDocument> listChildren(
            String treeUriValue,
            String directoryUriValue
    ) throws IOException {
        Uri treeUri = Uri.parse(treeUriValue);
        Uri directoryUri = directoryUriValue == null || directoryUriValue.isEmpty()
                ? rootDocumentUri(treeUri) : Uri.parse(directoryUriValue);
        String documentId = DocumentsContract.getDocumentId(directoryUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri, documentId);
        List<WorkspaceDocument> children = new ArrayList<>();
        try (Cursor cursor = contentResolver.query(
                childrenUri, CHILD_PROJECTION, null, null, null)) {
            if (cursor == null) {
                throw new FileNotFoundException(childrenUri.toString());
            }
            int idColumn = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            if (idColumn < 0) {
                throw new IOException("Document provider omitted document ID");
            }
            int nameColumn = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_MIME_TYPE);
            int sizeColumn = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_SIZE);
            int modifiedColumn = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED);
            int flagsColumn = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_FLAGS);
            while (cursor.moveToNext()) {
                String childId = cursor.getString(idColumn);
                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri, childId);
                String name = stringValue(cursor, nameColumn, childId);
                String mimeType = stringValue(cursor, mimeColumn, "");
                int flags = intValue(cursor, flagsColumn);
                boolean directory = DocumentsContract.Document.MIME_TYPE_DIR.equals(
                        mimeType);
                boolean writable = (flags
                        & DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0;
                children.add(new WorkspaceDocument(
                        childUri.toString(),
                        name,
                        mimeType,
                        directory,
                        longValue(cursor, sizeColumn),
                        longValue(cursor, modifiedColumn),
                        writable
                ));
            }
        } catch (SecurityException exception) {
            throw new IOException("Workspace permission denied", exception);
        } catch (RuntimeException exception) {
            throw new IOException("Document provider query failed", exception);
        }
        Collections.sort(children, WorkspaceDocumentSorter.FOLDER_FIRST);
        return children;
    }

    @Override
    public String getDisplayName(String treeUriValue) throws IOException {
        Uri documentUri = rootDocumentUri(Uri.parse(treeUriValue));
        String[] projection = {
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
        };
        try (Cursor cursor = contentResolver.query(
                documentUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                String value = stringValue(cursor, column, "");
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException("Unable to resolve workspace name", exception);
        }
        String id = DocumentsContract.getTreeDocumentId(Uri.parse(treeUriValue));
        int separator = id.lastIndexOf('/');
        return separator >= 0 && separator < id.length() - 1
                ? id.substring(separator + 1) : id;
    }

    private static Uri rootDocumentUri(Uri treeUri) {
        return DocumentsContract.buildDocumentUriUsingTree(
                treeUri, DocumentsContract.getTreeDocumentId(treeUri));
    }

    private static String stringValue(Cursor cursor, int column, String fallback) {
        return column >= 0 && !cursor.isNull(column)
                ? cursor.getString(column) : fallback;
    }

    private static Long longValue(Cursor cursor, int column) {
        return column >= 0 && !cursor.isNull(column)
                ? cursor.getLong(column) : null;
    }

    private static int intValue(Cursor cursor, int column) {
        return column >= 0 && !cursor.isNull(column)
                ? cursor.getInt(column) : 0;
    }
}
