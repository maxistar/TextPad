package com.maxistar.textpad.workspace;

import android.content.ActivityNotFoundException;
import android.content.Intent;

public final class WorkspaceExternalDispatcher {
    public interface Launcher {
        void launch(Intent intent) throws ActivityNotFoundException;
    }

    public boolean dispatch(Intent intent, Launcher launcher) {
        try {
            launcher.launch(intent);
            return true;
        } catch (ActivityNotFoundException exception) {
            return false;
        }
    }
}
