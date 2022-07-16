package com.maxistar.textpad.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

public class FileNameHelper {

    static public String getFilenameByUri(Context context, String uri) {
        Uri uri1 = Uri.parse(uri);
        return getFilenameByUri(context, uri1);
    }

    /**
     * found solution here
     * https://stackoverflow.com/questions/64224012/xamarin-plugin-filepicker-content-com-android-providers-downloads-documents-p
     * @param uri Url
     * @return Parsed String
     */
    static public String getFilenameByUri(Context context, @NonNull Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    new String[] {
                            MediaStore.MediaColumns.DISPLAY_NAME
                    },
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            return getFilenameByUriFallback(uri);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return getLegacyFilenameByUri(uri);
    }

    static private String getLegacyFilenameByUri(Uri uri) {
        String path = uri.getPath();
        String[] paths = path.split("/");
        if (paths.length == 0) {
            return "";
        }
        return paths[paths.length -1];
    }

    /**
     * @todo Move to external class and cover with test
     * @param uri File Url
     * @return Readable File Name
     */
    static private String getFilenameByUriFallback(@NonNull Uri uri) {
        String path = uri.getPath();
        if (path == null) {
            return "";
        }
        String[] paths = path.split("/");
        if (paths.length == 0) {
            return "";
        }
        return paths[paths.length -1];
    }

}
