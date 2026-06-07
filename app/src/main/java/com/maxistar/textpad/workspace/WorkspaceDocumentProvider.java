package com.maxistar.textpad.workspace;

import java.io.IOException;
import java.util.List;

public interface WorkspaceDocumentProvider {
    List<WorkspaceDocument> listChildren(String treeUri, String directoryUri)
            throws IOException;

    String getDisplayName(String treeUri) throws IOException;
}
