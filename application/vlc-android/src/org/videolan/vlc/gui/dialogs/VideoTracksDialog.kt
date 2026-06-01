/*
 * ************************************************************************
 *  VideoTracksDialog.kt
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

package org.videolan.vlc.gui.dialogs

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.VlcMigrationHelper
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.getDisableTrack
import org.videolan.vlc.gui.dialogs.adapters.VlcTrack
import org.videolan.vlc.isVLC4

object VideoTracksDialog {
    const val TAG = "VLC/VideoTracksDialog"

    enum class TrackType {
        VIDEO, AUDIO, SPU
    }

    enum class VideoTrackOption {
        SUB_DELAY, SUB_PICK, SUB_DOWNLOAD, AUDIO_DELAY
    }
}

fun ComponentActivity.showVideoTracksComposeDialog(
    menuListener: (VideoTracksDialog.VideoTrackOption) -> Unit,
    trackSelectionListener: (String, VideoTracksDialog.TrackType) -> Unit
) {
    VideoTracksComposeDialog(
        activity = this,
        menuListener = menuListener,
        trackSelectionListener = trackSelectionListener
    ).show()
}

private class VideoTracksComposeDialog(
    activity: ComponentActivity,
    private val menuListener: (VideoTracksDialog.VideoTrackOption) -> Unit,
    private val trackSelectionListener: (String, VideoTracksDialog.TrackType) -> Unit
) : PlaybackComposeBottomSheetDialog(activity = activity, allowRemote = true) {

    private var sections by mutableStateOf(defaultSections())

    override fun onServiceAvailable(service: PlaybackService) {
        updateSections(service)
    }

    override fun onMediaChanged(service: PlaybackService) {
        updateSections(service)
    }

    @Composable
    override fun Content() {
        VideoTracksContent(
            sections = sections,
            onTrackSelected = ::selectTrack,
            onOptionSelected = ::selectOption
        )
    }

    private fun updateSections(service: PlaybackService) {
        sections = buildList {
            if (service.audioTracksCount > 0) {
                add(
                    TrackSectionState(
                        title = activity.getString(R.string.audio),
                        talkbackTitle = activity.getString(R.string.track_audio),
                        trackType = VideoTracksDialog.TrackType.AUDIO,
                        tracks = service.audioTracks.withDisableTrack(),
                        selectedTrackId = service.audioTrack,
                        options = listOf(
                            TrackOptionState(
                                title = activity.getString(R.string.audio_delay),
                                icon = R.drawable.ic_delay,
                                option = VideoTracksDialog.VideoTrackOption.AUDIO_DELAY
                            )
                        )
                    )
                )
            }

            val subtitleTracks = if (service.hasRenderer()) emptyList() else service.spuTracks.withDisableTrack()
            add(
                TrackSectionState(
                    title = activity.getString(R.string.subtitles),
                    talkbackTitle = activity.getString(R.string.track_text),
                    trackType = VideoTracksDialog.TrackType.SPU,
                    tracks = subtitleTracks,
                    selectedTrackId = service.spuTrack,
                    emptyText = activity.getString(if (service.hasRenderer()) R.string.no_sub_renderer else R.string.no_track),
                    options = if (service.hasRenderer()) {
                        emptyList()
                    } else {
                        buildList {
                            add(
                                TrackOptionState(
                                    title = activity.getString(R.string.spu_delay),
                                    icon = R.drawable.ic_delay,
                                    option = VideoTracksDialog.VideoTrackOption.SUB_DELAY
                                )
                            )
                            add(
                                TrackOptionState(
                                    title = activity.getString(R.string.subtitle_select),
                                    icon = R.drawable.ic_subtitles_file,
                                    option = VideoTracksDialog.VideoTrackOption.SUB_PICK
                                )
                            )
                            if (VlcMigrationHelper.isLolliPopOrLater) {
                                add(
                                    TrackOptionState(
                                        title = activity.getString(R.string.download_subtitles),
                                        icon = R.drawable.ic_download_subtitles,
                                        option = VideoTracksDialog.VideoTrackOption.SUB_DOWNLOAD
                                    )
                                )
                            }
                        }
                    }
                )
            )

            val showVideoTracks = if (isVLC4()) service.videoTracksCount >= 2 else service.videoTracksCount > 2
            if (showVideoTracks) {
                add(
                    TrackSectionState(
                        title = activity.getString(R.string.video),
                        talkbackTitle = activity.getString(R.string.track_video),
                        trackType = VideoTracksDialog.TrackType.VIDEO,
                        tracks = service.videoTracks.withDisableTrack(),
                        selectedTrackId = service.videoTrack
                    )
                )
            }
        }
    }

    private fun selectTrack(section: TrackSectionState, track: VlcTrack) {
        sections = sections.map {
            if (it.trackType == section.trackType) it.copy(selectedTrackId = track.getId()) else it
        }
        trackSelectionListener(track.getId(), section.trackType)
    }

    private fun selectOption(option: VideoTracksDialog.VideoTrackOption) {
        menuListener(option)
        dismiss()
    }

    private fun Array<out VlcTrack>?.withDisableTrack(): List<VlcTrack> {
        val tracks = this?.toList().orEmpty()
        if (!isVLC4()) return tracks
        return listOf(getDisableTrack(activity)) + tracks
    }

    private fun defaultSections() = listOf(
        TrackSectionState(
            title = activity.getString(R.string.subtitles),
            talkbackTitle = activity.getString(R.string.track_text),
            trackType = VideoTracksDialog.TrackType.SPU,
            tracks = emptyList(),
            selectedTrackId = null,
            emptyText = activity.getString(R.string.no_track)
        )
    )
}

@Composable
private fun VideoTracksContent(
    sections: List<TrackSectionState>,
    onTrackSelected: (TrackSectionState, VlcTrack) -> Unit,
    onOptionSelected: (VideoTracksDialog.VideoTrackOption) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
        ) {
            itemsIndexed(sections, key = { _, item -> item.trackType.name }) { index, section ->
                TrackSection(
                    section = section,
                    onTrackSelected = { onTrackSelected(section, it) },
                    onOptionSelected = onOptionSelected
                )
                if (index != sections.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.defaultDivider)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackSection(
    section: TrackSectionState,
    onTrackSelected: (VlcTrack) -> Unit,
    onOptionSelected: (VideoTracksDialog.VideoTrackOption) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)
        )
        section.options.forEach { option ->
            TrackOptionRow(option = option, onClick = { onOptionSelected(option.option) })
        }
        if (section.tracks.isEmpty()) {
            Text(
                text = section.emptyText ?: stringResource(R.string.no_track),
                color = colors.fontLight,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        } else {
            section.tracks.forEach { track ->
                TrackRow(
                    track = track,
                    selected = track.getId() == section.selectedTrackId,
                    trackTypePrefix = section.talkbackTitle,
                    onClick = { onTrackSelected(track) }
                )
            }
        }
    }
}

@Composable
private fun TrackOptionRow(
    option: TrackOptionState,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            painter = painterResource(option.icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = option.title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackRow(
    track: VlcTrack,
    selected: Boolean,
    trackTypePrefix: String,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val trackName = if (track.getId() == "-1") stringResource(R.string.disable_track) else track.getName()
    val selectedText = if (selected) stringResource(R.string.selected) else ""
    val description = stringResource(R.string.talkback_track, trackTypePrefix, trackName, selectedText)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .focusable()
            .semantics { contentDescription = description }
            .padding(horizontal = 16.dp)
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_delay_done),
                contentDescription = null,
                tint = colors.fontDefault,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = trackName,
            color = if (selected) colors.fontDefault else colors.fontLight,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private data class TrackSectionState(
    val title: String,
    val talkbackTitle: String,
    val trackType: VideoTracksDialog.TrackType,
    val tracks: List<VlcTrack>,
    val selectedTrackId: String?,
    val emptyText: String? = null,
    val options: List<TrackOptionState> = emptyList()
)

private data class TrackOptionState(
    val title: String,
    @DrawableRes val icon: Int,
    val option: VideoTracksDialog.VideoTrackOption
)
