package org.videolan.vlc.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.PlaylistInfo

/** Shared page size — matches legacy MEDIALIBRARY_PAGE_SIZE. */
const val MEDIA_PAGE_SIZE = 500

/**
 * Sort keys for library lists. Values align with common client-side fields;
 * Android maps them onto Medialibrary.SORT_* when paging from JNI.
 */
enum class MediaSort {
    DEFAULT,
    TITLE,
    FILENAME,
    ARTIST,
    ALBUM,
    DURATION,
    RELEASE_DATE,
    LAST_MODIFIED,
    INSERTION_DATE,
    FILE_SIZE,
    TRACK_COUNT,
    RECENT,
}

data class MediaQuery(
    val type: MediaType = MediaType.ALL,
    val query: String = "",
    val sort: MediaSort = MediaSort.DEFAULT,
    val desc: Boolean = false,
    val onlyFavorites: Boolean = false,
    /** When set, scope to a video group / folder container id. */
    val containerId: Long? = null,
    val containerKind: ContainerKind = ContainerKind.NONE,
)

enum class ContainerKind { NONE, FOLDER, VIDEO_GROUP }

enum class AudioEntityKind { ARTIST, ALBUM, GENRE }

/**
 * Typed audio library entity (artist / album / genre) for drill-down UIs.
 * [id] is the medialibrary id; [tracksUri] is a synthetic key for list identity.
 */
data class AudioEntity(
    val id: Long,
    val title: String,
    val kind: AudioEntityKind,
    val trackCount: Int = 0,
    val albumCount: Int = 0,
    val artworkUri: String? = null,
    val subtitle: String? = null,
    val isFavorite: Boolean = false,
)

/**
 * Repository contract for accessing the media library.
 *
 * Each platform provides its own implementation:
 *   - Android: wraps the JNI medialibrary (paged)
 *   - iOS: in-process catalog + Files/Photos intake
 */
interface MediaRepository {
    fun observeMedia(type: MediaType): Flow<List<MediaItem>>

    /**
     * Paged observation — preferred path for large libraries.
     * Default falls back to full [observeMedia] / [search] sliced client-side.
     */
    fun observeMediaPaged(query: MediaQuery): Flow<PagingData<MediaItem>> =
        defaultPagedMedia(query)

    suspend fun getMedia(id: Long): MediaItem?

    suspend fun getMediaByIds(ids: List<Long>): List<MediaItem>

    fun search(query: String, type: MediaType = MediaType.ALL): Flow<List<MediaItem>>

    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<MediaItem>>

    suspend fun count(type: MediaType): Int

    suspend fun markAsPlayed(id: Long)

    suspend fun markAsUnplayed(id: Long) {}

    suspend fun incrementPlayCount(id: Long)

    suspend fun setFavorite(id: Long, favorite: Boolean) {}

    /** Optional folder tree — empty if platform has no browser roots yet. */
    fun observeFolders(parentId: Long? = null): Flow<List<MediaFolder>> = flowOf(emptyList())

    /** Media contained in a folder path/id. */
    fun observeFolderMedia(folderId: Long): Flow<List<MediaItem>> = flowOf(emptyList())

    /** Browser favorites (network / custom roots). Empty by default. */
    fun observeBrowserFavorites(): Flow<List<MediaItem>> = flowOf(emptyList())

    /** Network roots when platform supports discovery. */
    fun observeNetworkRoots(): Flow<List<MediaFolder>> = flowOf(emptyList())

    /**
     * Browse a network or filesystem URI (libVLC MediaBrowser on Android).
     * Returns folders + media for one level. Empty default.
     */
    fun browseUri(uri: String): Flow<BrowserListing> = flowOf(BrowserListing())

    fun observeVideoGroups(
        sort: MediaSort = MediaSort.TITLE,
        desc: Boolean = false,
        onlyFavorites: Boolean = false,
    ): Flow<List<MediaFolder>> = flowOf(emptyList())

    fun observeVideoGroupMedia(groupId: Long): Flow<List<MediaItem>> = flowOf(emptyList())

    /**
     * Media-library video folders (Folder.TYPE_FOLDER_VIDEO). Empty default.
     * Used by VideoGroupingMode.FOLDER.
     */
    fun observeVideoFolders(
        sort: MediaSort = MediaSort.TITLE,
        desc: Boolean = false,
        onlyFavorites: Boolean = false,
    ): Flow<List<MediaFolder>> = flowOf(emptyList())

    /** Typed artists / albums / genres for audio browser tabs. */
    fun observeAudioEntities(
        kind: AudioEntityKind,
        sort: MediaSort = MediaSort.TITLE,
        desc: Boolean = false,
        onlyFavorites: Boolean = false,
        query: String = "",
    ): Flow<List<AudioEntity>> = flowOf(emptyList())

    /** Tracks belonging to a typed audio entity. */
    fun observeAudioEntityTracks(
        kind: AudioEntityKind,
        entityId: Long,
        sort: MediaSort = MediaSort.TITLE,
        desc: Boolean = false,
        onlyFavorites: Boolean = false,
    ): Flow<List<MediaItem>> = flowOf(emptyList())
}

/** One level of browser results (network or filesystem). */
data class BrowserListing(
    val folders: List<MediaFolder> = emptyList(),
    val media: List<MediaItem> = emptyList(),
)

/**
 * Repository contract for managing playlists.
 */
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<PlaylistInfo>>

    fun observePlaylistsPaged(
        sort: MediaSort = MediaSort.TITLE,
        desc: Boolean = false,
        onlyFavorites: Boolean = false,
        query: String = "",
    ): Flow<PagingData<PlaylistInfo>> = defaultPagedPlaylists(sort, desc, onlyFavorites, query)

    suspend fun getPlaylist(id: Long): Playlist?

    suspend fun createPlaylist(name: String): Playlist

    suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>)

    suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>)

    /** Remove the track at a 0-based playlist position, preserving duplicate entries. */
    suspend fun removeFromPlaylistAt(playlistId: Long, index: Int)

    /** Move track within playlist. Positions are 0-based indices in the playlist. */
    suspend fun moveInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {}

    suspend fun deletePlaylist(id: Long)

    suspend fun renamePlaylist(id: Long, name: String)

    suspend fun setFavorite(id: Long, favorite: Boolean) {}
}

/**
 * Repository contract for media history.
 */
interface HistoryRepository {
    fun observeHistory(limit: Int = 100): Flow<List<HistoryEntry>>

    suspend fun addToHistory(item: MediaItem)

    suspend fun clearHistory()

    suspend fun removeHistoryEntry(id: Long)

    suspend fun moveUp(id: Long) {}
}

/**
 * Named network / MRL streams (More hub).
 */
interface StreamRepository {
    fun observeStreams(): Flow<List<MediaItem>>

    suspend fun addStream(title: String, uri: String): MediaItem?

    suspend fun renameStream(id: Long, title: String)

    suspend fun deleteStream(id: Long)
}
