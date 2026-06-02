package org.videolan.vlc.gui

import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.resources.EXTRA_TARGET
import org.videolan.vlc.BaseUITest
import org.videolan.vlc.R

class PlaylistUITest : BaseUITest() {
    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    override fun beforeTest() {
        val intent = Intent().apply {
            putExtra(EXTRA_TARGET, R.id.nav_playlists)
        }
        activityTestRule.launchActivity(intent)
    }

    @After
    fun resetData() {
        Medialibrary.getInstance().getPlaylists(Playlist.Type.All, false).map { it.delete() }
    }

    private fun createDummyPlaylist() {
        val ml = Medialibrary.getInstance()
        val pl = ml.createPlaylist(DUMMY_PLAYLIST, true, false)
        pl.append(ml.getPagedVideos(Medialibrary.SORT_DEFAULT, false, true, false, 5, 0).map { it.id })
        pl.append(ml.getPagedAudio(Medialibrary.SORT_DEFAULT, false, true, false, 5, 0).map { it.id })
    }

    @Test
    fun whenAtRoot_checkComposeHostShown() {
        onView(withId(R.id.content_placeholder))
            .check(matches(isDisplayed()))
    }

    @Test
    fun whenOnePlaylist_checkTitleShown() {
        createDummyPlaylist()
        Thread.sleep(1500)

        onView(withText(DUMMY_PLAYLIST))
            .check(matches(isDisplayed()))
    }

    @Test
    fun whenAtRoot_checkAppbarWorks() {
        onView(withId(R.id.ml_menu_filter))
            .check(matches(isDisplayed()))

        onView(withId(R.id.ml_menu_sortby))
            .check(matches(isDisplayed()))
            .perform(click())

        onView(anyOf(withText(R.string.sortby_name), withId(R.id.ml_menu_sortby_name)))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        Espresso.pressBack()

        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.refresh))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        Espresso.pressBack()

        onView(withId(R.id.ml_menu_filter))
            .perform(click())

        Espresso.pressBack()
        Espresso.pressBack()

        onView(withId(R.id.ml_menu_filter))
            .check(matches(isDisplayed()))
        onView(withId(R.id.ml_menu_sortby))
            .check(matches(isDisplayed()))
    }

    companion object {
        const val DUMMY_PLAYLIST = "test"
    }
}
