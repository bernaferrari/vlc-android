package org.videolan.vlc.compose.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntSize
import org.videolan.vlc.model.Progress
import org.videolan.vlc.player.VideoScaleMode
import kotlin.math.abs
import kotlin.time.TimeSource

/**
 * Turns raw player gestures into playback actions and overlay feedback. It holds no Compose state
 * of its own: everything visible lives in [PlayerGestureFeedbackState].
 */
internal class PlayerGestureController(
    private val feedback: PlayerGestureFeedbackState,
    private val settings: PlayerGestureSettings,
    private val brightness: PlayerLevelControl?,
    private val volume: PlayerLevelControl?,
    private val progress: () -> Progress,
    private val playing: () -> Boolean,
    private val rate: () -> Float,
    private val scaleMode: () -> VideoScaleMode,
    private val onSeek: (Long) -> Unit,
    private val onTogglePlay: () -> Unit,
    private val onSetRate: (Float) -> Unit,
    private val onSetScaleMode: (VideoScaleMode) -> Unit,
    private val onToggleHud: () -> Unit,
    private val onHideHud: () -> Unit,
    private val now: () -> Long = monotonicMillis(),
) : PlayerGestureHandler {

    private var viewport = IntSize.Zero

    private var levelControl: PlayerLevelControl? = null
    private var levelTarget = PlayerSwipeTarget.VOLUME
    private var levelStart = 0f

    private var seekBaseMs = 0L
    private var seekLengthMs = 0L
    private var seekOffsetMs = 0L

    private var fastPlaying = false
    private var rateBeforeFastPlay = 1f

    override val doubleTapEnabled: Boolean get() = settings.doubleTapSeek || settings.doubleTapPlay
    override val longPressEnabled: Boolean get() = settings.fastPlay
    override val pinchEnabled: Boolean get() = settings.pinchZoom

    override fun updateViewport(size: IntSize) {
        viewport = size
    }

    override fun onTap(position: Offset) = onToggleHud()

    override fun continuesSeekBurst(position: Offset): Boolean {
        if (!settings.doubleTapSeek) return false
        val burst = feedback.tapSeek?.burst ?: return false
        if (!burst.isActiveAt(now())) return false
        return when (doubleTapZone(position.x, viewport.width.toFloat())) {
            DoubleTapZone.FORWARD -> burst.forward
            DoubleTapZone.REWIND -> !burst.forward
            DoubleTapZone.PLAY_PAUSE -> false
        }
    }

    override fun onDoubleTap(position: Offset) {
        when (doubleTapZone(position.x, viewport.width.toFloat())) {
            DoubleTapZone.REWIND -> if (settings.doubleTapSeek) tapSeek(forward = false, position) else onToggleHud()
            DoubleTapZone.FORWARD -> if (settings.doubleTapSeek) tapSeek(forward = true, position) else onToggleHud()
            DoubleTapZone.PLAY_PAUSE -> if (settings.doubleTapPlay) togglePlayWithFeedback() else onToggleHud()
        }
    }

    private fun tapSeek(forward: Boolean, position: Offset) {
        val stepMs = settings.doubleTapSeekSeconds.coerceAtLeast(1) * 1_000L
        burstSeek(if (forward) stepMs else -stepMs, position)
    }

    /** Seeks by [deltaMs], merging quick repeats into one labelled burst (taps, keys, remotes). */
    fun seekBy(deltaMs: Long, position: Offset = Offset.Unspecified) = burstSeek(deltaMs, position)

    private fun burstSeek(deltaMs: Long, position: Offset) {
        val current = progress()
        if (current.length <= 0L) {
            feedback.showNotice(PlayerNoticeKind.UNSEEKABLE)
            return
        }
        val burst = accumulateSeekBurst(
            previous = feedback.tapSeek?.burst,
            forward = deltaMs > 0L,
            stepMs = abs(deltaMs),
            currentTimeMs = current.time,
            nowMs = now(),
        )
        onSeek(burst.targetMs(current.length))
        feedback.tapSeek = PlayerGestureFeedback.TapSeek(burst, position)
        onHideHud()
    }

    fun togglePlayWithFeedback() {
        feedback.showNotice(if (playing()) PlayerNoticeKind.PAUSE else PlayerNoticeKind.PLAY)
        onTogglePlay()
    }

    override fun onLongPressStart() {
        if (fastPlaying) return
        fastPlaying = true
        rateBeforeFastPlay = rate()
        onSetRate(settings.fastPlayRate)
        feedback.fastPlay = PlayerGestureFeedback.FastPlay(settings.fastPlayRate)
        onHideHud()
    }

    override fun onLongPressEnd() {
        if (!fastPlaying) return
        fastPlaying = false
        onSetRate(rateBeforeFastPlay)
        feedback.fastPlay = null
    }

    override fun onVerticalDragStart(start: Offset): Boolean {
        val target = verticalSwipeTarget(
            x = start.x,
            width = viewport.width.toFloat(),
            brightnessAvailable = brightness != null && settings.brightness,
            volumeAvailable = volume != null && settings.volume,
        ) ?: return false
        val control = (if (target == PlayerSwipeTarget.BRIGHTNESS) brightness else volume) ?: return false
        levelControl = control
        levelTarget = target
        levelStart = control.read().coerceIn(0f, control.max)
        feedback.levelActive = true
        feedback.level = PlayerGestureFeedback.Level(target, levelStart, control.max)
        onHideHud()
        return true
    }

    override fun onVerticalDrag(totalDy: Float) {
        val control = levelControl ?: return
        val height = viewport.height.toFloat()
        val level = levelAfterVerticalDrag(levelStart, totalDy, height, control.max)
        val wanted = if (height > 0f) levelStart - totalDy / height * 1.25f else level
        if (level != feedback.level?.level) control.write(level)
        feedback.level = PlayerGestureFeedback.Level(
            target = levelTarget,
            level = level,
            max = control.max,
            heldAtUnity = wanted > 1.04f && isHoldingAtUnity(levelStart, level, control.max),
        )
    }

    override fun onVerticalDragEnd() {
        levelControl = null
        feedback.levelActive = false
    }

    /** Keyboard and remote volume steps; shows the same pill as a swipe. */
    fun nudgeVolume(step: Float) {
        val control = volume ?: return
        val level = (control.read() + step).coerceIn(0f, control.max)
        control.write(level)
        feedback.levelActive = false
        feedback.level = PlayerGestureFeedback.Level(PlayerSwipeTarget.VOLUME, level, control.max)
    }

    override fun onHorizontalDragStart(): Boolean {
        if (!settings.swipeSeek) return false
        val current = progress()
        if (current.length <= 0L) {
            feedback.showNotice(PlayerNoticeKind.UNSEEKABLE)
            return false
        }
        seekBaseMs = current.time.coerceIn(0L, current.length)
        seekLengthMs = current.length
        seekOffsetMs = 0L
        feedback.seekActive = true
        return true
    }

    override fun onHorizontalDrag(totalDxDp: Float) {
        seekOffsetMs = swipeSeekOffsetMs(totalDxDp, seekBaseMs, seekLengthMs)
        if (seekOffsetMs != 0L || feedback.seek != null) {
            feedback.seek = PlayerGestureFeedback.Seek(seekBaseMs + seekOffsetMs, seekOffsetMs, seekLengthMs)
            onHideHud()
        }
    }

    override fun onHorizontalDragEnd() {
        feedback.seekActive = false
        // One seek on release: native decoders do not all tolerate a seek per drag frame.
        if (seekOffsetMs != 0L) onSeek(seekBaseMs + seekOffsetMs)
        seekOffsetMs = 0L
    }

    override fun onPinch(cumulativeZoom: Float) {
        feedback.pinchScale = pinchPreviewScale(cumulativeZoom)
    }

    override fun onPinchEnd(cumulativeZoom: Float) {
        feedback.pinchScale = 1f
        val mode = pinchScaleMode(cumulativeZoom, scaleMode()) ?: return
        onSetScaleMode(mode)
        feedback.showNotice(mode.zoomNotice())
    }
}

private fun monotonicMillis(): () -> Long {
    val origin = TimeSource.Monotonic.markNow()
    return { origin.elapsedNow().inWholeMilliseconds }
}

/**
 * Keyboard, D-pad and remote handling for the shared player. Keys only reach this while the
 * player root has focus, so focused HUD controls keep their normal navigation.
 */
internal class PlayerKeyActions(
    private val hasVideoOutput: Boolean,
    private val onTogglePlay: () -> Unit,
    private val onSeekBy: (Long) -> Unit,
    /** Null when the target cannot change volume; arrows then reveal the controls instead. */
    private val onVolumeBy: ((Float) -> Unit)?,
    private val onShowHud: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onClose: (() -> Unit)?,
    private val onEnterPictureInPicture: (() -> Unit)?,
    private val seekStepMs: Long,
) {
    fun onKey(event: KeyEvent): Boolean {
        val action = playerKeyAction(event.key, hasVideoOutput, onVolumeBy != null) ?: return false
        // Consume the matching key-up too, so a parent never acts on half of the press.
        if (event.type != KeyEventType.KeyDown) return true
        perform(action)
        return true
    }

    fun onRemote(command: PlayerRemoteCommand) {
        val action = when (command) {
            PlayerRemoteCommand.UP -> if (onVolumeBy != null) PlayerKeyAction.VOLUME_UP else PlayerKeyAction.SHOW_HUD
            PlayerRemoteCommand.DOWN -> if (onVolumeBy != null) PlayerKeyAction.VOLUME_DOWN else PlayerKeyAction.SHOW_HUD
            PlayerRemoteCommand.LEFT -> PlayerKeyAction.SEEK_BACK
            PlayerRemoteCommand.RIGHT -> PlayerKeyAction.SEEK_FORWARD
            PlayerRemoteCommand.CENTER, PlayerRemoteCommand.PLAY_PAUSE -> PlayerKeyAction.TOGGLE_PLAY
            PlayerRemoteCommand.BACK -> PlayerKeyAction.CLOSE
            PlayerRemoteCommand.NEXT -> PlayerKeyAction.NEXT
            PlayerRemoteCommand.PREVIOUS -> PlayerKeyAction.PREVIOUS
            PlayerRemoteCommand.PICTURE_IN_PICTURE -> PlayerKeyAction.PICTURE_IN_PICTURE
        }
        perform(action)
    }

    private fun perform(action: PlayerKeyAction) {
        when (action) {
            PlayerKeyAction.TOGGLE_PLAY -> onTogglePlay()
            PlayerKeyAction.SEEK_BACK -> onSeekBy(-seekStepMs)
            PlayerKeyAction.SEEK_FORWARD -> onSeekBy(seekStepMs)
            PlayerKeyAction.VOLUME_UP -> onVolumeBy?.invoke(KeyVolumeStep)
            PlayerKeyAction.VOLUME_DOWN -> onVolumeBy?.invoke(-KeyVolumeStep)
            PlayerKeyAction.SHOW_HUD -> onShowHud()
            PlayerKeyAction.NEXT -> onNext()
            PlayerKeyAction.PREVIOUS -> onPrevious()
            PlayerKeyAction.CLOSE -> onClose?.invoke()
            PlayerKeyAction.PICTURE_IN_PICTURE -> onEnterPictureInPicture?.invoke()
        }
    }
}

internal enum class PlayerKeyAction {
    TOGGLE_PLAY,
    SEEK_BACK,
    SEEK_FORWARD,
    VOLUME_UP,
    VOLUME_DOWN,
    SHOW_HUD,
    NEXT,
    PREVIOUS,
    CLOSE,
    PICTURE_IN_PICTURE,
}

/**
 * Pure key policy. Escape/Back are left to the shell, which already closes the player. Arrows are
 * only claimed for video: on the audio player they keep moving focus between controls.
 */
internal fun playerKeyAction(key: Key, hasVideoOutput: Boolean, volumeAvailable: Boolean): PlayerKeyAction? =
    when (key) {
        Key.Spacebar, Key.MediaPlayPause -> PlayerKeyAction.TOGGLE_PLAY
        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> if (hasVideoOutput) PlayerKeyAction.TOGGLE_PLAY else null
        Key.DirectionLeft -> if (hasVideoOutput) PlayerKeyAction.SEEK_BACK else null
        Key.DirectionRight -> if (hasVideoOutput) PlayerKeyAction.SEEK_FORWARD else null
        Key.MediaRewind -> PlayerKeyAction.SEEK_BACK
        Key.MediaFastForward -> PlayerKeyAction.SEEK_FORWARD
        Key.DirectionUp -> when {
            !hasVideoOutput -> null
            volumeAvailable -> PlayerKeyAction.VOLUME_UP
            else -> PlayerKeyAction.SHOW_HUD
        }
        Key.DirectionDown -> when {
            !hasVideoOutput -> null
            volumeAvailable -> PlayerKeyAction.VOLUME_DOWN
            else -> PlayerKeyAction.SHOW_HUD
        }
        Key.MediaNext -> PlayerKeyAction.NEXT
        Key.MediaPrevious -> PlayerKeyAction.PREVIOUS
        else -> null
    }
