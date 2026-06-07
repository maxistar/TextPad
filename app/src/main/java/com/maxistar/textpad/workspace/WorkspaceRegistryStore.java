package com.maxistar.textpad.workspace;

public interface WorkspaceRegistryStore {
    String read();

    void write(String value);
}
