package com.maxistar.textpad.test;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.service.SettingsService;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.R;
import com.maxistar.textpad.TPStrings;
import com.maxistar.textpad.test.assertions.TextViewAssertions;

import org.hamcrest.Matcher;
import org.junit.Assert;
// import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
// import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.FailureHandler;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
// import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
// import static androidx.test.espresso.intent.Intents.intending;
// import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
public class EditorActivityTest {

    Context targetContext;

    ActivityScenario<EditorActivity> activityRule;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private EditorActivity currentActivity;

    // @Rule
    // public IntentsTestRule<EditorActivity> intentsTestRule =
    //         new IntentsTestRule<>(EditorActivity.class);

    // @BeforeEach
    public void launchActivity() {
        setLegacyFileFinder();

        activityRule = ActivityScenario.launch(EditorActivity.class);

        onView(isRoot()).check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {

                View checkedView = view;

                while (checkedView instanceof ViewGroup && ((ViewGroup) checkedView).getChildCount() > 0) {

                    checkedView = ((ViewGroup) checkedView).getChildAt(0);

                    if (checkedView.getContext() instanceof Activity) {
                        currentActivity = (EditorActivity) checkedView.getContext();
                        return;
                    }
                }
            }
        });
    }

    void setLegacyFileFinder() {
        targetContext = ApplicationProvider.getApplicationContext();

        SettingsService settingsService = ServiceLocator.getInstance()
                .getSettingsService(targetContext);
        settingsService.setLegacyFilePicker(true, targetContext);
    }

    void setDefaultFileFinder() {
        targetContext = ApplicationProvider.getApplicationContext();

        SettingsService settingsService = ServiceLocator.getInstance()
                .getSettingsService(targetContext);
        settingsService.setLegacyFilePicker(false, targetContext);
    }

    /**
     * Check if the text is empty if to click on new menu item
     */
    // @Test
    public void listGoesOverTheFold() {
        Context context = ApplicationProvider.getApplicationContext();
        openActionBarOverflowOrOptionsMenu(context );

        onView(withText(R.string.New))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText("newfile.txt")).check(matches(isDisplayed()));
        //settingsService.reloadSettings(context);
    }

    /**
     * Test save text
     */
    //Test
    public void listSaveText() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            return;
        }

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        onView(withText(R.string.New))
                .check(matches(isDisplayed()))
                .perform(click());


        String textExample = "some new text";

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        onView(withText(R.string.Save))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        long time = (new Date()).getTime();
        String filename = "file" + time + ".txt";

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText(R.string.Save))
                .check(matches(isDisplayed()))
                .perform(click());

        String rootPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = rootPath + "/" + filename;
        String content = getFile(filePath);

        Assert.assertEquals(content, textExample);
    }

    //Test
    public void rewriteConfirmationText() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            return;
        }

        String textExample = "some new text";
        String oldTextExample = "some old content";

        long time = (new Date()).getTime();
        String filename = "file" + time + ".txt";

        putFile(Environment.getExternalStorageDirectory().getPath() + "/" + filename, oldTextExample);

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        onView(withText(R.string.New))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        onView(withText(R.string.Save))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText(R.string.Save))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.No))
                .check(matches(isDisplayed()))
                .perform(click());


        String rootPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = rootPath + "/" + filename;
        String content = getFile(filePath);

        Assert.assertEquals(oldTextExample, content);
    }


    //Test
    public void rewriteConfirmationYesText() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            Assert.assertTrue(true);
            return;
        }

        String textExample = "some new text";
        String oldTextExample = "some old content";

        long time = (new Date()).getTime();
        String filename = "file" + time + ".txt";

        putFile(Environment.getExternalStorageDirectory().getPath() + "/" + filename, oldTextExample);

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        onView(withText(R.string.New))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1))
                .perform(setTextInTextView(textExample));

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        onView(withText(R.string.Save))
                .check(matches(isDisplayed()))
                .perform(click());

        gotToDocumentsFolder();

        onView(withId(R.id.fdEditTextFile))
                .perform(setTextInTextView(filename));

        onView(withText(R.string.Save))
                .check(matches(isDisplayed()))
                .perform(click());

        SystemClock.sleep(1000);

        onView(withText(R.string.Yes))
                .check(matches(isDisplayed()))
                .perform(click());

        String rootPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = rootPath + "/" + filename;
        String content = getFile(filePath);

        Assert.assertEquals(textExample, content);
    }

    // @Test
    public void testActivityRotation() {
        String textExample = "some new text";

        currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(withId(R.id.editText1)).perform(typeText(textExample)).check(TextViewAssertions.hasInsertionPointerAtIndex(13));

        SystemClock.sleep(3000);

        currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        SystemClock.sleep(3000);

        onView(withId(R.id.editText1)).check(matches(withText(textExample)));

        onView(withId(R.id.editText1)).check(TextViewAssertions.hasInsertionPointerAtIndex(13));
    }

    // Test does not work in hardware device test
    public void testUndoRedo() throws Throwable {
        testUndoRedoFunctions();

        setText();

        testUndoRedoFunctions();
    }

    private void setText() throws Throwable {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentActivity.clearFile();
            }
        });
    }

    void testUndoRedoFunctions() {
        String textExample = "some new text";

        onView(withId(R.id.editText1)).perform(typeText(textExample));

        onView(withId(R.id.editText1)).check(matches(withText(textExample)));

        clickOptionMenu(R.string.action_edit);

        onView(withText(R.string.action_undo))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1)).check(matches(withText("some new tex")));


        clickOptionMenu(R.string.action_edit);

        onView(withText(R.string.action_redo))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.editText1)).check(matches(withText("some new text")));
    }

    //Test
    public void testRewriteDocument() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            return;
        }
        setDefaultFileFinder();
        String textExample = "some new long long long long long long long long long text";
        onView(withId(R.id.editText1)).perform(typeText(textExample));

        String filenameUrl = "content://com.android.externalstorage.documents/document/primary%3Anewfile.txt";

        Intent resultData = new Intent();
        resultData.setData(Uri.parse(filenameUrl));
        Instrumentation.ActivityResult result =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
        // intending(toPackage("com.android.documentsui")).respondWith(result);

        clickOptionMenu(R.string.Save);

        String textNewExample = "some new short text";
        onView(withId(R.id.editText1)).perform(replaceText(textNewExample));

        clickOptionMenu(R.string.Save);

        onView(withId(R.id.editText1)).perform(typeText("something"));

        SystemClock.sleep(3000);

        clickOptionMenu(R.string.Open);

        onView(withText(R.string.No))
                .check(matches(isDisplayed()))
                .perform(click());

        someDelay(3000);

        onView(withId(R.id.editText1)).check(matches(withText(textNewExample)));

        someDelay(10000);
    }

    private void clickOptionMenu(int stringId) {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        onView(withText(stringId))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    /**
     * Put file to the path
     *
     * @param filePath File Path
     * @param data Data
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
        } catch (IOException ignored) {
        }
    }

    /**
     * Get file content of the file
     *
     * @param filePath File Path
     * @return String Loaded
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
            // empty catch block
        }
        return "";
    }

    /**
     * Some Delay to wait
     * @param milliseconds Number of milliseconds to wait
     */
    private void someDelay(int milliseconds) {
        SystemClock.sleep(milliseconds);
    }

    /**
     * Go to document folder
     */
    private void gotToDocumentsFolder() {
        while(hasText(TPStrings.FOLDER_UP)) {
            onView(withText(TPStrings.FOLDER_UP)).check(matches(isDisplayed())).perform(click());
        }

        onView(withText(("emulated")))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    /**
     *
     * @param s Text to search
     * @return boolean
     */
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
