package org.videolan.vlc.gui.browser

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.videolan.vlc.BaseUITest
import org.videolan.vlc.R
import org.videolan.vlc.gui.SecondaryActivity

class StorageBrowserUITest : BaseUITest() {
    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(SecondaryActivity::class.java, true, false)

    override fun beforeTest() {
        val intent = Intent().apply {
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.STORAGE_BROWSER)
        }
        activityTestRule.launchActivity(intent)
    }

    @Test
    fun whenAtRoot_checkCorrectAppbar() {
        onView(withId(R.id.main_toolbar))
            .check(matches(hasDescendant(withText(R.string.directories_summary))))

        onView(withId(R.id.content_placeholder))
            .check(matches(isDisplayed()))

        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.add_custom_path))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun whenAtRootClickAddCustomPath_showDialog() {
        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.add_custom_path))
            .inRoot(isPlatformPopup())
            .perform(click())
    }

    @Test
    fun whenAtRoot_checkInternalStorageShown() {
        onView(withText(R.string.internal_memory))
            .check(matches(isDisplayed()))
    }
}
