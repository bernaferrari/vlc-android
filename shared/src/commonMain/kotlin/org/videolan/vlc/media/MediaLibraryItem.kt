package org.videolan.vlc.media

/**
 * Platform-agnostic media item abstraction.
 *
 * Mirrors the core read-only properties of `org.videolan.medialibrary.media.MediaLibraryItem`
 * without any Android/Parcelable/JNI dependencies. Platform implementations delegate
 * to the native medialibrary types.
 */
interface MediaLibraryItem {
    val id: Long
    val title: String
    val description: String?
    val favorite: Boolean
    val itemType: Int

    fun tracksCount(): Int

    companion object {
        const val TYPE_ALBUM       = 1 shl 1
        const val TYPE_ARTIST      = 1 shl 2
        const val TYPE_GENRE       = 1 shl 3
        const val TYPE_PLAYLIST    = 1 shl 4
        const val TYPE_MEDIA       = 1 shl 5
        const val TYPE_DUMMY       = 1 shl 6
        const val TYPE_STORAGE     = 1 shl 7
        const val TYPE_HISTORY     = 1 shl 9
        const val TYPE_FOLDER      = 1 shl 10
        const val TYPE_VIDEO_GROUP = 1 shl 11
        const val TYPE_BOOKMARK    = 1 shl 12

        const val FLAG_NONE = 0
        const val FLAG_SELECTED = 1
        const val FLAG_FAVORITE = 1 shl 1
        const val FLAG_STORAGE = 1 shl 2
    }

    enum class MediaType { Unknown, Video, Audio, External, Stream }
}
