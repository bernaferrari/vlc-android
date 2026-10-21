package org.videolan.television.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.Artist
import org.videolan.medialibrary.interfaces.media.Folder
import org.videolan.medialibrary.interfaces.media.Genre
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.interfaces.media.VideoGroup
import org.videolan.medialibrary.media.DummyItem
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.CATEGORY_ALBUMS
import org.videolan.resources.CATEGORY_ARTISTS
import org.videolan.resources.CATEGORY_GENRES
import org.videolan.resources.CATEGORY_NOW_PLAYING
import org.videolan.resources.CATEGORY_NOW_PLAYING_PAUSED
import org.videolan.resources.CATEGORY_NOW_PLAYING_PIP
import org.videolan.resources.CATEGORY_NOW_PLAYING_PIP_PAUSED
import org.videolan.resources.CATEGORY_SONGS
import org.videolan.resources.HEADER_DIRECTORIES
import org.videolan.resources.HEADER_MOVIES
import org.videolan.resources.HEADER_NETWORK
import org.videolan.resources.HEADER_PERMISSION
import org.videolan.resources.HEADER_PLAYLISTS
import org.videolan.resources.HEADER_SERVER
import org.videolan.resources.HEADER_STREAM
import org.videolan.resources.HEADER_TV_SHOW
import org.videolan.resources.HEADER_VIDEO
import org.videolan.tools.HttpImageLoader
import org.videolan.tools.Settings
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.tv.VLCTvSymbolArtwork
import org.videolan.vlc.compose.icons.ExtraSymbols
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.isSchemeHttpOrHttps
import org.videolan.vlc.R as VlcR

/**
 * Remote (scraped) artwork: posters, backdrops and cast portraits. Falls back to the shared tonal
 * symbol tile while loading or when the image is missing, so every card keeps the same shape.
 */
@Composable
internal fun TvRemoteArtworkImage(
    imageUrl: String?,
    fallback: MaterialIcon,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        val uri = imageUrl?.takeIf { it.isNotBlank() }?.toUri()
        value = if (uri != null && isSchemeHttpOrHttps(uri.scheme)) {
            withContext(Dispatchers.IO) { HttpImageLoader.downloadBitmap(imageUrl) }
        } else {
            null
        }
    }
    TvArtworkImage(bitmap, fallback, modifier, contentScale, contentDescription)
}

/** Medialibrary thumbnail (video frame, album cover...) with the item's symbol as fallback. */
@Composable
internal fun TvMediaArtworkImage(
    item: MediaLibraryItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val imageWidth = remember(context) { context.resources.getDimensionPixelSize(VlcR.dimen.tv_grid_card_thumb_width) }
    val bitmap by produceState<Bitmap?>(initialValue = null, item, imageWidth, Settings.showVideoThumbs) {
        value = if (item.shouldUseTvFallbackArtworkOnly()) null else ThumbnailsProvider.obtainBitmap(item, imageWidth)
    }
    TvArtworkImage(bitmap, item.tvSymbol(), modifier, contentScale, contentDescription)
}

/** Already-decoded artwork (covers read by the host) with a symbol fallback. */
@Composable
internal fun TvArtworkImage(
    bitmap: Bitmap?,
    fallback: MaterialIcon,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    val cover = bitmap?.takeIf { it.width > 1 && it.height > 1 }
    val motion = LocalVLCMotion.current
    Crossfade(cover, modifier.fillMaxSize(), tween(if (motion.reducedMotion) 0 else 180), label = "tv-artwork") { image ->
        if (image == null) {
            VLCTvSymbolArtwork(fallback)
        } else {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun MediaLibraryItem.shouldUseTvFallbackArtworkOnly(): Boolean {
    if (this is DummyItem) return artworkMrl.isNullOrEmpty()
    if (Settings.showVideoThumbs) return false
    val isVideoMedia = this is MediaWrapper && type == MediaWrapper.TYPE_VIDEO
    return isVideoMedia || itemType == MediaLibraryItem.TYPE_VIDEO_GROUP || this is Folder
}

/** Shared symbol for any library entry point or media, replacing the legacy `*_big` drawables. */
internal fun MediaLibraryItem.tvSymbol(): MaterialIcon = when (this) {
    is DummyItem -> when (id) {
        HEADER_VIDEO -> MaterialSymbols.Filled.VideoLibrary
        HEADER_MOVIES, CATEGORY_NOW_PLAYING_PIP -> ExtraSymbols.Movie
        HEADER_TV_SHOW -> ExtraSymbols.Tv
        HEADER_PERMISSION -> MaterialSymbols.Filled.Lock
        HEADER_DIRECTORIES -> MaterialSymbols.Filled.Folder
        HEADER_NETWORK -> ExtraSymbols.Cloud
        HEADER_SERVER -> MaterialSymbols.Filled.Add
        HEADER_STREAM -> MaterialSymbols.Filled.Language
        HEADER_PLAYLISTS -> MaterialSymbols.Filled.QueueMusic
        CATEGORY_ARTISTS -> MaterialSymbols.Filled.Groups
        CATEGORY_ALBUMS -> ExtraSymbols.Album
        CATEGORY_GENRES -> MaterialSymbols.Filled.Tune
        CATEGORY_SONGS -> MaterialSymbols.Filled.MusicNote
        CATEGORY_NOW_PLAYING -> ExtraSymbols.Equalizer
        CATEGORY_NOW_PLAYING_PAUSED, CATEGORY_NOW_PLAYING_PIP_PAUSED -> MaterialSymbols.Filled.Pause
        else -> MaterialSymbols.Filled.PlayArrow
    }
    is MediaWrapper -> when (type) {
        MediaWrapper.TYPE_VIDEO -> ExtraSymbols.Movie
        MediaWrapper.TYPE_AUDIO -> MaterialSymbols.Filled.MusicNote
        MediaWrapper.TYPE_DIR -> if (uri?.scheme == "file") MaterialSymbols.Filled.Folder else ExtraSymbols.Cloud
        MediaWrapper.TYPE_STREAM -> MaterialSymbols.Filled.Language
        else -> MaterialSymbols.Filled.PlayArrow
    }
    is Album -> ExtraSymbols.Album
    is Artist -> ExtraSymbols.Person
    is Genre -> MaterialSymbols.Filled.Tune
    is Playlist -> MaterialSymbols.Filled.QueueMusic
    is VideoGroup -> MaterialSymbols.Filled.VideoLibrary
    is Folder -> MaterialSymbols.Filled.Folder
    else -> MaterialSymbols.Filled.PlayArrow
}
