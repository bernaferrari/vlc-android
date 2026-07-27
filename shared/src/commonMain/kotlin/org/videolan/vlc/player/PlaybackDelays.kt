package org.videolan.vlc.player

/** Decoder timing offsets in microseconds, matching LibVLC and MobileVLCKit. */
data class PlaybackDelays(
    val audioUs: Long = 0L,
    val subtitleUs: Long = 0L,
    val supported: Boolean = false,
)
