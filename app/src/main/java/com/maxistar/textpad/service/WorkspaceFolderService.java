package com.maxistar.textpad.service;

import com.maxistar.textpad.workspace.WorkspaceRegistryCodec;
import com.maxistar.textpad.workspace.WorkspaceRegistryStore;
import com.maxistar.textpad.workspace.WorkspaceRoot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorkspaceFolderService {
    private final WorkspaceRegistryStore store;
    private final WorkspaceRegistryCodec codec;
    private List<WorkspaceRoot> roots;

    public WorkspaceFolderService(
            WorkspaceRegistryStore store,
            WorkspaceRegistryCodec codec
    ) {
        this.store = store;
        this.codec = codec;
    }

    public synchronized List<WorkspaceRoot> getRoots() {
        load();
        return Collections.unmodifiableList(new ArrayList<>(roots));
    }

    public synchronized boolean add(WorkspaceRoot root) {
        load();
        if (indexOf(root.getUri()) >= 0) {
            return false;
        }
        roots.add(root);
        save();
        return true;
    }

    public synchronized boolean replace(String previousUri, WorkspaceRoot replacement) {
        load();
        int index = indexOf(previousUri);
        int replacementIndex = indexOf(replacement.getUri());
        if (index < 0 || (replacementIndex >= 0 && replacementIndex != index)) {
            return false;
        }
        roots.set(index, replacement);
        save();
        return true;
    }

    public synchronized boolean remove(String uri) {
        load();
        int index = indexOf(uri);
        if (index < 0) {
            return false;
        }
        roots.remove(index);
        save();
        return true;
    }

    public synchronized void updateAccess(
            String uri,
            boolean available,
            boolean readable,
            boolean writable
    ) {
        load();
        int index = indexOf(uri);
        if (index >= 0) {
            roots.set(index, roots.get(index).withAccess(available, readable, writable));
        }
    }

    private void load() {
        if (roots == null) {
            roots = new ArrayList<>(codec.decode(store.read()));
        }
    }

    private int indexOf(String uri) {
        for (int index = 0; index < roots.size(); index++) {
            if (roots.get(index).getUri().equals(uri)) {
                return index;
            }
        }
        return -1;
    }

    private void save() {
        store.write(codec.encode(roots));
    }
}
