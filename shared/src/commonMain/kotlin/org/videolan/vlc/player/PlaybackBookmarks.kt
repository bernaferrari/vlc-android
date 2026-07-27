package org.videolan.vlc.player

/** A durable position marker belonging to the current media item. */
data class PlaybackBookmark(
    /** Stable within a media item; platform stores may use a database id or the position. */
    val id: String,
    val timeMs: Long,
    val title: String,
)

/**
 * Shared bookmark capability and data. Native media-library persistence stays
 * behind [PlaybackService], so the player UI does not fork per platform.
 */
data class PlaybackBookmarks(
    val supported: Boolean = false,
    val entries: List<PlaybackBookmark> = emptyList(),
)
