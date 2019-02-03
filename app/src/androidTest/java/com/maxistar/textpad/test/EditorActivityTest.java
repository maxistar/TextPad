package com.maxistar.textpad.test;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import com.maxistar.textpad.EditorActivity;
import com.maxistar.textpad.R;
import com.maxistar.textpad.TPApplication;
import com.maxistar.textpad.TPStrings;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import androidx.test.espresso.FailureHandler;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EditorActivityTest {

    @Rule
    public ActivityTestRule<EditorActivity> mActivityRule =
            new ActivityTestRule<>(EditorActivity.class);

    @Test
    public void listGoesOverTheFold() {
        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((TPApplication.getApplication().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText("newfile.txt")).check(matches(isDisplayed()));
    }

    @Test
    public void listSaveText() {
        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((TPApplication.getApplication().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());


        String textExample = "some new text";

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((TPApplication.getApplication().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        long time = (new Date()).getTime();
        String filename = "file" + String.valueOf(time) + ".txt";

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText((TPApplication.getApplication().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        String rootPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = rootPath + "/" + filename;
        String content = getFile(filePath);

        Assert.assertEquals(content, textExample);
    }

    @Test
    public void rewriteConfirmationText() {
        String textExample = "some new text";
        String oldTextExample = "some old content";

        long time = (new Date()).getTime();
        String filename = "file" + String.valueOf(time) + ".txt";

        putFile(Environment.getExternalStorageDirectory().getPath() + "/" + filename, oldTextExample);

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((TPApplication.getApplication().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((TPApplication.getApplication().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText((TPApplication.getApplication().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText((TPApplication.getApplication().getString(R.string.No))))
                .check(matches(isDisplayed()))
                .perform(click());


        String rootPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = rootPath + "/" + filename;
        String content = getFile(filePath);

        Assert.assertEquals(oldTextExample, content);
    }


    @Test
    public void rewriteConfirmationYesText() {
        String textExample = "some new text";
        String oldTextExample = "some old content";

        long time = (new Date()).getTime();
        String filename = "file" + String.valueOf(time) + ".txt";

        putFile(Environment.getExternalStorageDirectory().getPath() + "/" + filename, oldTextExample);

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((TPApplication.getApplication().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((TPApplication.getApplication().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText((TPApplication.getApplication().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText((TPApplication.getApplication().getString(R.string.Yes))))
                .check(matches(isDisplayed()))
                .perform(click());


        String rootPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = rootPath + "/" + filename;
        String content = getFile(filePath);

        Assert.assertEquals(textExample, content);
    }

    private void putFile(String filePath, String data) {
        try {
            Files.write( Paths.get(filePath), data.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
        }
    }


    private String getFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
        } catch (IOException e) {
        }
        return "";
    }

    private void gotToDocumentsFolder() {
        while(hasText("..")) {
            onView(withText(TPStrings.FOLDER_UP)).check(matches(isDisplayed())).perform(click());
        }

        onView(withText(("emulated")))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    private boolean hasText(String s) {
        final boolean[] isDisplayed = {true};
        onView(withText(s))
                .withFailureHandler(new FailureHandler() {
                    @Override
                    public void handle(Throwable error, org.hamcrest.Matcher<View> viewMatcher) {
                        isDisplayed[0] = false;
                    }
                })
                .check(matches(isDisplayed()));
        return isDisplayed[0];
    }

    public static ViewAction setTextInTextView(final String value){
        return new ViewAction() {
            @SuppressWarnings("unchecked")
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
