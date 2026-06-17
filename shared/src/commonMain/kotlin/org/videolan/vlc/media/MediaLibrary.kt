package org.videolan.vlc.media

/**
 * Platform-agnostic sorting constants for media queries.
 *
 * Mirrors the sorting constants from `org.videolan.medialibrary.interfaces.Medialibrary`.
 */
object MediaSort {
    const val DEFAULT = 0
    const val ALPHA = 1
    const val DURATION = 2
    const val INSERTION_DATE = 3
    const val LAST_MODIFICATION_DATE = 4
    const val RELEASE_DATE = 5
    const val FILE_SIZE = 6
    const val ARTIST = 7
    const val PLAY_COUNT = 8
    const val ALBUM = 9
    const val FILENAME = 10
    const val TRACK_NUMBER = 11
    const val TRACK_ID = 12
    const val NB_VIDEO = 13
    const val NB_AUDIO = 14
    const val NB_MEDIA = 15
}

/**
 * Medialibrary initialization result codes.
 */
object MediaLibInit {
    const val SUCCESS = 0
    const val ALREADY_INITIALIZED = 1
    const val FAILED = 2
    const val DB_RESET = 3
    const val DB_CORRUPTED = 4
    const val DB_UNRECOVERABLE = 5
}

/**
 * History type constants.
 */
object MediaHistoryType {
    const val GLOBAL = 0
    const val LOCAL = 1
    const val NETWORK = 2
}

/**
 * Media update/add flag constants.
 */
object MediaFlags {
    const val UPDATED_AUDIO = 1 shl 0
    const val UPDATED_AUDIO_EMPTY = 1 shl 1
    const val UPDATED_VIDEO = 1 shl 2
    const val UPDATED_VIDEO_EMPTY = 1 shl 3
    const val ADDED_AUDIO = 1 shl 4
    const val ADDED_AUDIO_EMPTY = 1 shl 5
    const val ADDED_VIDEO = 1 shl 6
    const val ADDED_VIDEO_EMPTY = 1 shl 7
}

/**
 * Platform-agnostic medialibrary query interface.
 *
 * Provides read-only access to the media library without Android/JNI dependencies.
 * Platform implementations delegate to the native medialibrary (Android JNI, iOS, etc.).
 */
interface MediaLibrary {
    fun getMedia(id: Long): MediaWrapper?
    fun getVideoCount(): Int
    fun getAudioCount(): Int
    fun getAlbums(sort: Int = MediaSort.DEFAULT): List<Album>
    fun getArtists(sort: Int = MediaSort.DEFAULT): List<Artist>
    fun getGenres(sort: Int = MediaSort.DEFAULT): List<Genre>
    fun getPlaylists(sort: Int = MediaSort.DEFAULT): List<Playlist>
    fun getFolders(sort: Int = MediaSort.DEFAULT): List<Folder>
    fun search(query: String): SearchResult?

    companion object {
        lateinit var instance: MediaLibrary
    }
}

/**
 * Platform-agnostic search result aggregate.
 */
data class SearchResult(
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val tracks: List<MediaWrapper> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)
