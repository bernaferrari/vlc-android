/*****************************************************************************
 * PlaylistAdapter.kt
 *
 * Copyright © 2014-2015 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
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
package org.videolan.television.ui.audioplayer

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.television.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.DiffUtilAdapter
import org.videolan.vlc.gui.helpers.getBitmapFromDrawable
import org.videolan.vlc.gui.helpers.loadImage
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.viewmodels.PlaylistModel

class PlaylistAdapter
internal constructor(private val audioPlayerActivity: AudioPlayerActivity, val model: PlaylistModel) : DiffUtilAdapter<MediaWrapper, PlaylistAdapter.ViewHolder>() {
    var selectedItem = -1
        private set
    private var defaultCoverAudio: BitmapDrawable = BitmapDrawable(audioPlayerActivity.resources, getBitmapFromDrawable(audioPlayerActivity, R.drawable.ic_song_background))

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        ComposeView(parent.context).apply {
            isFocusable = true
            isClickable = true
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundResource(R.drawable.rectangle_circle_right_white_selector)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
    ), View.OnClickListener {
        private var rowState by mutableStateOf(TvPlaylistRowState())

        init {
            itemView.setOnClickListener(this)
            (itemView as ComposeView).setContent {
                VLCTheme(darkTheme = true) {
                    rowState.media?.let { media ->
                        TvPlaylistRow(
                            media = media,
                            current = rowState.current,
                            playing = rowState.playing,
                            defaultCover = defaultCoverAudio
                        )
                    }
                }
            }
        }

        override fun onClick(v: View) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            setSelection(position)
            audioPlayerActivity.playSelection()
        }

        fun bind(media: MediaWrapper, current: Boolean, playing: Boolean) {
            rowState = TvPlaylistRowState(
                media = media,
                current = current,
                playing = playing
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataset[position], selectedItem == position, model.playing)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNullOrEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else {
            val isCurrent = payloads[0] as Boolean
            holder.bind(dataset[position], isCurrent, isCurrent && model.playing)
        }
    }

    fun setSelection(pos: Int) {
        if (pos == selectedItem) return
        val previous = selectedItem
        selectedItem = pos
        if (previous != -1) notifyItemChanged(previous, false)
        if (pos != -1) notifyItemChanged(selectedItem, true)
    }

    fun refreshCurrentPlayingState() {
        if (selectedItem != -1) notifyItemChanged(selectedItem, true)
    }

    override fun onUpdateFinished() {
        audioPlayerActivity.onUpdateFinished()
    }

    companion object {
        const val TAG = "VLC/PlaylistAdapter"
    }
}

private data class TvPlaylistRowState(
    val media: MediaWrapper? = null,
    val current: Boolean = false,
    val playing: Boolean = false
)

@Composable
private fun TvPlaylistRow(
    media: MediaWrapper,
    current: Boolean,
    playing: Boolean,
    defaultCover: BitmapDrawable
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .padding(end = dimensionResource(R.dimen.tv_overscan_horizontal)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                .size(58.dp),
            contentAlignment = Alignment.Center
        ) {
            if (current) {
                TvPlaylistPlayingIndicator(playing)
            } else {
                TvPlaylistCover(media = media, defaultCover = defaultCover)
            }
        }

        Column(
            modifier = Modifier
                .weight(1F)
                .padding(start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = MediaUtils.getMediaTitle(media),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = MediaUtils.getMediaSubtitle(media),
                color = Color.White.copy(alpha = 0.6F),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TvPlaylistCover(
    media: MediaWrapper,
    defaultCover: BitmapDrawable
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { image ->
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            image.setImageDrawable(defaultCover)
            loadImage(image, media, tv = true, card = true)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TvPlaylistPlayingIndicator(playing: Boolean) {
    val heights = if (playing) {
        val transition = rememberInfiniteTransition(label = "tvPlaylistPlaying")
        val first by transition.animateFloat(
            initialValue = 14F,
            targetValue = 30F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 420, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "tvPlaylistPlayingBar1"
        )
        val second by transition.animateFloat(
            initialValue = 30F,
            targetValue = 12F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "tvPlaylistPlayingBar2"
        )
        val third by transition.animateFloat(
            initialValue = 20F,
            targetValue = 28F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 460, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "tvPlaylistPlayingBar3"
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
                    .background(Color.White.copy(alpha = 0.5F))
            )
        }
    }
}
