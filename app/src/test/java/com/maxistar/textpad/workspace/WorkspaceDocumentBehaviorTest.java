package com.maxistar.textpad.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class WorkspaceDocumentBehaviorTest {
    @Test
    void sortingIsFolderFirstCaseInsensitiveAndDeterministic() {
        List<WorkspaceDocument> documents = new ArrayList<>(Arrays.asList(
                document("3", "readme.md", false, "text/markdown"),
                document("2", "Beta", true, ""),
                document("1", "alpha", true, ""),
                document("5", "README.md", false, "text/markdown"),
                document("4", "readme.md", false, "text/markdown")
        ));

        documents.sort(WorkspaceDocumentSorter.FOLDER_FIRST);

        assertEquals(Arrays.asList("1", "2", "5", "3", "4"),
                Arrays.asList(
                        documents.get(0).getUri(),
                        documents.get(1).getUri(),
                        documents.get(2).getUri(),
                        documents.get(3).getUri(),
                        documents.get(4).getUri()));
    }

    @Test
    void classifierUsesMimeTypeThenKnownExtensions() {
        WorkspaceFileClassifier classifier = new WorkspaceFileClassifier();

        assertTrue(classifier.isInternalText(
                document("1", "without-extension", false, "text/plain")));
        assertTrue(classifier.isInternalText(
                document("2", "README.MD", false, "application/octet-stream")));
        assertTrue(classifier.isInternalText(
                document("3", "script.MjS", false, "")));
        assertFalse(classifier.isInternalText(
                document("4", "photo.jpg", false, "image/jpeg")));
        assertFalse(classifier.isInternalText(
                document("5", "unknown", false, "")));
        assertFalse(classifier.isInternalText(
                document("6", "folder.txt", true, "text/plain")));
    }

    private static WorkspaceDocument document(
            String uri,
            String name,
            boolean directory,
            String mime
    ) {
        return new WorkspaceDocument(
                uri, name, mime, directory, null, null, false);
    }
}
