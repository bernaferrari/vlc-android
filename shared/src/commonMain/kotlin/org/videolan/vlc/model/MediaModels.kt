package org.videolan.vlc.model

/**
 * Media type classification — shared across all platforms.
 *
 * Mirrors the Android MediaWrapper.TYPE_* constants but as a clean enum
 * usable from common code and Swift.
 */
enum class MediaType(val value: Int) {
    ALL(-1),
    VIDEO(0),
    AUDIO(1),
    GROUP(2),
    DIR(3),
    SUBTITLE(4),
    PLAYLIST(5),
    STREAM(6);

    companion object {
        fun fromValue(value: Int): MediaType = entries.firstOrNull { it.value == value } ?: ALL
    }
}

/**
 * Flags controlling how a media item is played back.
 * Bit positions match the Android MediaWrapper MEDIA_* constants.
 */
object MediaFlags {
    const val VIDEO = 0x01
    const val NO_HWACCEL = 0x02
    const val PAUSED = 0x04
    const val FORCE_AUDIO = 0x08
    const val BENCHMARK = 0x10
    const val FROM_START = 0x20
    const val NO_PARSE = 0x40
}

/**
 * Playback repeat mode — shared across all platforms.
 */
enum class RepeatMode {
    NONE,
    ONE,
    ALL;

    companion object {
        fun fromOrdinalSafe(ordinal: Int): RepeatMode =
            entries.getOrElse(ordinal) { NONE }
    }
}

/**
 * Resume playback behavior.
 */
enum class ResumeStatus {
    ALWAYS,
    ASK,
    NEVER
}

/**
 * Simple playback position holder.
 */
data class Progress(
    var time: Long = 0L,
    var length: Long = 0L
) {
    val progressPercent: Float
        get() = if (length > 0) (time.toFloat() / length).coerceIn(0f, 1f) else 0f

    val remainingTime: Long
        get() = (length - time).coerceAtLeast(0L)
}

/**
 * A/B repeat markers for a media item.
 */
data class ABRepeat(
    var start: Long = -1L,
    var stop: Long = -1L
) {
    val isActive: Boolean get() = start >= 0 && stop >= 0
}

/**
 * Audio/video delay values for playback.
 */
data class DelayValues(
    var start: Long = -1L,
    var stop: Long = -1L
)

/**
 * Core media item metadata — the platform-agnostic subset of what
 * Android's MediaWrapper carries.
 *
 * Platform-specific code (Android/iOS) wraps native media objects into
 * this common type so that shared business logic can operate uniformly.
 */
data class MediaItem(
    val id: Long,
    val title: String,
    val uri: String,
    val type: MediaType = MediaType.ALL,
    val duration: Long = 0L,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val artworkUri: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val mime: String? = null,
    val lastModified: Long = 0L,
    val size: Long = 0L,
    val rating: Float = 0f,
    val playedCount: Int = 0,
    val lastPlayed: Long = 0L,
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
    val isAudio: Boolean get() = type == MediaType.AUDIO
    val isStream: Boolean get() = type == MediaType.STREAM
    val isDirectory: Boolean get() = type == MediaType.DIR
    val displayTitle: String get() = if (title.isNotBlank()) title else uri.getFileName()
}

/**
 * A playlist of media items with playback position tracking.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val size: Int get() = items.size

    val current: MediaItem? get() = items.getOrNull(currentIndex)

    fun next(): MediaItem? {
        if (items.isEmpty()) return null
        val nextIndex = when {
            repeatMode == RepeatMode.ONE -> currentIndex
            shuffle -> (0 until items.size).random()
            currentIndex < items.lastIndex -> currentIndex + 1
            repeatMode == RepeatMode.ALL -> 0
            else -> return null
        }
        return items.getOrNull(nextIndex)
    }

    fun previous(): MediaItem? {
        if (items.isEmpty()) return null
        val prevIndex = when {
            repeatMode == RepeatMode.ONE -> currentIndex
            currentIndex > 0 -> currentIndex - 1
            repeatMode == RepeatMode.ALL -> items.lastIndex
            else -> return null
        }
        return items.getOrNull(prevIndex)
    }
}

private fun String.getFileName(): String = substringBeforeLast('/')
