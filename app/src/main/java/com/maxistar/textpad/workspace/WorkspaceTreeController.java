package com.maxistar.textpad.workspace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public final class WorkspaceTreeController {
    public interface ResultPoster {
        void post(Runnable runnable);
    }

    public interface Listener {
        void onTreeChanged();
    }

    private final WorkspaceDocumentProvider provider;
    private final Executor executor;
    private final ResultPoster resultPoster;
    private final Listener listener;
    private final List<WorkspaceTreeNode> roots = new ArrayList<>();
    private boolean destroyed;

    public WorkspaceTreeController(
            WorkspaceDocumentProvider provider,
            Executor executor,
            ResultPoster resultPoster,
            Listener listener
    ) {
        this.provider = provider;
        this.executor = executor;
        this.resultPoster = resultPoster;
        this.listener = listener;
    }

    public void setRoots(List<WorkspaceRoot> workspaceRoots) {
        roots.clear();
        for (WorkspaceRoot root : workspaceRoots) {
            roots.add(WorkspaceTreeNode.root(root));
        }
        listener.onTreeChanged();
    }

    public List<WorkspaceTreeNode> getRoots() {
        return Collections.unmodifiableList(roots);
    }

    public void toggle(WorkspaceTreeNode node) {
        if (!node.isExpandable() || node.getState() == WorkspaceTreeNode.State.UNAVAILABLE) {
            return;
        }
        node.setExpanded(!node.isExpanded());
        listener.onTreeChanged();
        if (node.isExpanded()
                && node.getState() == WorkspaceTreeNode.State.NOT_LOADED) {
            load(node);
        }
    }

    public void refresh(WorkspaceTreeNode node) {
        if (!node.isExpandable()) {
            return;
        }
        node.invalidate();
        node.setExpanded(true);
        listener.onTreeChanged();
        load(node);
    }

    public void retry(WorkspaceTreeNode node) {
        refresh(node);
    }

    public void destroy() {
        destroyed = true;
        for (WorkspaceTreeNode root : roots) {
            invalidateRecursively(root);
        }
    }

    private void load(WorkspaceTreeNode node) {
        final long generation = node.beginLoad();
        listener.onTreeChanged();
        executor.execute(() -> {
            try {
                List<WorkspaceDocument> documents = provider.listChildren(
                        node.getRootUri(),
                        node.isRoot() ? null : node.getDocument().getUri());
                List<WorkspaceTreeNode> children = new ArrayList<>();
                for (WorkspaceDocument document : documents) {
                    children.add(WorkspaceTreeNode.document(
                            node.getRootUri(), document));
                }
                resultPoster.post(() -> applyResult(node, generation, children, null));
            } catch (IOException exception) {
                resultPoster.post(() -> applyResult(
                        node, generation, Collections.emptyList(), exception));
            }
        });
    }

    private void applyResult(
            WorkspaceTreeNode node,
            long generation,
            List<WorkspaceTreeNode> children,
            IOException error
    ) {
        if (destroyed || !node.isCurrent(generation) || !contains(node)) {
            return;
        }
        if (error == null) {
            node.setChildren(children);
        } else {
            node.setUnavailable();
        }
        listener.onTreeChanged();
    }

    private boolean contains(WorkspaceTreeNode candidate) {
        for (WorkspaceTreeNode root : roots) {
            if (contains(root, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(
            WorkspaceTreeNode current,
            WorkspaceTreeNode candidate
    ) {
        if (current == candidate) {
            return true;
        }
        for (WorkspaceTreeNode child : current.getChildren()) {
            if (contains(child, candidate)) {
                return true;
            }
        }
        return false;
    }

    private void invalidateRecursively(WorkspaceTreeNode node) {
        for (WorkspaceTreeNode child : node.getChildren()) {
            invalidateRecursively(child);
        }
        node.invalidate();
    }
}
