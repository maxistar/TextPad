package com.maxistar.textpad.workspace;

import java.util.ArrayList;
import java.util.List;

public final class WorkspaceTreeFlattener {
    public List<WorkspaceTreeRow> flatten(List<WorkspaceTreeNode> roots) {
        List<WorkspaceTreeRow> rows = new ArrayList<>();
        for (WorkspaceTreeNode root : roots) {
            append(rows, root, 0);
        }
        return rows;
    }

    private void append(
            List<WorkspaceTreeRow> rows,
            WorkspaceTreeNode node,
            int depth
    ) {
        rows.add(new WorkspaceTreeRow(node, depth));
        if (!node.isExpanded()) {
            return;
        }
        for (WorkspaceTreeNode child : node.getChildren()) {
            append(rows, child, depth + 1);
        }
    }
}
