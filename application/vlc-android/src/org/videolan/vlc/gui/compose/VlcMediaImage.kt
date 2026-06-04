/*
 * ************************************************************************
 *  VlcMediaImage.kt
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

package org.videolan.vlc.gui.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.tools.Settings
import org.videolan.vlc.util.ThumbnailsProvider

/**
 * Compose-native media artwork host backed by VLC's existing thumbnail pipeline.
 *
 * This keeps migrated Compose screens on [Image] instead of hosting legacy
 * ImageViews while preserving ThumbnailsProvider caching and video-thumbnail
 * preferences.
 */
@Composable
internal fun VlcMediaImage(
    item: MediaLibraryItem,
    width: Dp,
    fallbackPainter: Painter,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    fallbackColorFilter: ColorFilter? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    val itemUri = (item as? MediaWrapper)?.uri
    val thumbnail by produceState<Bitmap?>(null, item.id, itemUri, item.artworkMrl, widthPx) {
        value = if (!Settings.showVideoThumbs && item is MediaWrapper && item.type == MediaWrapper.TYPE_VIDEO) {
            null
        } else {
            ThumbnailsProvider.obtainBitmap(item, widthPx)
        }
    }

    val bitmap = thumbnail
    if (bitmap == null) {
        Image(
            painter = fallbackPainter,
            contentDescription = contentDescription,
            colorFilter = fallbackColorFilter,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
