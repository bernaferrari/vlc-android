package org.videolan.vlc.compose.app

/**
 * The shared shell's transient destinations, in the order Back must unwind
 * them. Keeping this decision outside the composable makes the priority
 * explicit and testable while the larger Navigation 3 migration is staged.
 */
internal enum class ShellBackTarget {
    OVERLAY,
    PLAYLIST_DETAIL,
    BROWSER_FOLDER,
    AUDIO_ENTITY,
    VIDEO_CONTAINER,
}

internal fun shellBackTarget(
    showOverlay: Boolean,
    hasPlaylistDetail: Boolean,
    hasBrowserFolder: Boolean,
    hasAudioEntity: Boolean,
    hasVideoContainer: Boolean,
): ShellBackTarget? = when {
    showOverlay -> ShellBackTarget.OVERLAY
    hasPlaylistDetail -> ShellBackTarget.PLAYLIST_DETAIL
    hasBrowserFolder -> ShellBackTarget.BROWSER_FOLDER
    hasAudioEntity -> ShellBackTarget.AUDIO_ENTITY
    hasVideoContainer -> ShellBackTarget.VIDEO_CONTAINER
    else -> null
}
