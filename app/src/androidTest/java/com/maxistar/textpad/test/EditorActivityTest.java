package com.maxistar.textpad.test;

import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import com.maxistar.textpad.EditorActivity;
import com.maxistar.textpad.R;
import com.maxistar.textpad.TPStrings;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
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
    /**
     * Check if the text is empty it to click on new menu item
     */
    public void listGoesOverTheFold() {
        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((mActivityRule.getActivity().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText("newfile.txt")).check(matches(isDisplayed()));
    }

    @Test
    /**
     *
     */
    public void listSaveText() {
        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((mActivityRule.getActivity().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());


        String textExample = "some new text";

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((mActivityRule.getActivity().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        long time = (new Date()).getTime();
        String filename = "file" + String.valueOf(time) + ".txt";

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText((mActivityRule.getActivity().getString(R.string.Save))))
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

        onView(withText((mActivityRule.getActivity().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((mActivityRule.getActivity().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText((mActivityRule.getActivity().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText((mActivityRule.getActivity().getString(R.string.No))))
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

        onView(withText((mActivityRule.getActivity().getString(R.string.New))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getTargetContext());

        onView(withText((mActivityRule.getActivity().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText((mActivityRule.getActivity().getString(R.string.Save))))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText((mActivityRule.getActivity().getString(R.string.Yes))))
                .check(matches(isDisplayed()))
                .perform(click());


        String rootPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = rootPath + "/" + filename;
        String content = getFile(filePath);

        Assert.assertEquals(textExample, content);
    }

    @Test
    public void testActivityRotation() {
        String textExample = "some new text";

        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        SystemClock.sleep(3000);

        mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        SystemClock.sleep(3000);

        onView(withId(R.id.editText1)).check(matches(withText(textExample)));
    }

    /**
     * Put file to the path
     *
     * @param filePath
     * @param data
     */
    private void putFile(String filePath, String data) {
        try {
            //Files.write( Paths.get(filePath), data.getBytes(), StandardOpenOption.CREATE);
            File f = new File(filePath);
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data.getBytes());
            fos.close();
        } catch (IOException e) {
        }
    }

    /**
     * Get file content of the file
     *
     * @param filePath
     * @return
     */
    private String getFile(String filePath) {
        try {
            File f = new File(filePath);
            FileInputStream fis = new FileInputStream(f);

            long size = f.length();
            DataInputStream dis = new DataInputStream(fis);
            byte[] b = new byte[(int) size];
            int length = dis.read(b, 0, (int) size);

            dis.close();
            fis.close();

            return new String(b, 0, length);
            //return new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
        } catch (IOException e) {
        }
        return "";
    }

    private void someDelay(int milliseconds) {
        try {
            wait(milliseconds);
        } catch (InterruptedException e) {

        }
    }

    /**
     * Go to document folder
     */
    private void gotToDocumentsFolder() {
        while(hasText(TPStrings.FOLDER_UP)) {
            onView(withText(TPStrings.FOLDER_UP)).check(matches(isDisplayed())).perform(click());
        }

        //someDelay(3000);
        //waitFor(3000);
        //SystemClock.sleep(3000);

        //if (hasText("storage")) {
        //    onView(withText(("storage")))
        //            .check(matches(isDisplayed()))
        //            .perform(click());
        //}

        //someDelay(3000);
        //waitFor(3000);
        //SystemClock.sleep(3000);

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

    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
}
