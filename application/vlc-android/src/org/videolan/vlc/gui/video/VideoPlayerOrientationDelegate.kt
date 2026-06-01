/*
 * ************************************************************************
 *  VideoPlayerOrientationDelegate.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.video

import android.content.pm.ActivityInfo
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.edit
import org.videolan.tools.SHOW_ORIENTATION_BUTTON
import org.videolan.tools.Settings
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.gui.view.VideoOrientationOverlayView

class VideoPlayerOrientationDelegate(private val player: VideoPlayerActivity) {
    private val overlayDelegate: VideoPlayerOverlayDelegate
        get() = player.overlayDelegate
    private lateinit var orientationMainView: VideoOrientationOverlayView

    /**
     * Check if the orientation overlay is currently shown
     * @return true if it's shown
     */
    fun isShowing() = ::orientationMainView.isInitialized && orientationMainView.visibility == View.VISIBLE

    /**
     * Show the orientation overlay. Inflate it if it's not yet
     */
    private fun showOrientationOverlay() {
        player.findViewById<VideoOrientationOverlayView>(R.id.player_orientation_stub)?.let {
            orientationMainView = it
            val settings = Settings.getInstance(player)
            orientationMainView.bind(
                selected = OrientationMode.findByValue(if (player.orientationMode.locked) player.orientationMode.orientation else -1),
                showButton = settings.getBoolean(SHOW_ORIENTATION_BUTTON, true),
                onDismiss = ::hideOrientationOverlay,
                onShowButtonChange = { isChecked ->
                    settings.edit { putBoolean(SHOW_ORIENTATION_BUTTON, isChecked) }
                    player.overlayDelegate.updateOrientationIcon()
                    orientationMainView.updateShowOrientationButton(isChecked)
                },
                onOrientationSelected = { orientation ->
                    player.setOrientation(orientation.value)
                    hideOrientationOverlay()
                }
            )
            orientationMainView.setVisible()
        }
    }

    /**
     * Hide the overlay
     */
    fun hideOrientationOverlay() {
        orientationMainView.setGone()
    }


    /**
     * display the orientation overlay and hide everything else
     */
    fun displayOrientation(): Boolean {
        if (player.service?.hasRenderer() == true) return false
        showOrientationOverlay()
        overlayDelegate.hideOverlay(true)
        orientationMainView.setVisible()
        return true
    }

}

enum class OrientationMode(@StringRes val title: Int, val value: Int) {
    SENSOR(R.string.screen_orientation_sensor, -1),
    PORTRAIT(R.string.screen_orientation_portrait, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    PORTRAIT_REVERSE(R.string.screen_orientation_portrait_reverse, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT),
    LANDSCAPE(R.string.screen_orientation_landscape, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    LANDSCAPE_REVERSE(R.string.screen_orientation_landscape_reverse, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE),
    LANDSCAPE_SENSOR(R.string.screen_orientation_landscape_sensor, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

    companion object {
        fun findByValue(value: Int) = OrientationMode.entries.firstOrNull { it.value == value } ?: PORTRAIT
    }
}
