package org.videolan.vlc.gui

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.resources.TAG_ITEM
import org.videolan.vlc.BaseUITest
import org.videolan.vlc.R
import org.videolan.vlc.sizeOfAtLeast

class HeaderMediaListActivityUITest : BaseUITest() {
    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(HeaderMediaListActivity::class.java, true, false)

    override fun beforeTest() {
        // TODO: Hack because of IO Dispatcher used in MediaParsingService channel
        Thread.sleep(3 * 1000)

        val ml = Medialibrary.getInstance()
        val pl = ml.createPlaylist("test", true, false)
        pl.append(ml.getPagedVideos(Medialibrary.SORT_DEFAULT, false, true, false, 5, 0).map { it.id })
        pl.append(ml.getPagedAudio(Medialibrary.SORT_DEFAULT, false, true, false, 5, 0).map { it.id })

        val intent = Intent().apply {
            putExtra(TAG_ITEM, pl)
        }
        activityTestRule.launchActivity(intent)
    }

    @Test
    fun whenAtTestPlaylist_checkMediaListAndPlayButton() {
        onView(withId(R.id.songs))
            .check(matches(sizeOfAtLeast(1)))

        onView(withId(R.id.fab))
            .check(matches(isDisplayed()))
    }

    @Test
    fun whenAtTestPlaylist_checkToolbarAndMediaListShown() {
        onView(withId(R.id.main_toolbar))
            .check(matches(hasDescendant(withText("test"))))

        onView(withId(R.id.songs))
            .check(matches(isDisplayed()))
    }
}
