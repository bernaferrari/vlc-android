/*****************************************************************************
 * AudioPlayerViews.kt
 *
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
 */

package org.videolan.vlc.gui.audio

import android.view.View
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.view.AbRepeatControlsView
import org.videolan.vlc.gui.view.AbRepeatMarkerContainerView
import org.videolan.vlc.gui.view.AudioPlayerBackgroundView
import org.videolan.vlc.gui.view.AudioPlaylistSearchFieldView
import org.videolan.vlc.gui.view.BookmarkMarkerContainerView
import org.videolan.vlc.gui.view.CoverMediaSwitcher
import org.videolan.vlc.gui.view.HeaderMediaSwitcher

class AudioPlayerViews(val root: ConstraintLayout) {
    val contentLayout: ConstraintLayout = root
    val backgroundView: AudioPlayerBackgroundView = root.requireAudioPlayerView(R.id.backgroundView)
    val progressBar: VLCComposeView = root.requireAudioPlayerView(R.id.progressBar)
    val header: ConstraintLayout = root.requireAudioPlayerView(R.id.header)
    val headerShuffle: VLCComposeView = root.requireAudioPlayerView(R.id.header_shuffle)
    val headerPrevious: VLCComposeView = root.requireAudioPlayerView(R.id.header_previous)
    val headerLargePlayPause: VLCComposeView = root.requireAudioPlayerView(R.id.header_large_play_pause)
    val headerNext: VLCComposeView = root.requireAudioPlayerView(R.id.header_next)
    val headerRepeat: VLCComposeView = root.requireAudioPlayerView(R.id.header_repeat)
    val headerBackground: VLCComposeView = root.requireAudioPlayerView(R.id.header_background)
    val headerDivider: VLCComposeView = root.requireAudioPlayerView(R.id.header_divider)
    val audioMediaSwitcher: HeaderMediaSwitcher = root.requireAudioPlayerView(R.id.audio_media_switcher)
    val playlistSearchText: AudioPlaylistSearchFieldView = root.requireAudioPlayerView(R.id.playlist_search_text)
    val abRepeatReset: VLCComposeView = root.requireAudioPlayerView(R.id.ab_repeat_reset)
    val abRepeatStop: VLCComposeView = root.requireAudioPlayerView(R.id.ab_repeat_stop)
    val playlistSearch: VLCComposeView = root.requireAudioPlayerView(R.id.playlist_search)
    val playlistSwitch: VLCComposeView = root.requireAudioPlayerView(R.id.playlist_switch)
    val advFunction: VLCComposeView = root.requireAudioPlayerView(R.id.adv_function)
    val headerTime: VLCComposeView = root.requireAudioPlayerView(R.id.header_time)
    val headerPlayPause: VLCComposeView = root.requireAudioPlayerView(R.id.header_play_pause)
    val playbackChips: VLCComposeView = root.requireAudioPlayerView(R.id.playback_chips)
    val resumeVideoHint: VLCComposeView = root.requireAudioPlayerView(R.id.resume_video_hint)
    val songsList: VLCComposeView = root.requireAudioPlayerView(R.id.songs_list)
    val audioPlayProgress: VLCComposeView = root.requireAudioPlayerView(R.id.audio_play_progress)
    val coverMediaSwitcher: CoverMediaSwitcher = root.requireAudioPlayerView(R.id.cover_media_switcher)
    val audioRewindBookmark: VLCComposeView = root.requireAudioPlayerView(R.id.audio_rewind_bookmark)
    val audioRewind10: VLCComposeView = root.requireAudioPlayerView(R.id.audio_rewind_10)
    val audioRewindText: VLCComposeView = root.requireAudioPlayerView(R.id.audio_rewind_text)
    val audioForward10: VLCComposeView = root.requireAudioPlayerView(R.id.audio_forward_10)
    val audioForwardText: VLCComposeView = root.requireAudioPlayerView(R.id.audio_forward_text)
    val audioForwardBookmark: VLCComposeView = root.requireAudioPlayerView(R.id.audio_forward_bookmark)
    val time: VLCComposeView = root.requireAudioPlayerView(R.id.time)
    val timeline: VLCComposeView = root.requireAudioPlayerView(R.id.timeline)
    val length: VLCComposeView = root.requireAudioPlayerView(R.id.length)
    val shuffle: VLCComposeView = root.requireAudioPlayerView(R.id.shuffle)
    val previous: VLCComposeView = root.requireAudioPlayerView(R.id.previous)
    val playPause: VLCComposeView = root.requireAudioPlayerView(R.id.play_pause)
    val next: VLCComposeView = root.requireAudioPlayerView(R.id.next)
    val repeat: VLCComposeView = root.requireAudioPlayerView(R.id.repeat)
    val centerGuideline: Guideline = root.requireAudioPlayerView(R.id.centerGuideline)
    val hingeGoLeft: VLCComposeView = root.requireAudioPlayerView(R.id.hinge_go_left)
    val hingeGoRight: VLCComposeView = root.requireAudioPlayerView(R.id.hinge_go_right)
    val bookmarkMarkerContainer: BookmarkMarkerContainerView = root.requireAudioPlayerView(R.id.bookmark_marker_container)
    val abRepeatMarkerGuidelineContainer: AbRepeatMarkerContainerView = root.requireAudioPlayerView(R.id.ab_repeat_marker_guideline_container)
    val abRepeatContainer: AbRepeatControlsView = root.requireAudioPlayerView(R.id.ab_repeat_container)
    val trackInfoContainer: ConstraintLayout? = root.findAudioPlayerView(R.id.track_info_container)
    val previousChapter: VLCComposeView? = root.findAudioPlayerView(R.id.previous_chapter)
    val nextChapter: VLCComposeView? = root.findAudioPlayerView(R.id.next_chapter)
    val songTitle: VLCComposeView? = root.findAudioPlayerView(R.id.song_title)
    val songSubtitle: VLCComposeView? = root.findAudioPlayerView(R.id.song_subtitle)
    val songTrackInfo: VLCComposeView? = root.findAudioPlayerView(R.id.song_track_info)
}

private inline fun <reified T : View> View.requireAudioPlayerView(@IdRes id: Int): T =
        findAudioPlayerView(id) ?: error("Missing audio player view ${resources.getResourceEntryName(id)}")

private inline fun <reified T : View> View.findAudioPlayerView(@IdRes id: Int): T? = findViewById(id)
