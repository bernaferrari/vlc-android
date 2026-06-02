/*
 * ************************************************************************
 *  PhoneScreenhotsInstrumentedTest.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc

import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.hamcrest.core.AllOf.allOf
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.EXTRA_FOR_ESPRESSO
import org.videolan.resources.EXTRA_TARGET
import org.videolan.tools.KEY_AUDIO_RESUME_CARD
import org.videolan.tools.Settings
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.util.ScreenshotUtil
import org.videolan.vlc.util.UiUtils.waitId
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

//@RunWith(AndroidJUnit4::class)
class PhoneScreenhotsInstrumentedTest : BaseUITest() {
    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule
    @JvmField
    val demoModeRule = DemoModeRule()

    @Test
    fun testTakeScreenshot() {
        onView(isRoot()).perform(waitId(R.id.content_placeholder, 5000))
        ScreenshotUtil.takeScreenshot(2, "audio_root")
    }

    @Test
    fun testTakeScreenshotVideo() {
        onView(allOf(withId(R.id.nav_video), withEffectiveVisibility(Visibility.VISIBLE)))
            .perform(click())
        onView(isRoot()).perform(waitId(R.id.content_placeholder, 5000))

        ScreenshotUtil.takeScreenshot(1, "video_root")
    }

    @Test
    fun testTakeScreenshotBrowser() {
        onView(allOf(withId(R.id.nav_directories), withEffectiveVisibility(Visibility.VISIBLE)))
            .perform(click())
        onView(isRoot()).perform(waitId(R.id.content_placeholder, 5000))

        ScreenshotUtil.takeScreenshot(5, "browser")
    }

    companion object {
        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }

    override fun beforeTest() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        Settings.getInstance(context).edit().putBoolean("auto_rescan", false).putBoolean(KEY_AUDIO_RESUME_CARD, false).commit()
        val intent = Intent().apply {
            putExtra(EXTRA_TARGET, R.id.nav_audio)
            putParcelableArrayListExtra(
                EXTRA_FOR_ESPRESSO, arrayListOf(
                    MLServiceLocator.getAbstractMediaWrapper(
                        Uri.parse("upnp://test/mock"), 0L, 0F, 0L, MediaWrapper.TYPE_ALL,
                        null, "My NAS", -1, -1, "",
                        "", -1, "", "",
                        0, 0, "/storage/emulated/0/Download/upnp2.png",
                        0, 0, 0,
                        0, 0L, 0L,
                        0L
                    ),
                    MLServiceLocator.getAbstractMediaWrapper(
                        Uri.parse("upnp://test/mock"), 0L, 0F, 0L, MediaWrapper.TYPE_ALL,
                        null, "My SMB server", -1, -1, "",
                        "", -1, "", "",
                        0, 0, "/storage/emulated/0/Download/upnp1.png",
                        0, 0, 0,
                        0, 0L, 0L,
                        0L
                    )
                )
            )
        }
        activityTestRule.launchActivity(intent)
    }
}
