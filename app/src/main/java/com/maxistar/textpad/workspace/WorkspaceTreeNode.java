package com.maxistar.textpad.workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorkspaceTreeNode {
    public enum State {
        NOT_LOADED,
        LOADING,
        LOADED,
        EMPTY,
        UNAVAILABLE
    }

    private final String id;
    private final String rootUri;
    private final String displayName;
    private final WorkspaceDocument document;
    private final boolean root;
    private boolean expanded;
    private State state;
    private long generation;
    private List<WorkspaceTreeNode> children = Collections.emptyList();

    public static WorkspaceTreeNode root(WorkspaceRoot root) {
        WorkspaceTreeNode node = new WorkspaceTreeNode(
                "root:" + root.getUri(),
                root.getUri(),
                root.getDisplayName(),
                null,
                true
        );
        if (!root.isAvailable()) {
            node.state = State.UNAVAILABLE;
        }
        return node;
    }

    public static WorkspaceTreeNode document(
            String rootUri,
            WorkspaceDocument document
    ) {
        return new WorkspaceTreeNode(
                "document:" + document.getUri(),
                rootUri,
                document.getDisplayName(),
                document,
                false
        );
    }

    private WorkspaceTreeNode(
            String id,
            String rootUri,
            String displayName,
            WorkspaceDocument document,
            boolean root
    ) {
        this.id = id;
        this.rootUri = rootUri;
        this.displayName = displayName;
        this.document = document;
        this.root = root;
        this.state = isExpandable() ? State.NOT_LOADED : State.LOADED;
    }

    public String getId() {
        return id;
    }

    public String getRootUri() {
        return rootUri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public WorkspaceDocument getDocument() {
        return document;
    }

    public boolean isRoot() {
        return root;
    }

    public boolean isDirectory() {
        return root || (document != null && document.isDirectory());
    }

    public boolean isExpandable() {
        return isDirectory();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public State getState() {
        return state;
    }

    public List<WorkspaceTreeNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    long beginLoad() {
        state = State.LOADING;
        return ++generation;
    }

    boolean isCurrent(long requestGeneration) {
        return generation == requestGeneration;
    }

    void setChildren(List<WorkspaceTreeNode> value) {
        children = new ArrayList<>(value);
        state = children.isEmpty() ? State.EMPTY : State.LOADED;
    }

    void setUnavailable() {
        state = State.UNAVAILABLE;
    }

    void invalidate() {
        generation++;
        state = State.NOT_LOADED;
        children = Collections.emptyList();
    }
}
