package com.maxistar.textpad.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WorkspaceAndroidBehaviorTest {
    private static final String AUTHORITY = "com.maxistar.textpad.workspace.test";
    private static final String TREE_URI =
            "content://" + AUTHORITY + "/tree/root";

    @Test
    public void providerUsesMetadataFallbacksAndClosesCursor() throws Exception {
        ContentResolver resolver = ApplicationProvider.getApplicationContext()
                .getContentResolver();
        WorkspaceTestProvider.reset();
        AndroidWorkspaceDocumentProvider documents =
                new AndroidWorkspaceDocumentProvider(resolver);

        List<WorkspaceDocument> children =
                documents.listChildren(TREE_URI, null);

        assertEquals(1, children.size());
        assertEquals("child", children.get(0).getDisplayName());
        assertEquals("", children.get(0).getMimeType());
        assertFalse(children.get(0).isDirectory());
        assertTrue(WorkspaceTestProvider.wasLastCursorClosed());
        assertEquals("root", documents.getDisplayName(TREE_URI));
    }

    @Test
    public void providerWrapsQueryFailuresAsIoExceptions() {
        ContentResolver resolver = ApplicationProvider.getApplicationContext()
                .getContentResolver();
        WorkspaceTestProvider.setFail(true);
        AndroidWorkspaceDocumentProvider documents =
                new AndroidWorkspaceDocumentProvider(resolver);

        assertThrows(IOException.class,
                () -> documents.listChildren(TREE_URI, null));
    }

    @Test
    public void externalIntentIncludesExpectedTemporaryGrants() {
        WorkspaceExternalIntentFactory factory =
                new WorkspaceExternalIntentFactory();
        WorkspaceDocument readOnly = new WorkspaceDocument(
                "content://example/read-only",
                "photo.jpg",
                "image/jpeg",
                false,
                null,
                null,
                false
        );
        WorkspaceDocument writable = new WorkspaceDocument(
                "content://example/writable",
                "document.pdf",
                "application/pdf",
                false,
                null,
                null,
                true
        );

        Intent readIntent = factory.create(readOnly);
        Intent writeIntent = factory.create(writable);

        assertEquals(Intent.ACTION_VIEW, readIntent.getAction());
        assertEquals("image/jpeg", readIntent.getType());
        assertTrue((readIntent.getFlags()
                & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertFalse((readIntent.getFlags()
                & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0);
        assertTrue((writeIntent.getFlags()
                & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0);
    }

    @Test
    public void externalDispatcherReportsMissingHandler() {
        WorkspaceExternalDispatcher dispatcher =
                new WorkspaceExternalDispatcher();

        boolean launched = dispatcher.dispatch(
                new Intent(Intent.ACTION_VIEW),
                intent -> {
                    throw new ActivityNotFoundException();
                });

        assertFalse(launched);
    }

}
