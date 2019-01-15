package com.maxistar.textpad.test;


import com.maxistar.textpad.EditorActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class Application {
    @Rule
    public ActivityTestRule<EditorActivity> mActivityRule =
            new ActivityTestRule<>(EditorActivity.class);

    @Test
    public void listGoesOverTheFold() {
        onView(withText("newfile.txt")).check(matches(isDisplayed()));
    }
}



