package org.videolan.vlc.compose.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A continuous platform-owned level such as window brightness or media volume.
 *
 * Levels are normalised so `1f` is 100% of the device range. A [max] above `1f` means the
 * platform can amplify past the device maximum (VLC's audio boost).
 */
interface PlayerLevelControl {
    val max: Float get() = 1f
    fun read(): Float
    fun write(level: Float)
}

/** Locks the player window to its current orientation, or returns it to the user's preference. */
interface PlayerOrientationControl {
    val isLocked: Boolean
    fun setLocked(locked: Boolean)
}

/** Saves the frame currently shown by the native video output. */
fun interface PlayerScreenshotControl {
    /** Returns true once the frame has been written to the user's media library. */
    suspend fun capture(): Boolean
}

/**
 * The user's player-gesture preferences. Hosts without a preference store keep the defaults,
 * which match VLC's behaviour for a fresh installation.
 */
data class PlayerGestureSettings(
    val volume: Boolean = true,
    val brightness: Boolean = true,
    val swipeSeek: Boolean = true,
    val doubleTapSeek: Boolean = true,
    val doubleTapPlay: Boolean = true,
    val pinchZoom: Boolean = true,
    val fastPlay: Boolean = true,
    val fastPlayRate: Float = 2f,
    val doubleTapSeekSeconds: Int = 10,
    val keySeekSeconds: Int = 10,
)

/** Semantic commands from a non-touch remote (for example Android's remote-access web page). */
enum class PlayerRemoteCommand {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    CENTER,
    BACK,
    NEXT,
    PREVIOUS,
    PLAY_PAUSE,
    PICTURE_IN_PICTURE,
}

/**
 * Platform effects that the shared player can drive.
 *
 * Every member is optional: `null` means the target cannot perform the action, so the shared
 * player removes the matching gesture or menu entry instead of rendering a no-op affordance
 * (see `shared/CAPABILITIES.md`).
 */
interface PlayerHostControls {
    val brightness: PlayerLevelControl? get() = null
    val volume: PlayerLevelControl? get() = null
    val orientation: PlayerOrientationControl? get() = null
    val screenshot: PlayerScreenshotControl? get() = null
    val gestures: PlayerGestureSettings get() = PlayerGestureSettings()
    val remoteCommands: Flow<PlayerRemoteCommand> get() = emptyFlow()
}
