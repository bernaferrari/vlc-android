package org.videolan.vlc.media

/**
 * Platform-agnostic media wrapper abstraction.
 *
 * Mirrors the core read-only properties of `org.videolan.medialibrary.interfaces.media.MediaWrapper`
 * without Android Uri/Bitmap/Parcelable/JNI dependencies.
 *
 * Platform implementations delegate to the native medialibrary types.
 */
interface MediaWrapper : MediaLibraryItem {
    val location: String
    val uri: String
    val filename: String?
    val type: Int
    val length: Long
    val width: Int
    val height: Int
    val time: Long
    val position: Float
    val audioTrack: Int
    val spuTrack: Int
    val artistId: Long
    val albumArtistId: Long
    val artistName: String?
    val albumArtistName: String?
    val genre: String?
    val albumId: Long
    val albumName: String?
    val trackNumber: Int
    val discNumber: Int
    val rating: String?
    val date: String?
    val releaseYear: Int
    val artworkURL: String?
    val nowPlaying: String?
    val publisher: String?
    val displayTitle: String?
    val playCount: Long
    val lastModified: Long
    val insertionDate: Long
    val seen: Long
    val present: Boolean
    val tag: String?

    companion object {
        const val TYPE_ALL = -1
        const val TYPE_VIDEO = 0
        const val TYPE_AUDIO = 1
        const val TYPE_GROUP = 2
        const val TYPE_DIR = 3
        const val TYPE_SUBTITLE = 4
        const val TYPE_PLAYLIST = 5
        const val TYPE_STREAM = 6

        const val MEDIA_VIDEO = 0x01
        const val MEDIA_NO_HWACCEL = 0x02
        const val MEDIA_PAUSED = 0x4
        const val MEDIA_FORCE_AUDIO = 0x8
        const val MEDIA_BENCHMARK = 0x10
        const val MEDIA_FROM_START = 0x20
        const val MEDIA_NO_PARSE = 0x40
    }
}
