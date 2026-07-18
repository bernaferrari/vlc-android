package org.videolan.vlc.model

/**
 * Folder node in the shared browser tree (Documents, storage roots, etc.).
 */
data class MediaFolder(
    val id: Long,
    val title: String,
    val path: String,
    val uri: String = path,
    val childCount: Int = 0,
    val isRoot: Boolean = false,
)

/**
 * Named playlist summary for list UIs (items loaded on demand).
 */
data class PlaylistInfo(
    val id: Long,
    val name: String,
    val itemCount: Int = 0,
    val artworkUri: String? = null,
    val duration: Long = 0L,
)

/**
 * History entry with optional last-played timestamp.
 */
data class HistoryEntry(
    val item: MediaItem,
    val playedAt: Long = 0L,
)
