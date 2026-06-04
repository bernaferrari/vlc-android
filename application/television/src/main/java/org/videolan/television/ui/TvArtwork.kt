package org.videolan.television.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import org.videolan.medialibrary.interfaces.media.Folder
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.tools.HttpImageLoader
import org.videolan.tools.Settings
import org.videolan.vlc.gui.helpers.getTvIconRes
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.isSchemeHttpOrHttps
import org.videolan.vlc.R as VlcR

@Composable
internal fun TvRemoteArtworkImage(
    imageUrl: String?,
    @DrawableRes placeholder: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val imageUri = remember(imageUrl) { imageUrl?.takeIf { it.isNotBlank() }?.toUri() }
    val bitmap by tvRemoteBitmap(imageUri)
    TvArtworkImage(
        bitmap = bitmap,
        placeholder = placeholder,
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription
    )
}

@Composable
internal fun TvMediaArtworkImage(
    item: MediaLibraryItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = item.title
) {
    val context = LocalContext.current
    val imageWidth = remember(context) {
        context.resources.getDimensionPixelSize(VlcR.dimen.tv_grid_card_thumb_width)
    }
    val bitmap by produceState<Bitmap?>(initialValue = null, item, imageWidth, Settings.showVideoThumbs) {
        value = null
        value = if (item.shouldUseTvFallbackArtworkOnly()) {
            null
        } else {
            ThumbnailsProvider.obtainBitmap(item, imageWidth)
        }
    }
    TvArtworkImage(
        bitmap = bitmap,
        placeholder = getTvIconRes(item),
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription
    )
}

@Composable
private fun tvRemoteBitmap(imageUri: Uri?) = produceState<Bitmap?>(initialValue = null, imageUri) {
    val url = imageUri?.toString()
    value = null
    value = if (!url.isNullOrEmpty() && isSchemeHttpOrHttps(imageUri.scheme)) {
        HttpImageLoader.downloadBitmap(url)
    } else {
        null
    }
}

@Composable
private fun TvArtworkImage(
    bitmap: Bitmap?,
    @DrawableRes placeholder: Int,
    modifier: Modifier,
    contentScale: ContentScale,
    contentDescription: String?
) {
    val cover = bitmap?.takeIf { it.width > 1 && it.height > 1 }
    if (cover == null) {
        Image(
            painter = painterResource(placeholder),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            bitmap = cover.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun MediaLibraryItem.shouldUseTvFallbackArtworkOnly(): Boolean {
    if (Settings.showVideoThumbs) return false
    val isVideoMedia = this is MediaWrapper && type == MediaWrapper.TYPE_VIDEO
    return isVideoMedia || itemType == MediaLibraryItem.TYPE_VIDEO_GROUP || this is Folder
}
