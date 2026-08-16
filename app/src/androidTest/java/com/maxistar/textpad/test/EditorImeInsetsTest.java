package com.maxistar.textpad.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.service.SettingsService;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class EditorImeInsetsTest {
    private static final long IME_TIMEOUT_MILLIS = 5000;

    @After
    public void restoreStandardScrolling() {
        setSimpleScrolling(false);
    }

    @Test
    public void bothLayoutsKeepCaretAboveImeAndRestoreViewport() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM);
        setSimpleScrolling(false);

        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            verifyCurrentLayout(scenario, R.id.vscroll);

            setSimpleScrolling(true);
            scenario.onActivity(this::clearChangedStateForLayoutRecreation);
            scenario.recreate();
            verifyCurrentLayout(scenario, R.id.linear_layout);

            hideImeAndAssertViewportRestored(scenario);
        }
    }

    private void verifyCurrentLayout(
            ActivityScenario<EditorActivity> scenario,
            int expectedLayoutViewId
    ) {
        scenario.onActivity(activity -> {
            assertTrue("Expected editor layout must be active",
                    activity.findViewById(expectedLayoutViewId) != null);
            EditText editor = activity.findViewById(R.id.editText1);
            editor.setText(longDocument());
            editor.setSelection(editor.length());
            editor.requestFocus();
            editor.performClick();
            showIme(activity, editor);
        });

        waitForImeVisibility(scenario, true);
        onView(withId(R.id.editText1)).perform(typeText("text typed with the keyboard visible"));
        scenario.onActivity(activity -> {
            View editorRoot = activity.findViewById(R.id.editor_root);
            EditText editor = activity.findViewById(R.id.editText1);
            WindowInsets windowInsets = editorRoot.getRootWindowInsets();
            Insets bars = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
            );
            Insets ime = windowInsets.getInsets(WindowInsets.Type.ime());

            assertEquals(Math.max(bars.bottom, ime.bottom), editorRoot.getPaddingBottom());
            assertCaretAboveIme(activity, editor, ime.bottom);
        });
    }

    private void hideImeAndAssertViewportRestored(ActivityScenario<EditorActivity> scenario) {
        scenario.onActivity(activity -> {
            EditText editor = activity.findViewById(R.id.editText1);
            InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(editor.getWindowToken(), 0);
        });

        waitForImeVisibility(scenario, false);
        scenario.onActivity(activity -> {
            View editorRoot = activity.findViewById(R.id.editor_root);
            WindowInsets windowInsets = editorRoot.getRootWindowInsets();
            Insets bars = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
            );
            assertEquals(bars.bottom, editorRoot.getPaddingBottom());
        });
    }

    private void assertCaretAboveIme(EditorActivity activity, EditText editor, int imeBottom) {
        int line = editor.getLayout().getLineForOffset(editor.getSelectionStart());
        int[] editorLocation = new int[2];
        int[] decorLocation = new int[2];
        editor.getLocationOnScreen(editorLocation);
        activity.getWindow().getDecorView().getLocationOnScreen(decorLocation);

        int caretBottom = editorLocation[1]
                + editor.getTotalPaddingTop()
                + editor.getLayout().getLineBottom(line)
                - editor.getScrollY();
        int imeTop = decorLocation[1]
                + activity.getWindow().getDecorView().getHeight()
                - imeBottom;
        Rect visibleEditor = new Rect();

        assertTrue("The focused editor must have a visible region", editor.getGlobalVisibleRect(visibleEditor));
        assertTrue("The editor viewport must end above the IME: visibleBottom="
                + visibleEditor.bottom + ", imeTop=" + imeTop, visibleEditor.bottom <= imeTop);
        assertTrue("The caret must remain inside the visible editor viewport: caretBottom="
                + caretBottom + ", visibleBottom=" + visibleEditor.bottom
                + ", editorTop=" + editorLocation[1] + ", scrollY=" + editor.getScrollY(),
                caretBottom <= visibleEditor.bottom);
    }

    private void waitForImeVisibility(
            ActivityScenario<EditorActivity> scenario,
            boolean expectedVisible
    ) {
        long deadline = SystemClock.uptimeMillis() + IME_TIMEOUT_MILLIS;
        while (SystemClock.uptimeMillis() < deadline) {
            AtomicBoolean matches = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                View editorRoot = activity.findViewById(R.id.editor_root);
                WindowInsets windowInsets = editorRoot.getRootWindowInsets();
                matches.set(windowInsets != null
                        && windowInsets.isVisible(WindowInsets.Type.ime()) == expectedVisible);
                if (expectedVisible && !matches.get() && activity.hasWindowFocus()) {
                    EditText editor = activity.findViewById(R.id.editText1);
                    editor.requestFocus();
                    showIme(activity, editor);
                }
            });
            if (matches.get()) {
                return;
            }
            SystemClock.sleep(100);
        }
        assertTrue("IME visibility did not become " + expectedVisible, false);
    }

    private void showIme(EditorActivity activity, EditText editor) {
        WindowInsetsController controller = editor.getWindowInsetsController();
        if (controller != null) {
            controller.show(WindowInsets.Type.ime());
        }
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.restartInput(editor);
        inputMethodManager.showSoftInput(editor, InputMethodManager.SHOW_FORCED);
    }

    private void clearChangedStateForLayoutRecreation(EditorActivity activity) {
        try {
            Field changed = EditorActivity.class.getDeclaredField("changed");
            changed.setAccessible(true);
            changed.setBoolean(activity, false);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to prepare editor layout recreation", exception);
        }
    }

    private String longDocument() {
        StringBuilder text = new StringBuilder();
        for (int line = 0; line < 120; line++) {
            text.append("Document line ").append(line).append('\n');
        }
        return text.toString();
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
