package com.maxistar.textpad.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.maxistar.textpad.R;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;

import com.maxistar.textpad.activities.EditorActivity;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
          // BasicActivityTest.java
public class BasicActivityTest {


    @Rule
    public IntentsTestRule<EditorActivity> intentsTestRule =
            new IntentsTestRule<>(EditorActivity.class);

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.maxistar.textpad", appContext.getPackageName());
    }


    @Test
    public void typeText() {

        onView(withId(R.id.editText1))
                .perform(setTextInTextView("some text text text"));

        // Context of the app under test.
        // Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // assertEquals("me.maxistar.testinteractivetestingsetup", appContext.getPackageName());
    }

    public static ViewAction setTextInTextView(final String value){
        return new ViewAction() {
            @Override
            public org.hamcrest.Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
//                                            ^^^^^^^^^^^^^^^^^^^
// To check that the found view is TextView or it's subclass like EditText
// so it will work for TextView and it's descendants
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextView) view).setText(value);
            }

            @Override
            public String getDescription() {
                return "replace text";
            }
        };
    }
}
