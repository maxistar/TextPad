package com.maxistar.textpad.workspace;

public final class WorkspaceTreeRow {
    private final WorkspaceTreeNode node;
    private final int depth;

    public WorkspaceTreeRow(WorkspaceTreeNode node, int depth) {
        this.node = node;
        this.depth = depth;
    }

    public WorkspaceTreeNode getNode() {
        return node;
    }

    public int getDepth() {
        return depth;
    }

    public String getStableId() {
        return node.getId();
    }
}
