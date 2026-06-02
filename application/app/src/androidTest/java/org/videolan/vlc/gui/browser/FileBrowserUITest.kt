package org.videolan.vlc.gui.browser

import android.content.Intent
import androidx.test.espresso.Espresso
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
import org.hamcrest.Matchers.anyOf
import org.junit.Rule
import org.junit.Test
import org.videolan.resources.EXTRA_TARGET
import org.videolan.vlc.BaseUITest
import org.videolan.vlc.R
import org.videolan.vlc.gui.MainActivity

class FileBrowserUITest : BaseUITest() {
    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    override fun beforeTest() {
        val intent = Intent().apply {
            putExtra(EXTRA_TARGET, R.id.nav_directories)
        }
        activityTestRule.launchActivity(intent)
    }

    @Test
    fun whenAtRoot_checkCorrectAppbarTitle() {
        onView(withId(R.id.main_toolbar))
            .check(matches(hasDescendant(withText(R.string.directories))))
    }

    @Test
    fun whenAtRoot_checkComposeHostShown() {
        onView(withId(R.id.content_placeholder))
            .check(matches(isDisplayed()))
    }

    @Test
    fun whenAtRoot_checkInternalStorageShown() {
        onView(withText(R.string.internal_memory))
            .check(matches(isDisplayed()))
    }

    @Test
    fun whenAtRoot_checkOverflowMenuShowsRefresh() {
        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.refresh))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun whenAtRoot_checkSortMenuShown() {
        openActionBarOverflowOrOptionsMenu(context)

        onView(anyOf(withText(R.string.sortby), withId(R.id.ml_menu_sortby)))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
            .perform(click())

        onView(anyOf(withText(R.string.sortby_name), withId(R.id.ml_menu_sortby_name)))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        Espresso.pressBack()
    }
}
