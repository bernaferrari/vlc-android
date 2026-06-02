/*****************************************************************************
 * TvAudioPlaylist.kt
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
import android.widget.ImageView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.television.R
import org.videolan.vlc.gui.helpers.getBitmapFromDrawable
import org.videolan.vlc.gui.helpers.loadImage
import org.videolan.vlc.media.MediaUtils

@Composable
internal fun TvAudioPlaylist(
    items: List<MediaWrapper>,
    selectedItem: Int,
    playing: Boolean,
    focusEnabled: Boolean,
    scrollTarget: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollTarget, items.size) {
        if (scrollTarget in items.indices) listState.animateScrollToItem(scrollTarget)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.black_transparent_20))
            .padding(top = dimensionResource(R.dimen.tv_overscan_vertical))
    ) {
        itemsIndexed(items) { index, media ->
            TvPlaylistRow(
                media = media,
                current = selectedItem == index,
                playing = playing && selectedItem == index,
                focusEnabled = focusEnabled,
                onClick = { onItemClick(index) }
            )
        }
    }
}

@Composable
private fun TvPlaylistRow(
    media: MediaWrapper,
    current: Boolean,
    playing: Boolean,
    focusEnabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusShape = RoundedCornerShape(topEnd = 48.dp, bottomEnd = 48.dp)
    val rowBackground = when {
        focused -> Color.White.copy(alpha = 0.20F)
        current -> Color.White.copy(alpha = 0.07F)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .padding(end = dimensionResource(R.dimen.tv_overscan_horizontal))
            .background(rowBackground, focusShape)
            .focusProperties { canFocus = focusEnabled }
            .focusable(enabled = focusEnabled, interactionSource = interactionSource)
            .clickable(
                enabled = focusEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
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
                TvPlaylistCover(media = media)
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
private fun TvPlaylistCover(media: MediaWrapper) {
    val context = LocalContext.current
    val defaultCover = remember {
        BitmapDrawable(context.resources, getBitmapFromDrawable(context, R.drawable.ic_song_background))
    }

    AndroidView(
        factory = { viewContext ->
            ImageView(viewContext).apply {
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
