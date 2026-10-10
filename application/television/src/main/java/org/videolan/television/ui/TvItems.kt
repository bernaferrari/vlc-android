package org.videolan.television.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.Artist
import org.videolan.medialibrary.interfaces.media.Genre
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.DummyItem
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.moviepedia.database.models.MediaImage
import org.videolan.moviepedia.database.models.MediaImageType
import org.videolan.moviepedia.database.models.MediaMetadataType
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
import org.videolan.moviepedia.database.models.Person
import org.videolan.moviepedia.database.models.tvEpisodeSubtitle
import org.videolan.resources.CATEGORY_ALBUMS
import org.videolan.resources.CATEGORY_ARTISTS
import org.videolan.resources.CATEGORY_GENRES
import org.videolan.resources.CATEGORY_NOW_PLAYING
import org.videolan.resources.CATEGORY_NOW_PLAYING_PAUSED
import org.videolan.resources.CATEGORY_NOW_PLAYING_PIP
import org.videolan.resources.CATEGORY_NOW_PLAYING_PIP_PAUSED
import org.videolan.resources.CATEGORY_SONGS
import org.videolan.resources.HEADER_PLAYLISTS
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.VLCTvArtwork
import org.videolan.vlc.compose.tv.VLCTvCardShape
import org.videolan.vlc.compose.tv.VLCTvItem
import org.videolan.vlc.compose.tv.VLCTvSymbolArtwork
import org.videolan.vlc.compose.tv.VLCTvSymbols
import org.videolan.vlc.util.generateResolutionClass

/**
 * Maps host objects (medialibrary items, scraped metadata, cast, artwork choices) onto the shared
 * [VLCTvItem] model and keeps the association so the artwork slot can load the right thumbnail.
 */
internal class TvEntries<T : Any>(val sources: List<T>, map: (T) -> VLCTvItem) {
    val items: List<VLCTvItem> = sources.map(map)
    val sourcesByKey: Map<String, T> = items.indices.associate { items[it].key to sources[it] }
    val artwork: VLCTvArtwork = { item -> TvSourceArtwork(sourcesByKey[item.key], item) }
    operator fun get(index: Int): T? = sources.getOrNull(index)
}

/** Artwork renderer for any source object a TV screen may show. */
@Composable
internal fun BoxScope.TvSourceArtwork(source: Any?, item: VLCTvItem) {
    when (source) {
        is MediaMetadataWithImages -> TvRemoteArtworkImage(
            imageUrl = source.metadata.currentPoster,
            fallback = if (source.metadata.type == MediaMetadataType.TV_SHOW) VLCTvSymbols.Tv else VLCTvSymbols.Movie,
            contentDescription = item.title,
        )
        is Person -> TvRemoteArtworkImage(source.image, VLCTvSymbols.Person, contentDescription = item.title)
        is MediaImage -> TvRemoteArtworkImage(source.url, VLCTvSymbols.Image)
        is MediaLibraryItem -> TvMediaArtworkImage(source, contentDescription = item.title)
        else -> VLCTvSymbolArtwork(item.symbol)
    }
}

internal fun MediaLibraryItem.tvKey(): String = when (this) {
    is MediaWrapper -> "media-$id-${uri}"
    else -> "$itemType-$id-$title"
}

/** Video-like entries are 16:9, music collections square. */
internal fun MediaLibraryItem.tvShape(): VLCTvCardShape = when (this) {
    is Album, is Artist, is Genre, is Playlist -> VLCTvCardShape.Square
    is MediaWrapper -> if (type == MediaWrapper.TYPE_AUDIO) VLCTvCardShape.Square else VLCTvCardShape.Landscape
    is DummyItem -> when (id) {
        CATEGORY_ARTISTS, CATEGORY_ALBUMS, CATEGORY_GENRES, CATEGORY_SONGS, HEADER_PLAYLISTS,
        CATEGORY_NOW_PLAYING, CATEGORY_NOW_PLAYING_PAUSED -> VLCTvCardShape.Square
        else -> VLCTvCardShape.Landscape
    }
    else -> VLCTvCardShape.Landscape
}

internal fun MediaLibraryItem.toTvItem(shape: VLCTvCardShape = tvShape()): VLCTvItem {
    val media = this as? MediaWrapper
    val isVideo = media?.type == MediaWrapper.TYPE_VIDEO
    val progress = if (isVideo && media.length > 0 && media.displayTime > 0) media.displayTime.toFloat() / media.length else null
    val subtitle = when {
        media != null && media.type != MediaWrapper.TYPE_DIR && media.length > 0 && description.isNullOrBlank() -> Tools.millisToString(media.length)
        else -> description
    }
    return VLCTvItem(
        key = tvKey(),
        title = title.orEmpty(),
        subtitle = subtitle,
        badge = if (isVideo) generateResolutionClass(media.width, media.height) else null,
        progress = progress,
        symbol = tvSymbol(),
        statusIcon = when ((this as? DummyItem)?.id) {
            CATEGORY_NOW_PLAYING, CATEGORY_NOW_PLAYING_PIP -> VLCTvSymbols.Equalizer
            CATEGORY_NOW_PLAYING_PAUSED, CATEGORY_NOW_PLAYING_PIP_PAUSED -> MaterialSymbols.Filled.Pause
            else -> null
        },
        shape = shape,
    )
}

internal fun MediaMetadataWithImages.toTvItem(subtitle: String? = tvEpisodeSubtitle().takeIf { it.isNotBlank() }): VLCTvItem {
    val media = media
    val progress = media?.let { if (it.length > 0 && it.displayTime > 0) it.displayTime.toFloat() / it.length else null }
    return VLCTvItem(
        key = "metadata-${metadata.moviepediaId}-${metadata.mlId}",
        title = metadata.title,
        subtitle = subtitle,
        badge = media?.let { generateResolutionClass(it.width, it.height) },
        progress = progress,
        symbol = if (metadata.type == MediaMetadataType.TV_SHOW) VLCTvSymbols.Tv else VLCTvSymbols.Movie,
        statusIcon = if ((media?.seen ?: 0L) > 0L) MaterialSymbols.Filled.CheckCircle else null,
        shape = VLCTvCardShape.Poster,
    )
}

internal fun Person.toTvItem() = VLCTvItem(
    key = "person-$moviepediaId",
    title = name,
    symbol = VLCTvSymbols.Person,
    shape = VLCTvCardShape.Poster,
)

internal fun MediaImage.toTvItem() = VLCTvItem(
    key = "image-$mediaId-$url",
    title = "",
    symbol = VLCTvSymbols.Image,
    shape = if (imageType == MediaImageType.POSTER) VLCTvCardShape.Poster else VLCTvCardShape.Landscape,
)
