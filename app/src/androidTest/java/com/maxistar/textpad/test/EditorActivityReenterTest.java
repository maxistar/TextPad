package com.maxistar.textpad.test;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;

import com.maxistar.textpad.ServiceLocator;
import com.maxistar.textpad.SettingsService;
import com.maxistar.textpad.activities.EditorActivity;
import com.maxistar.textpad.R;
import com.maxistar.textpad.test.assertions.TextViewAssertions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@LargeTest
public class EditorActivityReenterTest {

    Context targetContext;

    ActivityScenario<EditorActivity> activityRule;

    private EditorActivity currentActivity;

    @Rule
    public IntentsTestRule<EditorActivity> intentsTestRule =
            new IntentsTestRule<>(EditorActivity.class);

    @Before
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

    /**
     * Check if the text is empty if to click on new menu item
     */
    @Test
    public void testReenterActivity() {
        String textExample = "some new text";


        currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(withId(R.id.editText1))
                .perform(typeText(textExample))
                .check(TextViewAssertions.hasInsertionPointerAtIndex(13));

        currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(withId(R.id.editText1))
                .perform(typeText(textExample))
                .check(TextViewAssertions.hasInsertionPointerAtIndex(26));

        clickOptionMenu(R.string.Settings);
        onView(isRoot()).perform(ViewActions.pressBack());

        onView(withId(R.id.editText1))
                .check(matches(withText(textExample + textExample)));
        onView(withId(R.id.editText1))
                .check(TextViewAssertions.hasInsertionPointerAtIndex(26));
    }

    private void clickOptionMenu(int stringId) {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        onView(withText(stringId))
                .check(matches(isDisplayed()))
                .perform(click());
    }
}
