package org.videolan.vlc.compose.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import org.videolan.vlc.player.VideoScaleMode

/**
 * Transient visual feedback for a player gesture. Public so design review can render each
 * overlay in a stable, visible state; production code only creates them from gestures.
 */
sealed interface PlayerGestureFeedback {
    /** A brightness or volume swipe. [level] and [max] are normalised so 1f is 100%. */
    data class Level(
        val target: PlayerSwipeTarget,
        val level: Float,
        val max: Float = 1f,
        val heldAtUnity: Boolean = false,
    ) : PlayerGestureFeedback

    /** A horizontal scrub preview. Playback only seeks once the finger lifts. */
    data class Seek(val targetMs: Long, val deltaMs: Long, val lengthMs: Long) : PlayerGestureFeedback

    /** Double-tap seeking on one side; [touch] is where the latest tap landed. */
    data class TapSeek(val burst: SeekBurst, val touch: Offset = Offset.Unspecified) : PlayerGestureFeedback

    /** Press-and-hold fast play at [rate]. */
    data class FastPlay(val rate: Float) : PlayerGestureFeedback

    /** A short confirmation such as a zoom change or a saved screenshot. */
    data class Notice(val kind: PlayerNoticeKind, val id: Long = 0L) : PlayerGestureFeedback
}

enum class PlayerNoticeKind {
    PLAY,
    PAUSE,
    ZOOM_FILL,
    ZOOM_FIT,
    SCREENSHOT_SAVED,
    SCREENSHOT_FAILED,
    ROTATION_LOCKED,
    ROTATION_UNLOCKED,
    UNSEEKABLE,
}

internal fun VideoScaleMode.zoomNotice(): PlayerNoticeKind =
    if (this == VideoScaleMode.BEST_FIT) PlayerNoticeKind.ZOOM_FIT else PlayerNoticeKind.ZOOM_FILL

/** One slot per overlay family so, for example, fast play and a volume swipe can coexist. */
@Stable
internal class PlayerGestureFeedbackState(preview: PlayerGestureFeedback? = null) {
    var level by mutableStateOf(preview as? PlayerGestureFeedback.Level)
    var seek by mutableStateOf(preview as? PlayerGestureFeedback.Seek)
    var tapSeek by mutableStateOf(preview as? PlayerGestureFeedback.TapSeek)
    var fastPlay by mutableStateOf(preview as? PlayerGestureFeedback.FastPlay)
    var notice by mutableStateOf(preview as? PlayerGestureFeedback.Notice)

    /** Live stretch applied to the picture while two fingers are down. */
    var pinchScale by mutableStateOf(1f)

    /** True while a finger still drives the level or seek preview, so it is not faded out. */
    var levelActive by mutableStateOf(preview is PlayerGestureFeedback.Level)
    var seekActive by mutableStateOf(preview is PlayerGestureFeedback.Seek)

    /** Pinned feedback is used for design snapshots and never times out. */
    val pinned: Boolean = preview != null

    private var noticeCounter = 0L

    fun showNotice(kind: PlayerNoticeKind) {
        notice = PlayerGestureFeedback.Notice(kind, ++noticeCounter)
    }
}
