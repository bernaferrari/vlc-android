package org.videolan.vlc.media

/**
 * Platform-agnostic album abstraction.
 * Mirrors core properties of `org.videolan.medialibrary.interfaces.media.Album`.
 */
interface Album : MediaLibraryItem {
    val albumArtistId: Long
    val albumArtistName: String?
    val trackCount: Int
    val duration: Long
    val releaseYear: Int
    val artworkMrl: String?

    fun tracks(): List<MediaWrapper>?
    fun artists(): List<Artist>?
}

/**
 * Platform-agnostic artist abstraction.
 * Mirrors core properties of `org.videolan.medialibrary.interfaces.media.Artist`.
 */
interface Artist : MediaLibraryItem {
    val thumbnail: String?
    fun tracks(): List<MediaWrapper>?
    fun albums(): List<Album>?
}

/**
 * Platform-agnostic genre abstraction.
 * Mirrors core properties of `org.videolan.medialibrary.interfaces.media.Genre`.
 */
interface Genre : MediaLibraryItem {
    fun tracks(): List<MediaWrapper>?
    fun albums(): List<Album>?
    fun artists(): List<Artist>?
}

/**
 * Platform-agnostic playlist abstraction.
 * Mirrors core properties of `org.videolan.medialibrary.interfaces.media.Playlist`.
 */
interface Playlist : MediaLibraryItem {
    val trackCount: Int
    val duration: Long
    fun tracks(): List<MediaWrapper>?
}

/**
 * Platform-agnostic folder abstraction.
 * Mirrors core properties of `org.videolan.medialibrary.interfaces.media.Folder`.
 */
interface Folder : MediaLibraryItem {
    fun media(type: Int): List<MediaWrapper>?
    fun subFolders(): List<Folder>?
}

/**
 * Platform-agnostic video group abstraction.
 * Mirrors core properties of `org.videolan.medialibrary.interfaces.media.VideoGroup`.
 */
interface VideoGroup : MediaLibraryItem {
    val videoCount: Int
    val duration: Long
    fun media(): List<MediaWrapper>?
}

/**
 * Platform-agnostic bookmark abstraction.
 * Mirrors core properties of `org.videolan.medialibrary.interfaces.media.Bookmark`.
 */
interface Bookmark {
    val id: Long
    val name: String?
    val time: Long
    val description: String?
}
