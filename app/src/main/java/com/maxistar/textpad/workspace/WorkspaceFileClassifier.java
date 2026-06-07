package com.maxistar.textpad.workspace;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class WorkspaceFileClassifier {
    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "md", "markdown", "json", "js", "mjs", "cjs", "log", "srt"
    ));

    public boolean isInternalText(WorkspaceDocument document) {
        if (document.isDirectory()) {
            return false;
        }
        String mimeType = document.getMimeType();
        if (mimeType != null
                && mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
            return true;
        }
        String name = document.getDisplayName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        return TEXT_EXTENSIONS.contains(
                name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
