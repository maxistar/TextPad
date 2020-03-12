package com.maxistar.textpad.utils;

import android.app.Activity;

public class System {
    static public void exitFromApp(Activity activity) {
        activity.finish();
        java.lang.System.exit(0);
    }
}
