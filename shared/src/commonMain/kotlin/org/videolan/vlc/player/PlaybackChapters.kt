package org.videolan.vlc.player

/** A decoder-provided chapter. Positions are milliseconds to match shared seek/progress state. */
data class PlaybackChapter(
    val index: Int,
    val title: String,
    val positionMs: Long = 0L,
    val selected: Boolean = false,
)

data class PlaybackChapters(val entries: List<PlaybackChapter> = emptyList())
