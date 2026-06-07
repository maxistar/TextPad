package com.maxistar.textpad.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

class WorkspaceTreeTest {
    @Test
    void expansionLoadsOnlyImmediateChildrenAndCachesThem() {
        AtomicInteger queries = new AtomicInteger();
        WorkspaceDocumentProvider provider = provider(queries, Arrays.asList(
                document("folder", true),
                document("file.txt", false)
        ));
        AtomicInteger changes = new AtomicInteger();
        WorkspaceTreeController controller = controller(provider, changes);
        controller.setRoots(Collections.singletonList(root()));
        WorkspaceTreeNode root = controller.getRoots().get(0);

        controller.toggle(root);
        controller.toggle(root);
        controller.toggle(root);

        assertEquals(1, queries.get());
        assertEquals(2, root.getChildren().size());
        assertEquals(3, new WorkspaceTreeFlattener()
                .flatten(controller.getRoots()).size());
    }

    @Test
    void emptyAndFailureStatesCanBeRetried() {
        AtomicInteger queries = new AtomicInteger();
        WorkspaceTreeController empty = controller(
                provider(queries, Collections.emptyList()), new AtomicInteger());
        empty.setRoots(Collections.singletonList(root()));
        WorkspaceTreeNode root = empty.getRoots().get(0);
        empty.toggle(root);
        assertEquals(WorkspaceTreeNode.State.EMPTY, root.getState());

        WorkspaceTreeController failing = controller(
                new WorkspaceDocumentProvider() {
                    @Override
                    public List<WorkspaceDocument> listChildren(
                            String treeUri, String directoryUri
                    ) throws IOException {
                        throw new IOException("provider unavailable");
                    }

                    @Override
                    public String getDisplayName(String treeUri) {
                        return "Root";
                    }
                }, new AtomicInteger());
        failing.setRoots(Collections.singletonList(root()));
        WorkspaceTreeNode failedRoot = failing.getRoots().get(0);
        failing.toggle(failedRoot);
        assertEquals(WorkspaceTreeNode.State.UNAVAILABLE, failedRoot.getState());
    }

    @Test
    void refreshReplacesOnlySelectedSubtree() {
        AtomicInteger queries = new AtomicInteger();
        WorkspaceTreeController controller = controller(
                provider(queries, Collections.singletonList(
                        document("file.txt", false))),
                new AtomicInteger());
        controller.setRoots(Arrays.asList(
                root("one"), root("two")));
        WorkspaceTreeNode first = controller.getRoots().get(0);
        WorkspaceTreeNode second = controller.getRoots().get(1);
        controller.toggle(first);
        controller.toggle(second);

        controller.refresh(first);

        assertEquals(3, queries.get());
        assertEquals(WorkspaceTreeNode.State.LOADED, second.getState());
    }

    @Test
    void supersededAndDestroyedResultsAreDiscarded() {
        List<Runnable> background = new ArrayList<>();
        List<Runnable> posted = new ArrayList<>();
        AtomicInteger queries = new AtomicInteger();
        WorkspaceTreeController controller = new WorkspaceTreeController(
                provider(queries, Collections.singletonList(
                        document("file.txt", false))),
                background::add,
                posted::add,
                () -> {
                });
        controller.setRoots(Collections.singletonList(root()));
        WorkspaceTreeNode root = controller.getRoots().get(0);

        controller.toggle(root);
        controller.refresh(root);
        background.get(0).run();
        posted.get(0).run();
        assertEquals(WorkspaceTreeNode.State.LOADING, root.getState());

        background.get(1).run();
        controller.destroy();
        posted.get(1).run();
        assertEquals(WorkspaceTreeNode.State.NOT_LOADED, root.getState());
    }

    @Test
    void largeDirectoryBuildsOnlyItsVisibleImmediateRows() {
        List<WorkspaceDocument> documents = new ArrayList<>();
        for (int index = 0; index < 5000; index++) {
            documents.add(document("file-" + index + ".txt", false));
        }
        AtomicInteger queries = new AtomicInteger();
        WorkspaceTreeController controller = controller(
                provider(queries, documents), new AtomicInteger());
        controller.setRoots(Collections.singletonList(root()));

        controller.toggle(controller.getRoots().get(0));

        assertEquals(1, queries.get());
        assertEquals(5001, new WorkspaceTreeFlattener()
                .flatten(controller.getRoots()).size());
    }

    private static WorkspaceTreeController controller(
            WorkspaceDocumentProvider provider,
            AtomicInteger changes
    ) {
        Executor direct = Runnable::run;
        return new WorkspaceTreeController(
                provider, direct, Runnable::run, changes::incrementAndGet);
    }

    private static WorkspaceDocumentProvider provider(
            AtomicInteger queries,
            List<WorkspaceDocument> documents
    ) {
        return new WorkspaceDocumentProvider() {
            @Override
            public List<WorkspaceDocument> listChildren(
                    String treeUri,
                    String directoryUri
            ) {
                queries.incrementAndGet();
                return documents;
            }

            @Override
            public String getDisplayName(String treeUri) {
                return "Root";
            }
        };
    }

    private static WorkspaceRoot root() {
        return root("root");
    }

    private static WorkspaceRoot root(String name) {
        return new WorkspaceRoot(
                "content://tree/" + name, name, true, true, true);
    }

    private static WorkspaceDocument document(String name, boolean directory) {
        return new WorkspaceDocument(
                "content://document/" + name,
                name,
                directory ? "vnd.android.document/directory" : "text/plain",
                directory,
                null,
                null,
                false
        );
    }
}
