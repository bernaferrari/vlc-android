/*
 * ************************************************************************
 *  VideoHudOverlayViews.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
 *
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

package org.videolan.vlc.gui.video

import android.view.View
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.view.PlayerTimelineSeekBarHost
import org.videolan.vlc.gui.view.playerTimelineSeekBarHost

class VideoHudOverlayViews(val progressOverlay: ConstraintLayout) {
    val statsContainer: VLCComposeView = progressOverlay.requireHudView(R.id.stats_container)
    val abRepeatContainer: VLCComposeView = progressOverlay.requireHudView(R.id.ab_repeat_container)
    val abRepeatReset: VLCComposeView = progressOverlay.requireHudView(R.id.ab_repeat_reset)
    val abRepeatStop: VLCComposeView = progressOverlay.requireHudView(R.id.ab_repeat_stop)
    val fastSeekWarning: VLCComposeView = progressOverlay.requireHudView(R.id.fast_seek_warning)
    val bookmarksBackground: VLCComposeView = progressOverlay.requireHudView(R.id.bookmarks_background)
    val playerOverlayTime: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_time)
    val playerOverlayLength: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_length)
    val playerOverlaySeekbarView: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_seekbar)
    val playerOverlaySeekbar: PlayerTimelineSeekBarHost = playerOverlaySeekbarView.playerTimelineSeekBarHost()
    val bookmarkMarkerContainer: VLCComposeView = progressOverlay.requireHudView(R.id.bookmark_marker_container)
    val abRepeatMarkerGuidelineContainer: VLCComposeView = progressOverlay.requireHudView(R.id.ab_repeat_marker_guideline_container)
    val playerOverlayTracks: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_tracks)
    val orientationToggle: VLCComposeView = progressOverlay.requireHudView(R.id.orientation_toggle)
    val playerSpaceLeft: VLCComposeView = progressOverlay.requireHudView(R.id.player_space_left)
    val playlistPrevious: VLCComposeView = progressOverlay.requireHudView(R.id.playlist_previous)
    val playerOverlayRewind: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_rewind)
    val playerOverlayRewindText: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_rewind_text)
    val playerOverlayPlay: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_play)
    val playerOverlayForward: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_forward)
    val playerOverlayForwardText: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_forward_text)
    val playlistNext: VLCComposeView = progressOverlay.requireHudView(R.id.playlist_next)
    val playerSpaceRight: VLCComposeView = progressOverlay.requireHudView(R.id.player_space_right)
    val playerResize: VLCComposeView = progressOverlay.requireHudView(R.id.player_resize)
    val playerOverlayAdvFunction: VLCComposeView = progressOverlay.requireHudView(R.id.player_overlay_adv_function)
    val swipeToUnlock: VLCComposeView = progressOverlay.requireHudView(R.id.swipe_to_unlock)

    private inline fun <reified T : View> View.requireHudView(@IdRes id: Int): T {
        return findViewById(id) ?: error("Missing video HUD view ${resources.getResourceEntryName(id)}")
    }
}
