package com.maxistar.textpad.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.maxistar.textpad.R;
import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.recovery.RecoveryRepository;
import com.maxistar.textpad.service.SettingsService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class EditorNavigationTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        clearRecovery();
        setEditorPreferences(false, false);
    }

    @After
    public void tearDown() {
        clearRecovery();
        setEditorPreferences(false, true);
    }

    @Test
    public void twoFingerPanRoutesAxesInStandardScrolling() {
        setEditorPreferences(false, false);

        try (ActivityScenario<EditorActivity> scenario = launchWithContent(longDocument())) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                ScrollView scrollView = activity.findViewById(R.id.vscroll);
                assertNotNull(scrollView);
                resetScroll(editor, scrollView);

                performCompletedPan(editor, 600, 600, 800, 800, 500, 500, 700, 700);

                assertEquals(100, editor.getScrollX());
                assertEquals(0, editor.getScrollY());
                assertEquals(100, scrollView.getScrollY());
            });
        }
    }

    @Test
    public void twoFingerPanRoutesBothAxesInSimpleScrolling() {
        setEditorPreferences(true, false);

        try (ActivityScenario<EditorActivity> scenario = launchWithContent(longDocument())) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                assertNotNull(activity.findViewById(R.id.linear_layout));
                resetScroll(editor, null);

                performCompletedPan(editor, 600, 600, 800, 800, 500, 500, 700, 700);

                assertEquals(100, editor.getScrollX());
                assertEquals(100, editor.getScrollY());
            });
        }
    }

    @Test
    public void pointerReleaseStopsPanBeforeRemainingPointerMoves() {
        setEditorPreferences(true, false);

        try (ActivityScenario<EditorActivity> scenario = launchWithContent(longDocument())) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                resetScroll(editor, null);
                long downTime = SystemClock.uptimeMillis();

                dispatch(editor, event(downTime, downTime, MotionEvent.ACTION_DOWN,
                        point(300, 300)));
                dispatch(editor, event(downTime, downTime + 10,
                        pointerAction(MotionEvent.ACTION_POINTER_DOWN, 1),
                        point(300, 300), point(500, 500)));
                dispatch(editor, event(downTime, downTime + 20, MotionEvent.ACTION_MOVE,
                        point(300, 300), point(400, 400)));
                dispatch(editor, event(downTime, downTime + 30,
                        pointerAction(MotionEvent.ACTION_POINTER_UP, 1),
                        point(300, 300), point(400, 400)));

                int scrollX = editor.getScrollX();
                int scrollY = editor.getScrollY();
                dispatch(editor, event(downTime, downTime + 40, MotionEvent.ACTION_MOVE,
                        point(300, 300)));

                assertEquals(scrollX, editor.getScrollX());
                assertEquals(scrollY, editor.getScrollY());
                dispatch(editor, event(downTime, downTime + 50, MotionEvent.ACTION_UP,
                        point(300, 300)));
            });
        }
    }

    @Test
    public void finalUpAfterPanDoesNotMoveCaret() {
        setEditorPreferences(true, false);

        try (ActivityScenario<EditorActivity> scenario = launchWithContent(longDocument())) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                resetScroll(editor, null);
                editor.setSelection(0);
                long downTime = SystemClock.uptimeMillis();

                dispatch(editor, event(downTime, downTime, MotionEvent.ACTION_DOWN,
                        point(300, 300)));
                dispatch(editor, event(downTime, downTime + 10,
                        pointerAction(MotionEvent.ACTION_POINTER_DOWN, 1),
                        point(300, 300), point(500, 500)));
                dispatch(editor, event(downTime, downTime + 20, MotionEvent.ACTION_MOVE,
                        point(250, 250), point(450, 450)));
                dispatch(editor, event(downTime, downTime + 30,
                        pointerAction(MotionEvent.ACTION_POINTER_UP, 1),
                        point(250, 250), point(450, 450)));

                int selectionStart = editor.getSelectionStart();
                int selectionEnd = editor.getSelectionEnd();
                dispatch(editor, event(downTime, downTime + 40, MotionEvent.ACTION_UP,
                        point(450, 450)));

                assertEquals(selectionStart, editor.getSelectionStart());
                assertEquals(selectionEnd, editor.getSelectionEnd());
            });
        }
    }

    @Test
    public void cancelAndThirdPointerTransitionsStopPan() {
        setEditorPreferences(true, false);

        try (ActivityScenario<EditorActivity> scenario = launchWithContent(longDocument())) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                resetScroll(editor, null);

                long cancelDownTime = SystemClock.uptimeMillis();
                startPanWithOffset(editor, cancelDownTime);
                dispatch(editor, event(cancelDownTime, cancelDownTime + 30,
                        MotionEvent.ACTION_CANCEL, point(300, 300), point(400, 400)));
                int scrollAfterCancelX = editor.getScrollX();
                int scrollAfterCancelY = editor.getScrollY();
                dispatch(editor, event(cancelDownTime, cancelDownTime + 40,
                        MotionEvent.ACTION_MOVE, point(300, 300)));
                assertEquals(scrollAfterCancelX, editor.getScrollX());
                assertEquals(scrollAfterCancelY, editor.getScrollY());

                resetScroll(editor, null);
                long thirdPointerDownTime = SystemClock.uptimeMillis() + 100;
                startPanWithOffset(editor, thirdPointerDownTime);
                dispatch(editor, event(thirdPointerDownTime, thirdPointerDownTime + 30,
                        pointerAction(MotionEvent.ACTION_POINTER_DOWN, 2),
                        point(300, 300), point(400, 400), point(700, 700)));
                int scrollAfterThirdPointerX = editor.getScrollX();
                int scrollAfterThirdPointerY = editor.getScrollY();
                dispatch(editor, event(thirdPointerDownTime, thirdPointerDownTime + 40,
                        MotionEvent.ACTION_MOVE,
                        point(300, 300), point(400, 400), point(700, 700)));
                assertEquals(scrollAfterThirdPointerX, editor.getScrollX());
                assertEquals(scrollAfterThirdPointerY, editor.getScrollY());
                dispatch(editor, event(thirdPointerDownTime, thirdPointerDownTime + 50,
                        MotionEvent.ACTION_CANCEL,
                        point(300, 300), point(400, 400), point(700, 700)));
            });
        }
    }

    @Test
    public void panClampsShortAndLongContentBounds() {
        setEditorPreferences(true, false);

        try (ActivityScenario<EditorActivity> shortScenario = launchWithContent("short")) {
            shortScenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                resetScroll(editor, null);
                performCompletedPan(editor, 600, 600, 800, 800, 500, 500, 700, 700);
                assertEquals(0, editor.getScrollX());
                assertEquals(0, editor.getScrollY());
            });
        }

        try (ActivityScenario<EditorActivity> longScenario = launchWithContent(longDocument())) {
            longScenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                int maxX = maxHorizontalScroll(editor);
                int maxY = Math.max(0, editor.getLayout().getHeight()
                        + editor.getTotalPaddingTop() + editor.getTotalPaddingBottom()
                        - editor.getHeight());
                assertTrue(maxX > 0);
                assertTrue(maxY > 0);

                editor.scrollTo(maxX, maxY);
                performCompletedPan(editor, 600, 600, 800, 800, 500, 500, 700, 700);
                assertEquals(maxX, editor.getScrollX());
                assertEquals(maxY, editor.getScrollY());

                performCompletedPan(editor, 100, 100, 200, 200,
                        maxX + 300, maxY + 300, maxX + 400, maxY + 400);
                assertEquals(0, editor.getScrollX());
                assertEquals(0, editor.getScrollY());
            });
        }
    }

    @Test
    public void horizontalPanBoundsAreCachedAndInvalidatedByTextChanges() {
        setEditorPreferences(true, false);

        try (ActivityScenario<EditorActivity> scenario = launchWithContent(longDocument())) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                resetScroll(editor, null);

                int initialCalculations = activity.getHorizontalScrollBoundsCalculationCountForTests();
                performCompletedPan(editor, 600, 600, 800, 800, 500, 500, 700, 700);
                int afterFirstPan = activity.getHorizontalScrollBoundsCalculationCountForTests();
                assertEquals(initialCalculations + 1, afterFirstPan);

                resetScroll(editor, null);
                performCompletedPan(editor, 600, 600, 800, 800, 500, 500, 700, 700);
                assertEquals(afterFirstPan,
                        activity.getHorizontalScrollBoundsCalculationCountForTests());

                editor.setText(longDocument() + "extra text invalidates horizontal bounds");
            });
            waitForIdle();
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                resetScroll(editor, null);
                int beforeInvalidatedPan = activity.getHorizontalScrollBoundsCalculationCountForTests();
                performCompletedPan(editor, 600, 600, 800, 800, 500, 500, 700, 700);
                assertEquals(beforeInvalidatedPan + 1,
                        activity.getHorizontalScrollBoundsCalculationCountForTests());
            });
        }
    }

    @Test
    public void goToMenuStateAndDestinationsWorkInBothLayouts() {
        verifyGoToNavigation(false);
        verifyGoToNavigation(true);
    }

    private void verifyGoToNavigation(boolean simpleScrolling) {
        setEditorPreferences(simpleScrolling, true);
        try (ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                Menu emptyMenu = inflateEditorMenu(activity, editor);
                activity.onPrepareOptionsMenu(emptyMenu);
                assertFalse(emptyMenu.findItem(R.id.menu_document_go_to).isEnabled());

                editor.setText(longDocument());
                editor.setSelection(0);
            });
            waitForIdle();

            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                Menu menu = inflateEditorMenu(activity, editor);
                activity.onPrepareOptionsMenu(menu);
                MenuItem goTo = menu.findItem(R.id.menu_document_go_to);
                assertTrue(goTo.isEnabled());
                assertTrue(goTo.hasSubMenu());
                assertNotNull(menu.findItem(R.id.menu_document_go_to_beginning));
                assertNotNull(menu.findItem(R.id.menu_document_go_to_end));

                activity.onOptionsItemSelected(menu.findItem(R.id.menu_document_go_to_end));
            });
            waitForIdle();
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                assertEquals(editor.length(), editor.getSelectionStart());
                assertCaretVisible(editor);
                if (simpleScrolling) {
                    assertTrue(editor.getScrollY() > 0);
                } else {
                    ScrollView scrollView = activity.findViewById(R.id.vscroll);
                    assertTrue(scrollView.getScrollY() > 0);
                }

                Menu menu = inflateEditorMenu(activity, editor);
                activity.onOptionsItemSelected(menu.findItem(R.id.menu_document_go_to_beginning));
            });
            waitForIdle();
            scenario.onActivity(activity -> {
                EditText editor = activity.findViewById(R.id.editText1);
                assertEquals(0, editor.getSelectionStart());
                assertCaretVisible(editor);
            });
        }
    }

    private ActivityScenario<EditorActivity> launchWithContent(String content) {
        ActivityScenario<EditorActivity> scenario = ActivityScenario.launch(EditorActivity.class);
        scenario.onActivity(activity -> {
            EditText editor = activity.findViewById(R.id.editText1);
            editor.setText(content);
            editor.setSelection(0);
            editor.requestFocus();
        });
        waitForIdle();
        return scenario;
    }

    private static Menu inflateEditorMenu(EditorActivity activity, EditText anchor) {
        PopupMenu popupMenu = new PopupMenu(activity, anchor);
        activity.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        return popupMenu.getMenu();
    }

    private static void assertCaretVisible(EditText editor) {
        assertNotNull(editor.getLayout());
        int line = editor.getLayout().getLineForOffset(editor.getSelectionStart());
        int[] editorLocation = new int[2];
        editor.getLocationOnScreen(editorLocation);
        int caretTop = editorLocation[1] + editor.getTotalPaddingTop()
                + editor.getLayout().getLineTop(line) - editor.getScrollY();
        int caretBottom = editorLocation[1] + editor.getTotalPaddingTop()
                + editor.getLayout().getLineBottom(line) - editor.getScrollY();
        Rect visible = new Rect();
        assertTrue(editor.getGlobalVisibleRect(visible));
        assertTrue("Caret top must be visible", caretTop >= visible.top);
        assertTrue("Caret bottom must be visible", caretBottom <= visible.bottom);
    }

    private static int maxHorizontalScroll(EditText editor) {
        float maxLineWidth = 0;
        for (int line = 0; line < editor.getLayout().getLineCount(); line++) {
            maxLineWidth = Math.max(maxLineWidth, editor.getLayout().getLineWidth(line));
        }
        int contentWidth = (int) Math.ceil(maxLineWidth)
                + editor.getTotalPaddingLeft() + editor.getTotalPaddingRight();
        return Math.max(0, contentWidth - editor.getWidth());
    }

    private static void resetScroll(EditText editor, ScrollView scrollView) {
        editor.setSelection(0);
        editor.scrollTo(0, 0);
        if (scrollView != null) {
            scrollView.scrollTo(0, 0);
        }
    }

    private static void startPanWithOffset(EditText editor, long downTime) {
        dispatch(editor, event(downTime, downTime, MotionEvent.ACTION_DOWN,
                point(300, 300)));
        dispatch(editor, event(downTime, downTime + 10,
                pointerAction(MotionEvent.ACTION_POINTER_DOWN, 1),
                point(300, 300), point(500, 500)));
        dispatch(editor, event(downTime, downTime + 20, MotionEvent.ACTION_MOVE,
                point(300, 300), point(400, 400)));
    }

    private static void performCompletedPan(
            EditText editor,
            float startX0,
            float startY0,
            float startX1,
            float startY1,
            float endX0,
            float endY0,
            float endX1,
            float endY1
    ) {
        long downTime = SystemClock.uptimeMillis();
        dispatch(editor, event(downTime, downTime, MotionEvent.ACTION_DOWN,
                point(startX0, startY0)));
        dispatch(editor, event(downTime, downTime + 10,
                pointerAction(MotionEvent.ACTION_POINTER_DOWN, 1),
                point(startX0, startY0), point(startX1, startY1)));
        dispatch(editor, event(downTime, downTime + 20, MotionEvent.ACTION_MOVE,
                point(endX0, endY0), point(endX1, endY1)));
        dispatch(editor, event(downTime, downTime + 30,
                pointerAction(MotionEvent.ACTION_POINTER_UP, 1),
                point(endX0, endY0), point(endX1, endY1)));
        dispatch(editor, event(downTime, downTime + 40, MotionEvent.ACTION_UP,
                point(endX0, endY0)));
    }

    private static int pointerAction(int action, int pointerIndex) {
        return action | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }

    private static float[] point(float x, float y) {
        return new float[]{x, y};
    }

    private static MotionEvent event(
            long downTime,
            long eventTime,
            int action,
            float[]... points
    ) {
        MotionEvent.PointerProperties[] properties =
                new MotionEvent.PointerProperties[points.length];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[points.length];
        for (int index = 0; index < points.length; index++) {
            MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
            pointerProperties.id = index;
            pointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[index] = pointerProperties;

            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            pointerCoords.x = points[index][0];
            pointerCoords.y = points[index][1];
            pointerCoords.pressure = 1;
            pointerCoords.size = 1;
            coordinates[index] = pointerCoords;
        }
        return MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                points.length,
                properties,
                coordinates,
                0,
                0,
                1,
                1,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0
        );
    }

    private static void dispatch(EditText editor, MotionEvent event) {
        try {
            editor.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }

    private static String longDocument() {
        StringBuilder document = new StringBuilder();
        for (int line = 0; line < 120; line++) {
            document.append("Line ").append(line).append(' ');
            for (int column = 0; column < 120; column++) {
                document.append("0123456789");
            }
            document.append('\n');
        }
        return document.toString();
    }

    private void setEditorPreferences(boolean simpleScrolling, boolean autoWrapping) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(SettingsService.SETTING_USE_SIMPLE_SCROLLING, simpleScrolling)
                .putBoolean(SettingsService.SETTING_AUTO_WRAPPING, autoWrapping)
                .putBoolean(SettingsService.SETTING_OPEN_LAST_FILE, false)
                .commit();
        ServiceLocator.getInstance().getSettingsService(context).reloadSettings(context);
    }

    private static void waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void clearRecovery() {
        context.getSharedPreferences("editor_recovery", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        deleteRecursively(new RecoveryRepository(context).getDirectoryForTests());
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
