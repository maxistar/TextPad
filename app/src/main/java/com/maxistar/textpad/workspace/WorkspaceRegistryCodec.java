package com.maxistar.textpad.workspace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorkspaceRegistryCodec {
    private static final int CURRENT_VERSION = 1;

    public String encode(List<WorkspaceRoot> roots) {
        JSONObject registry = new JSONObject();
        JSONArray items = new JSONArray();
        try {
            registry.put("version", CURRENT_VERSION);
            for (WorkspaceRoot root : roots) {
                JSONObject item = new JSONObject();
                item.put("uri", root.getUri());
                item.put("name", root.getDisplayName());
                items.put(item);
            }
            registry.put("roots", items);
            return registry.toString();
        } catch (JSONException exception) {
            return "{\"version\":1,\"roots\":[]}";
        }
    }

    public List<WorkspaceRoot> decode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JSONObject registry = new JSONObject(value);
            if (registry.optInt("version", -1) != CURRENT_VERSION) {
                return Collections.emptyList();
            }
            JSONArray items = registry.optJSONArray("roots");
            if (items == null) {
                return Collections.emptyList();
            }
            List<WorkspaceRoot> roots = new ArrayList<>();
            for (int index = 0; index < items.length(); index++) {
                JSONObject item = items.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                String uri = item.optString("uri", "");
                if (uri.isEmpty() || containsUri(roots, uri)) {
                    continue;
                }
                roots.add(new WorkspaceRoot(
                        uri,
                        item.optString("name", ""),
                        true,
                        true,
                        false
                ));
            }
            return roots;
        } catch (JSONException exception) {
            return Collections.emptyList();
        }
    }

    private static boolean containsUri(List<WorkspaceRoot> roots, String uri) {
        for (WorkspaceRoot root : roots) {
            if (root.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }
}
