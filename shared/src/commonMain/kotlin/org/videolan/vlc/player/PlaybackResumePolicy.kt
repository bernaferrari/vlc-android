package org.videolan.vlc.player

import org.videolan.vlc.model.MediaItem

/**
 * Shared interpretation of VLC's independent audio and video resume settings.
 *
 * Streams use the video branch because they can expose visual output only after
 * probing; restoring them under the audio policy would make a live video queue
 * unexpectedly reappear after the user explicitly disabled video resume.
 */
fun shouldPersistPlaybackSession(
    item: MediaItem,
    audioResumeEnabled: Boolean,
    videoResumeEnabled: Boolean,
): Boolean = if (item.isAudio) audioResumeEnabled else videoResumeEnabled
