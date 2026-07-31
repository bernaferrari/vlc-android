@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import kotlinx.coroutines.flow.first
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.AudioEntity
import org.videolan.vlc.repository.AudioEntityKind
import org.videolan.vlc.repository.ContainerKind
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.util.ContextOption
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.util.DefaultPlaybackAction

enum class MainTab {
    VIDEO, AUDIO, BROWSER, PLAYLISTS, MORE
}

enum class ViewMode { LIST, GRID }

/** UI sort picker — maps onto [MediaSort] for repository queries. */
enum class SortMode { TITLE, ARTIST, ALBUM, DURATION, RECENT, FILENAME }

enum class AudioSection { TRACKS, ARTISTS, ALBUMS, GENRES, PLAYLISTS }

enum class VideoGroupingMode { NONE, NAME, FOLDER }

data class MediaListUiState(
    val items: List<MediaItem> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val count: Int = 0,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortMode: SortMode = SortMode.TITLE,
    val sortDesc: Boolean = false,
    val onlyFavorites: Boolean = false,
    val selection: Set<String> = emptySet(), // by uri
    val sections: List<Pair<String, List<MediaItem>>> = emptyList(),
    val defaultPlaybackAction: String = DefaultPlaybackAction.PLAY.name,
    val containerId: Long? = null,
    val containerTitle: String? = null,
    val containerKind: ContainerKind = ContainerKind.NONE,
    val groups: List<MediaFolder> = emptyList(),
    val groupingMode: VideoGroupingMode = VideoGroupingMode.NONE,
    /** Typed artists/albums/genres when the platform exposes them. */
    val audioEntities: List<AudioEntity> = emptyList(),
    /** Non-null while drilled into an artist/album/genre. */
    val openedEntityTitle: String? = null,
    val showAllArtists: Boolean = false,
    val showTrackNumbers: Boolean = true,
    /** Whether the current repository can re-enumerate its local media source. */
    val supportsRescan: Boolean = false,
)
data class BrowserUiState(
    val favorites: List<MediaItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val networkRoots: List<MediaFolder> = emptyList(),
    val media: List<MediaItem> = emptyList(),
    val currentFolder: MediaFolder? = null,
    val stack: List<MediaFolder> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val selection: Set<String> = emptySet(),
    val defaultPlaybackAction: String = DefaultPlaybackAction.PLAY.name,
    val showHiddenFiles: Boolean = false,
    val showOnlyMultimedia: Boolean = false,
)

data class PlaylistsUiState(
    val playlists: List<PlaylistInfo> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val openPlaylistId: Long? = null,
    val openPlaylistName: String? = null,
    val openItems: List<MediaItem> = emptyList(),
    val onlyFavorites: Boolean = false,
    val sortDesc: Boolean = false,
    val query: String = "",
    val selection: Set<Long> = emptySet(),
    val viewMode: ViewMode = ViewMode.LIST,
    val defaultPlaybackAction: String = DefaultPlaybackAction.PLAY.name,
    val actionError: String? = null,
)

internal fun mainShellMediaRepo() = runCatching { VlcKoin.get().get<MediaRepository>() }
    .getOrElse { error("MediaRepository unavailable") }

internal fun mainShellPlaylistRepo() = runCatching { VlcKoin.get().get<PlaylistRepository>() }
    .getOrElse { error("PlaylistRepository unavailable") }

internal fun mainShellPlayback() = runCatching { PlaybackController.get() }
    .getOrElse { error("PlaybackController unavailable") }

internal fun SortMode.toMediaSort(): MediaSort = when (this) {
    SortMode.TITLE -> MediaSort.TITLE
    SortMode.ARTIST -> MediaSort.ARTIST
    SortMode.ALBUM -> MediaSort.ALBUM
    SortMode.DURATION -> MediaSort.DURATION
    SortMode.RECENT -> MediaSort.RECENT
    SortMode.FILENAME -> MediaSort.FILENAME
}

internal fun MediaSort.toSortMode(): SortMode = when (this) {
    MediaSort.ARTIST -> SortMode.ARTIST
    MediaSort.ALBUM -> SortMode.ALBUM
    MediaSort.DURATION -> SortMode.DURATION
    MediaSort.RECENT, MediaSort.INSERTION_DATE -> SortMode.RECENT
    MediaSort.FILENAME -> SortMode.FILENAME
    else -> SortMode.TITLE
}

internal fun AudioEntityKind.uriScheme(): String = when (this) {
    AudioEntityKind.ARTIST -> "artist"
    AudioEntityKind.ALBUM -> "album"
    AudioEntityKind.GENRE -> "genre"
}

internal fun AudioEntity.toSyntheticItem(): MediaItem {
    val scheme = kind.uriScheme()
    val desc = subtitle?.takeIf { it.isNotBlank() }
        ?: buildString {
            if (trackCount > 0) append("$trackCount tracks")
            if (albumCount > 0) {
                if (isNotEmpty()) append(" · ")
                append("$albumCount albums")
            }
        }.ifBlank { null }
    return MediaItem(
        id = id,
        title = title,
        uri = "$scheme://$id",
        type = MediaType.DIR,
        artworkUri = artworkUri,
        isFavorite = isFavorite,
        description = desc,
        artist = if (kind == AudioEntityKind.ARTIST) title else subtitle,
        album = if (kind == AudioEntityKind.ALBUM) title else null,
        genre = if (kind == AudioEntityKind.GENRE) title else null,
    )
}

internal fun String.isAudioEntityUri(): Boolean =
    startsWith("artist://") || startsWith("album://") || startsWith("genre://")

internal fun String.isPlaylistUri(): Boolean = startsWith("playlist://")

internal fun MediaItem.isVirtualAudioEntry(): Boolean =
    uri.isPlaylistUri() || uri.isAudioEntityUri()

internal fun VideoGroupingMode.isGrouped(): Boolean =
    this == VideoGroupingMode.NAME || this == VideoGroupingMode.FOLDER

internal fun parseAudioEntityUri(uri: String): Pair<AudioEntityKind, Long>? {
    val kind = when {
        uri.startsWith("artist://") -> AudioEntityKind.ARTIST
        uri.startsWith("album://") -> AudioEntityKind.ALBUM
        uri.startsWith("genre://") -> AudioEntityKind.GENRE
        else -> return null
    }
    val id = uri.substringAfter("://").toLongOrNull() ?: return null
    return kind to id
}

internal fun sortItems(items: List<MediaItem>, mode: SortMode, desc: Boolean): List<MediaItem> {
    val sorted = when (mode) {
        SortMode.TITLE -> items.sortedBy { it.displayTitle.lowercase() }
        SortMode.ARTIST -> items.sortedWith(
            compareBy({ it.artist?.lowercase().orEmpty() }, { it.displayTitle.lowercase() }),
        )
        SortMode.ALBUM -> items.sortedWith(
            compareBy({ it.album?.lowercase().orEmpty() }, { it.trackNumber }, { it.displayTitle.lowercase() }),
        )
        SortMode.DURATION -> items.sortedBy { it.duration }
        SortMode.RECENT -> items.sortedBy { it.lastPlayed }
        SortMode.FILENAME -> items.sortedBy { (it.fileName ?: it.displayTitle).lowercase() }
    }
    return if (desc) sorted.reversed() else sorted
}

internal fun sectionByArtist(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.artist?.takeIf { a -> a.isNotBlank() } ?: "Unknown artist" }
        .toList()
        .sortedBy { it.first.lowercase() }

internal fun sectionByAlbum(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.album?.takeIf { a -> a.isNotBlank() } ?: "Unknown album" }
        .toList()
        .sortedBy { it.first.lowercase() }

internal fun sectionByGenre(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.genre?.takeIf { g -> g.isNotBlank() } ?: "Unknown genre" }
        .toList()
        .sortedBy { it.first.lowercase() }

internal fun prefsOrNull(): VlcPreferences? =
    runCatching { VlcKoin.get().get<VlcPreferences>() }.getOrNull()

internal fun persistBool(key: String, value: Boolean) {
    SettingsWriteBridge.onBoolean?.invoke(key, value)
}

internal fun persistString(key: String, value: String) {
    SettingsWriteBridge.onString?.invoke(key, value)
}

/** Apply the configured primary-click playback action. */
internal fun applyDefaultPlayback(
    action: DefaultPlaybackAction,
    item: MediaItem,
    queue: List<MediaItem>,
    player: PlaybackController,
) {
    val list = queue.ifEmpty { listOf(item) }
    when (action) {
        DefaultPlaybackAction.PLAY -> player.play(item, list)
        DefaultPlaybackAction.PLAY_ALL -> {
            val idx = list.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
            player.playFromIndex(list, idx)
        }
        DefaultPlaybackAction.ADD_TO_QUEUE -> player.append(listOf(item))
        DefaultPlaybackAction.INSERT_NEXT -> player.insertNext(listOf(item))
    }
}

/** Primary queue-only actions mutate playback state without opening the full player route. */
internal fun defaultPlaybackActionOpensPlayer(name: String?): Boolean = when (
    DefaultPlaybackAction.fromName(name)
) {
    DefaultPlaybackAction.PLAY,
    DefaultPlaybackAction.PLAY_ALL,
    -> true
    DefaultPlaybackAction.ADD_TO_QUEUE,
    DefaultPlaybackAction.INSERT_NEXT,
    -> false
}

internal fun isUriBrowseTarget(folder: MediaFolder): Boolean {
    when (folder.kind) {
        FolderKind.NETWORK, FolderKind.STORAGE -> return true
        FolderKind.MEDIA_FOLDER -> {
            val uri = folder.uri.ifBlank { folder.path }
            if (uri.isNotBlank()) return true
        }
        else -> Unit
    }
    val uri = folder.uri.ifBlank { folder.path }
    if (uri.isBlank()) return false
    val scheme = uri.substringBefore(':', missingDelimiterValue = "").lowercase()
    return scheme in URI_BROWSE_SCHEMES
}

internal val URI_BROWSE_SCHEMES = setOf(
    "file", "content", "otg",
    "smb", "ftp", "ftps", "sftp", "upnp", "nfs", "http", "https", "rtp", "rtsp",
)

internal val HOST_CONTEXT_OPTIONS = setOf(
    ContextOption.CTX_DELETE,
    ContextOption.CTX_RENAME,
    ContextOption.CTX_INFORMATION,
    ContextOption.CTX_SHARE,
    ContextOption.CTX_DOWNLOAD_SUBTITLES,
    ContextOption.CTX_ADD_SHORTCUT,
    ContextOption.CTX_SET_RINGTONE,
    ContextOption.CTX_BAN_FOLDER,
    ContextOption.CTX_ADD_TO_PLAYLIST,
)
