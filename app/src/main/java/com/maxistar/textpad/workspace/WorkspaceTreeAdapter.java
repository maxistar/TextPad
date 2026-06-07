package com.maxistar.textpad.workspace;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.maxistar.textpad.R;

import java.util.Collections;
import java.util.List;

public final class WorkspaceTreeAdapter extends BaseAdapter {
    private final Context context;
    private final WorkspaceFileClassifier classifier = new WorkspaceFileClassifier();
    private List<WorkspaceTreeRow> rows = Collections.emptyList();
    private String selectedUri = "";

    public WorkspaceTreeAdapter(Context context) {
        this.context = context;
    }

    public void submit(List<WorkspaceTreeRow> value, String selectedUri) {
        rows = value;
        this.selectedUri = selectedUri == null ? "" : selectedUri;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public WorkspaceTreeRow getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getStableId().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RowHolder holder;
        if (convertView == null) {
            holder = createRow();
            convertView = holder.container;
            convertView.setTag(holder);
        } else {
            holder = (RowHolder) convertView.getTag();
        }
        WorkspaceTreeRow row = getItem(position);
        WorkspaceTreeNode node = row.getNode();
        int basePadding = dp(12);
        holder.container.setPadding(
                basePadding + dp(20) * row.getDepth(), 0, basePadding, 0);
        holder.icon.setImageResource(iconFor(node));
        holder.label.setTypeface(null, node.isRoot()
                ? Typeface.BOLD : Typeface.NORMAL);
        holder.label.setText(label(node));
        boolean selected = node.getDocument() != null
                && selectedUri.equals(node.getDocument().getUri());
        holder.container.setActivated(selected);
        holder.container.setAlpha(
                node.getState() == WorkspaceTreeNode.State.UNAVAILABLE ? 0.55f : 1f);
        return convertView;
    }

    private CharSequence label(WorkspaceTreeNode node) {
        switch (node.getState()) {
            case LOADING:
                return node.getDisplayName() + " · "
                        + context.getString(R.string.workspace_loading);
            case EMPTY:
                return node.getDisplayName() + " · "
                        + context.getString(R.string.workspace_empty_directory);
            case UNAVAILABLE:
                return node.getDisplayName() + " · "
                        + context.getString(R.string.workspace_unavailable);
            default:
                return node.getDisplayName();
        }
    }

    private int iconFor(WorkspaceTreeNode node) {
        if (node.isDirectory()) {
            return R.drawable.folder;
        }
        WorkspaceDocument document = node.getDocument();
        if (classifier.isInternalText(document)) {
            return R.drawable.file;
        }
        String mimeType = document.getMimeType();
        if (mimeType.startsWith("image/")) {
            return android.R.drawable.ic_menu_gallery;
        }
        if (mimeType.startsWith("audio/") || mimeType.startsWith("video/")) {
            return android.R.drawable.ic_media_play;
        }
        return android.R.drawable.ic_menu_view;
    }

    private RowHolder createRow() {
        LinearLayout container = new LinearLayout(context);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setBackgroundResource(
                android.R.drawable.list_selector_background);

        ImageView icon = new ImageView(context);
        container.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView label = new TextView(context);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setSingleLine(true);
        label.setTextAppearance(context, android.R.style.TextAppearance_Medium);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        labelParams.setMarginStart(dp(12));
        container.addView(label, labelParams);
        return new RowHolder(container, icon, label);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources()
                .getDisplayMetrics().density);
    }

    private static final class RowHolder {
        private final LinearLayout container;
        private final ImageView icon;
        private final TextView label;

        private RowHolder(
                LinearLayout container,
                ImageView icon,
                TextView label
        ) {
            this.container = container;
            this.icon = icon;
            this.label = label;
        }
    }
}
