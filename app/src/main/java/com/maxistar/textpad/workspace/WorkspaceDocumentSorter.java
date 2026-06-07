package com.maxistar.textpad.workspace;

import java.util.Comparator;

public final class WorkspaceDocumentSorter {
    private WorkspaceDocumentSorter() {
    }

    public static final Comparator<WorkspaceDocument> FOLDER_FIRST =
            (left, right) -> {
                if (left.isDirectory() != right.isDirectory()) {
                    return left.isDirectory() ? -1 : 1;
                }
                int nameOrder = left.getDisplayName().compareToIgnoreCase(
                        right.getDisplayName());
                if (nameOrder != 0) {
                    return nameOrder;
                }
                int exactNameOrder = left.getDisplayName().compareTo(
                        right.getDisplayName());
                if (exactNameOrder != 0) {
                    return exactNameOrder;
                }
                return left.getUri().compareTo(right.getUri());
            };
}
