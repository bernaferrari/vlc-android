/*
 * ************************************************************************
 *  AudioPlaylistTipsDelegate.kt
 * *************************************************************************
 * Copyright (C) 2021 VLC authors and VideoLAN
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
 */

package org.videolan.vlc.gui.audio

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.tools.PREF_PLAYLIST_TIPS_SHOWN
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.gui.AudioPlayerContainerActivity
import org.videolan.vlc.gui.helpers.UiTools.isTablet
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.viewmodels.PlaylistModel

class AudioPlaylistTipsDelegate(private val activity: AudioPlayerContainerActivity) {
    var currentTip: AudioPlaylistTipsStep? = null
    private lateinit var audioPlaylistTipsHost: AudioPlaylistTipsHostView
    private var media: MediaWrapper? = null
    private var subtitle: String = ""

    fun init(host: AudioPlaylistTipsHostView) {
        audioPlaylistTipsHost = host
        audioPlaylistTipsHost.tipsView.setCallbacks(
            onDismiss = ::close,
            onNext = ::next
        )
        activity.lockPlayer(true)
        media = ViewModelProvider(activity)[PlaylistModel::class.java].currentMediaWrapper
        subtitle = media?.let { MediaUtils.getMediaSubtitle(it) }.orEmpty()
        audioPlaylistTipsHost.setVisible()
        activity.lifecycleScope.launch(Dispatchers.Main) { next() }
    }

    /**
     * Load the next tip screen depending on the currentTip.
     */
    fun next() {
        if (currentTip == AudioPlaylistTipsStep.SEEK) {
            close()
            return
        }
        currentTip = currentTip?.next() ?: AudioPlaylistTipsStep.REMOVE

        val tablet = activity.isTablet()
        audioPlaylistTipsHost.setVisible()
        audioPlaylistTipsHost.tipsView.showTip(
            step = currentTip!!,
            title = currentTip!!.titleText,
            description = if (tablet) currentTip!!.descriptionTextTablet else currentTip!!.descriptionText,
            isTablet = tablet,
            media = media,
            subtitle = subtitle
        )
    }

    /**
     * Close the tips and relaunch playback.
     */
    fun close() {
        if (::audioPlaylistTipsHost.isInitialized) {
            audioPlaylistTipsHost.tipsView.hideTips()
            audioPlaylistTipsHost.setGone()
        }
        Settings.getInstance(activity).putSingle(PREF_PLAYLIST_TIPS_SHOWN, true)
        currentTip = null
        activity.audioPlayer.playlistModel.service?.play()
        activity.shownTips.add(R.id.audio_playlist_tips)
        activity.playerBehavior.lock(false)
    }
}

/**
 * Steps for the playlist tips.
 * @param titleText the string resource to display in the title
 * @param descriptionText the phone string resource to display in the description
 * @param descriptionTextTablet the tablet string resource to display in the description
 */
enum class AudioPlaylistTipsStep(
    @StringRes val titleText: Int,
    @StringRes val descriptionText: Int,
    @StringRes val descriptionTextTablet: Int
) {
    REMOVE(R.string.remove_song, R.string.tips_swipe_horizontal, R.string.tap_to_remove),
    REARRANGE(R.string.rearrange_order, R.string.tips_long_drop, R.string.tap_to_rearrange),
    SEEK(R.string.seek, R.string.tips_hold_seek, R.string.tips_hold_seek);

    /**
     * @return the next step.
     */
    fun next(): AudioPlaylistTipsStep {
        return AudioPlaylistTipsStep.entries[ordinal + 1]
    }
}
