/*
 * ************************************************************************
 *  AudioTipsDelegate.kt
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

package org.videolan.vlc.gui.audio

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.videolan.tools.PREF_AUDIOPLAYER_TIPS_SHOWN
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.gui.AudioPlayerContainerActivity
import org.videolan.vlc.gui.helpers.UiTools.isTablet
import org.videolan.vlc.gui.view.AudioPlayerTipsHostView

class AudioTipsDelegate(private val activity: AudioPlayerContainerActivity) {
    var currentTip: AudioPlayerTipsStep? = null
    private lateinit var audioPlayerTipsHost: AudioPlayerTipsHostView

    fun init(host: AudioPlayerTipsHostView) {
        audioPlayerTipsHost = host
        audioPlayerTipsHost.setVisible()
        audioPlayerTipsHost.tipsView.setCallbacks(
            onDismiss = ::close,
            onNext = ::next
        )
        activity.playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        activity.playerBehavior.lock(true)
        activity.playerBehavior.setPeekHeightListener {
            updateBackgroundPosition(it)
        }
        activity.lifecycleScope.launch(Dispatchers.Main) { next() }
    }

    private fun updateBackgroundPosition(peek: Int) {
        if (::audioPlayerTipsHost.isInitialized) audioPlayerTipsHost.tipsView.setBottomInset(peek)
    }

    /**
     * Load the next tip screen depending on the currentTip
     */
    fun next() {
        if (currentTip == AudioPlayerTipsStep.HOLD_STOP) {
            close()
            return
        }
        currentTip = currentTip?.next() ?: AudioPlayerTipsStep.SWIPE_NEXT

        val tablet = activity.isTablet()
        audioPlayerTipsHost.setVisible()
        audioPlayerTipsHost.tipsView.showTip(
            step = currentTip!!,
            title = currentTip!!.titleText,
            description = if (tablet) currentTip!!.descriptionTextTablet else currentTip!!.descriptionText,
            isTablet = tablet,
            playlistIndicatorCenterXPx = if (tablet) centerX(R.id.header_previous) else -1,
            stopIndicatorCenterXPx = if (tablet) centerX(R.id.header_large_play_pause) else -1
        )
        updateBackgroundPosition(activity.playerBehavior.peekHeight)
    }

    /**
     * Close the tips, cancel all the animations, relaunch the playback
     */
    fun close() {
        if (::audioPlayerTipsHost.isInitialized) {
            audioPlayerTipsHost.tipsView.hideTips()
            audioPlayerTipsHost.setGone()
        }
        activity.playerBehavior.removePeekHeightListener()
        Settings.getInstance(activity).putSingle(PREF_AUDIOPLAYER_TIPS_SHOWN, true)
        currentTip = null

        activity.audioPlayer.playlistModel.service?.play()
        activity.shownTips.add(R.id.audio_player_tips)
        activity.playerBehavior.lock(false)
    }

    private fun centerX(viewId: Int): Int {
        val view = activity.findViewById<View>(viewId) ?: return -1
        return view.left + view.width / 2
    }
}

/**
 * Steps for the tips
 * @param titleText: the string resource to display in the title
 * @param descriptionText: the string resource to display in the description
 */
enum class AudioPlayerTipsStep(@StringRes var titleText: Int, @StringRes var descriptionText: Int, @StringRes var descriptionTextTablet: Int) {
    SWIPE_NEXT(R.string.previous_next_song, R.string.tips_swipe_horizontal, R.string.tap_to_previous_next),
    TAP_PLAYLIST(R.string.tips_playlist, R.string.tap, R.string.tap),
    HOLD_STOP(R.string.stop, R.string.hold_to_stop, R.string.hold_to_stop);

    /**
     * @return the next step
     */
    fun next(): AudioPlayerTipsStep {
        return AudioPlayerTipsStep.entries[ordinal + 1]
    }
}
