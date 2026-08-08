package com.maxistar.textpad.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.service.SettingsService;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EditorLayoutTest {

    @After
    public void restoreStandardScrolling() {
        setSimpleScrolling(false);
    }

    @Test
    public void standardScrollingStartsBelowToolbar() {
        setSimpleScrolling(false);

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.vscroll));
                assertNull(activity.findViewById(R.id.linear_layout));
                assertEditorStartsBelowToolbar(activity);
            });
        }
    }

    @Test
    public void simpleScrollingStartsBelowToolbarWithoutScrollView() {
        setSimpleScrolling(true);

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.linear_layout));
                assertNull(activity.findViewById(R.id.vscroll));
                assertEditorStartsBelowToolbar(activity);
            });
        }
    }

    private void assertEditorStartsBelowToolbar(EditorActivity activity) {
        View toolbar = activity.findViewById(R.id.editor_toolbar);
        View editor = activity.findViewById(R.id.editText1);
        int[] toolbarLocation = new int[2];
        int[] editorLocation = new int[2];

        toolbar.getLocationOnScreen(toolbarLocation);
        editor.getLocationOnScreen(editorLocation);

        assertEquals(toolbarLocation[1] + toolbar.getHeight(), editorLocation[1]);
    }

    private void setSimpleScrolling(boolean enabled) {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(SettingsService.SETTING_USE_SIMPLE_SCROLLING, enabled)
                .commit();
        ServiceLocator.getInstance().getSettingsService(context).reloadSettings(context);
    }
}
