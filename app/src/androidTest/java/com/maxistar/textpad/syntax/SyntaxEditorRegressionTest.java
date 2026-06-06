package com.maxistar.textpad.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.maxistar.textpad.R;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.utils.EditTextUndoRedo;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SyntaxEditorRegressionTest {
    @Test
    public void renderingDoesNotDirtyTextOrEnterUndoHistory() {
        try (ActivityScenario<EditorActivity> scenario =
                     ActivityScenario.launch(EditorActivity.class)) {
            scenario.onActivity(activity -> {
                activity.clearFile();
                EditText editor = activity.findViewById(R.id.editText1);
                String cleanTitle = activity.getTitle().toString();

                editor.getText().setSpan(
                        new SyntaxSpan(0xff005cc5, 1),
                        0,
                        0,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                assertEquals("", editor.getText().toString());
                assertEquals(cleanTitle, activity.getTitle().toString());

                EditTextUndoRedo undoRedo = new EditTextUndoRedo(editor, activity);
                undoRedo.clearHistory();
                editor.append("text");
                assertTrue(undoRedo.getCanUndo());

                BackgroundColorSpan searchSpan = new BackgroundColorSpan(0xffffff00);
                editor.getText().setSpan(
                        searchSpan, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                editor.getText().setSpan(
                        new SyntaxSpan(0xff005cc5, 2),
                        0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                SyntaxHighlightController.removeSyntaxSpans(editor.getText(), null);

                assertEquals(1, editor.getText().getSpans(
                        0, editor.length(), BackgroundColorSpan.class).length);
                assertTrue(undoRedo.getCanUndo());
                undoRedo.undo();
                assertEquals("", editor.getText().toString());
                assertFalse(undoRedo.getCanUndo());
                undoRedo.disconnect();
            });
        }
    }
}
