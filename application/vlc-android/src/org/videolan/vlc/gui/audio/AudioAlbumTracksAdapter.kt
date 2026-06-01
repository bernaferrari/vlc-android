/*
 * *************************************************************************
 *  BaseAdapter.java
 * **************************************************************************
 *  Copyright © 2016-2017 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.audio

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.MotionEventCompat
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.interfaces.IEventsHandler
import org.videolan.vlc.interfaces.IListEventsHandler
import org.videolan.vlc.media.MediaUtils

class AudioAlbumTracksAdapter @JvmOverloads constructor(
    type: Int, eventsHandler: IEventsHandler<MediaLibraryItem>,
    listEventsHandler: IListEventsHandler? = null,
    ) : AudioBrowserAdapter(type, eventsHandler, listEventsHandler)
{

    var forceNoTracks = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractMediaItemViewHolder {
        return AlbumTrackComposeViewHolder(
                ComposeView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
        )
    }

    override fun submitList(pagedList: PagedList<MediaLibraryItem>?) {
        super.submitList(pagedList)
        forceNoTracks = pagedList?.any { ((it as? MediaWrapper)?.trackNumber ?: 0) > 0 } == false
    }

    override fun playbackStateChanged(former: MediaWrapper?, currentMedia: MediaWrapper?) {
        super.playbackStateChanged(former, super.currentMedia)
        // The current song has changed.
        // If we want to hide the track numbers but the current playback is in this list, show the track number (with no value)
        if (!Settings.showTrackNumber || forceNoTracks) {
                if (currentList?.contains(former) == false && currentList?.contains(currentMedia) == true) {
                    notifyItemRangeChanged(0, itemCount)
                }
                //playback just stopped
                if (currentList?.contains(former) == true || currentMedia == null) notifyItemRangeChanged(0, itemCount)
            }
    }

    inner class AlbumTrackComposeViewHolder(
            private val composeView: ComposeView
    ) : AbstractMediaItemViewHolder(composeView) {

        override fun bindItem(
                item: MediaLibraryItem?,
                selected: Boolean,
                inSelection: Boolean,
                isCurrent: Boolean,
                playing: Boolean
        ) {
            val media = item as? MediaWrapper
            val showTrackNumberColumn = media != null && shouldReserveTrackNumberColumn(isCurrent)
            val showTrackNumberText = media != null && !isCurrent && Settings.showTrackNumber && !forceNoTracks
            val trackNumberText = if (showTrackNumberText && (media?.trackNumber ?: 0) > 0) "${media?.trackNumber}." else ""
            val trackNumberContentDescription = media
                    ?.takeIf { showTrackNumberText && it.trackNumber > 0 }
                    ?.let { TalkbackUtil.getTrackNumber(composeView.context, it) }
            val isPresent = media?.isPresent ?: true
            composeView.setContent {
                VLCBrowserItemRow(
                        title = media?.title.orEmpty(),
                        subtitle = media?.let { MediaUtils.getMediaSubtitle(it) },
                        selected = selected,
                        contentDescription = media?.let { TalkbackUtil.getAudioTrack(composeView.context, it) },
                        titleMaxLines = 1,
                        showArtwork = showTrackNumberColumn,
                        onClick = ::onRowClick,
                        onLongClick = ::onRowLongClick,
                        artworkContent = {
                            AlbumTrackLeadingContent(
                                    trackNumberText = trackNumberText,
                                    trackNumberContentDescription = trackNumberContentDescription,
                                    showTrackNumber = showTrackNumberText,
                                    isCurrent = isCurrent,
                                    playing = playing
                            )
                        },
                        badgeContent = {
                            if (canBeReordered && !inSelection) AudioBrowserMoveHandle(onTouch = ::onMoveTouch)
                            AudioBrowserBadges(item = item, card = false, isPresent = isPresent)
                            if (selected) AlbumTrackSelectedIcon()
                        },
                        moreActionContent = if (isPresent && !inSelection) {
                            { AudioBrowserMoreIcon() }
                        } else null,
                        onMoreClick = ::onMoreActionClick
                )
            }
        }

        private fun shouldReserveTrackNumberColumn(isCurrent: Boolean): Boolean {
            return when {
                isCurrent -> true
                Settings.showTrackNumber && !forceNoTracks -> true
                currentMedia != null && currentList?.contains(currentMedia) == true -> true
                else -> false
            }
        }

        private fun onRowClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onClick(composeView, position, it) }
            }
        }

        private fun onRowLongClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onLongClick(composeView, position, it) }
            }
        }

        private fun onMoreActionClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onCtxClick(composeView, position, it) }
            }
        }

        private fun onMoveTouch(event: MotionEvent): Boolean {
            if (listEventsHandler == null) return false
            if (multiSelectHelper.getSelectionCount() != 0) return false
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                listEventsHandler.onStartDrag(this)
                return true
            }
            return false
        }
    }

}

@Composable
private fun AlbumTrackLeadingContent(
        trackNumberText: String,
        trackNumberContentDescription: String?,
        showTrackNumber: Boolean,
        isCurrent: Boolean,
        playing: Boolean
) {
    Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
    ) {
        when {
            isCurrent -> AlbumTrackPlayingBars(playing = playing)
            showTrackNumber -> {
                Text(
                        text = trackNumberText,
                        color = VLCThemeDefaults.colors.listTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        modifier = if (trackNumberContentDescription != null) {
                            Modifier.semantics { contentDescription = trackNumberContentDescription }
                        } else {
                            Modifier
                        }
                )
            }
        }
    }
}

@Composable
private fun AlbumTrackPlayingBars(playing: Boolean) {
    val context = LocalContext.current
    val barColor = Color(UiTools.getColorFromAttribute(context, R.attr.mini_visualizer_color))
    val heights = if (playing) {
        val transition = rememberInfiniteTransition(label = "albumTrackPlayingBars")
        val first by transition.animateFloat(
                initialValue = 6F,
                targetValue = 24F,
                animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 420, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                ),
                label = "albumTrackPlayingBars1"
        )
        val second by transition.animateFloat(
                initialValue = 24F,
                targetValue = 7F,
                animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 520, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                ),
                label = "albumTrackPlayingBars2"
        )
        val third by transition.animateFloat(
                initialValue = 10F,
                targetValue = 28F,
                animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 460, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                ),
                label = "albumTrackPlayingBars3"
        )
        listOf(first.dp, second.dp, third.dp)
    } else {
        listOf(4.dp, 4.dp, 4.dp)
    }

    Row(
            modifier = Modifier.size(32.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { height ->
            Box(
                    modifier = Modifier
                            .width(4.dp)
                            .height(height)
                            .background(barColor)
            )
        }
    }
}

@Composable
private fun AlbumTrackSelectedIcon() {
    Icon(
            painter = painterResource(R.drawable.ic_video_grid_check),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
    )
}
