/*
 * ************************************************************************
 *  VideoTipsDelegate.kt
 * *************************************************************************
 * Copyright © 2021 VLC authors and VideoLAN
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

import androidx.annotation.StringRes
import org.videolan.tools.PREF_TIPS_SHOWN
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.gui.view.VideoTipsControl
import org.videolan.vlc.gui.view.VideoTipsHostView

/**
 * Delegate to manage the video tips workflow.
 */
class VideoTipsDelegate(private val player: VideoPlayerActivity) {

    var currentTip: VideoPlayerTipsStep? = null
    var currentControl: VideoTipsControl? = null
    private lateinit var tipsHost: VideoTipsHostView

    /**
     * Init the tips:
     * - Initialize the Compose host
     * - Start the tips
     */
    fun init() {
        tipsHost = player.findViewById(R.id.player_overlay_tips)
        tipsHost.setCallbacks(
            onDismiss = ::close,
            onNext = ::next,
            onControl = ::onControlClick
        )
        currentTip = null
        currentControl = null
        next()
    }

    /**
     * Load the next tip screen depending on the currentTip
     */
    fun next() {
        if (!::tipsHost.isInitialized) return
        if (currentTip == VideoPlayerTipsStep.SEEK) {
            close()
            return
        }
        currentTip = currentTip?.next() ?: VideoPlayerTipsStep.CONTROLS
        currentControl = null
        val step = currentTip!!
        tipsHost.showStep(
            step = step,
            title = step.titleText,
            description = step.descriptionText,
            nextButtonText = if (step == VideoPlayerTipsStep.SEEK) R.string.close else R.string.next_step
        )
    }

    /**
     * Close the tips, cancel all the animations, relaunch the playback
     */
    fun close() {
        if (::tipsHost.isInitialized) tipsHost.hideTips()
        Settings.getInstance(player).putSingle(PREF_TIPS_SHOWN, true)
        currentTip = null
        currentControl = null
        player.play()
    }

    /**
     * Click listener for the tap indicators
     */
    private fun onControlClick(control: VideoTipsControl) {
        if (currentTip != VideoPlayerTipsStep.CONTROLS) return
        if (currentControl == control) {
            currentControl = null
            tipsHost.updateControlCopy(
                selectedControl = null,
                title = R.string.tips_player_controls,
                description = R.string.tips_player_controls_description
            )
            return
        }
        currentControl = control
        tipsHost.updateControlCopy(
            selectedControl = control,
            title = control.selectedTitle,
            description = control.selectedDescription
        )
    }
}

/**
 * Steps for the tips
 * @param titleText: the string resource to display in the title [TextView]
 * @param descriptionText: the string resource to display in the description [TextView]
 */
enum class VideoPlayerTipsStep(@StringRes var titleText: Int, @StringRes var descriptionText: Int) {
    CONTROLS(R.string.tips_player_controls, R.string.tips_player_controls_description),
    BRIGHTNESS(R.string.brightness, R.string.tips_swipe),
    VOLUME(R.string.volume, R.string.tips_swipe),
    PAUSE(R.string.pause, R.string.pause_description),
    SEEK_TAP(R.string.seek_tap, R.string.seek_tap_description),
    SEEK(R.string.seek, R.string.tips_swipe_horizontal);

    /**
     * @return the next step
     */
    fun next(): VideoPlayerTipsStep {
        return VideoPlayerTipsStep.entries[ordinal + 1]
    }
}
