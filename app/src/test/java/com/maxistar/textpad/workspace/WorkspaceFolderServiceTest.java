package com.maxistar.textpad.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maxistar.textpad.service.WorkspaceFolderService;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class WorkspaceFolderServiceTest {
    @Test
    void codecRoundTripPreservesOrder() {
        WorkspaceRegistryCodec codec = new WorkspaceRegistryCodec();
        List<WorkspaceRoot> roots = Arrays.asList(
                root("content://tree/one", "One"),
                root("content://tree/two", "Two")
        );

        List<WorkspaceRoot> decoded = codec.decode(codec.encode(roots));

        assertEquals("content://tree/one", decoded.get(0).getUri());
        assertEquals("content://tree/two", decoded.get(1).getUri());
    }

    @Test
    void malformedAndUnsupportedDataFallsBackToEmpty() {
        WorkspaceRegistryCodec codec = new WorkspaceRegistryCodec();

        assertEquals(Collections.emptyList(), codec.decode("{broken"));
        assertEquals(Collections.emptyList(),
                codec.decode("{\"version\":99,\"roots\":[]}"));
        assertEquals(Collections.emptyList(), codec.decode(""));
    }

    @Test
    void serviceRejectsDuplicatesButAllowsNestedRoots() {
        MemoryStore store = new MemoryStore();
        WorkspaceFolderService service =
                new WorkspaceFolderService(store, new WorkspaceRegistryCodec());

        assertTrue(service.add(root("content://tree/project", "Project")));
        assertFalse(service.add(root("content://tree/project", "Duplicate")));
        assertTrue(service.add(root("content://tree/project/document/docs", "Docs")));
        assertEquals(2, service.getRoots().size());
    }

    @Test
    void replacementPreservesPositionAndRemovalPersists() {
        MemoryStore store = new MemoryStore();
        WorkspaceFolderService service =
                new WorkspaceFolderService(store, new WorkspaceRegistryCodec());
        service.add(root("content://tree/one", "One"));
        service.add(root("content://tree/two", "Two"));

        assertTrue(service.replace(
                "content://tree/one",
                root("content://tree/replacement", "Replacement")));
        assertEquals("content://tree/replacement", service.getRoots().get(0).getUri());
        assertTrue(service.remove("content://tree/two"));

        WorkspaceFolderService restored =
                new WorkspaceFolderService(store, new WorkspaceRegistryCodec());
        assertEquals(1, restored.getRoots().size());
        assertEquals("Replacement", restored.getRoots().get(0).getDisplayName());
    }

    private static WorkspaceRoot root(String uri, String name) {
        return new WorkspaceRoot(uri, name, true, true, true);
    }

    private static final class MemoryStore implements WorkspaceRegistryStore {
        private String value = "";

        @Override
        public String read() {
            return value;
        }

        @Override
        public void write(String value) {
            this.value = value;
        }
    }
}
