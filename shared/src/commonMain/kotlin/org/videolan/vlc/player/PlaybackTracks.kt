package org.videolan.vlc.player

/** A decoder-provided elementary stream that the user can select. */
data class PlaybackTrack(
    val id: String,
    val label: String,
    val selected: Boolean,
)

/**
 * Portable representation of the audio and subtitle tracks in the current media.
 *
 * IDs deliberately remain opaque: LibVLC exposes string ES IDs whereas MobileVLCKit exposes
 * integer indexes. Keeping the conversion at the native edge lets the player UI stay shared.
 */
data class PlaybackTracks(
    val audio: List<PlaybackTrack> = emptyList(),
    val subtitles: List<PlaybackTrack> = emptyList(),
) {
    val hasSelectableTracks: Boolean get() = audio.size > 1 || subtitles.isNotEmpty()
}
