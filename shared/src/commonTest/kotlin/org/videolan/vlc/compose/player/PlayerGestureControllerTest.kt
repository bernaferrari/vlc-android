package org.videolan.vlc.compose.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.videolan.vlc.model.Progress
import org.videolan.vlc.player.VideoScaleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerGestureControllerTest {
    private class FakeLevel(var value: Float, override val max: Float = 1f) : PlayerLevelControl {
        override fun read() = value
        override fun write(level: Float) { value = level }
    }

    private class Harness(
        settings: PlayerGestureSettings = PlayerGestureSettings(),
        brightness: PlayerLevelControl? = FakeLevel(0.5f),
        volume: PlayerLevelControl? = FakeLevel(0.5f),
        var progress: Progress = Progress(time = 60_000L, length = 600_000L),
    ) {
        val feedback = PlayerGestureFeedbackState()
        val seeks = mutableListOf<Long>()
        val rates = mutableListOf<Float>()
        var scaleMode = VideoScaleMode.BEST_FIT
        var hudToggles = 0
        var clock = 0L
        val controller = PlayerGestureController(
            feedback = feedback,
            settings = settings,
            brightness = brightness,
            volume = volume,
            progress = { progress },
            playing = { true },
            rate = { 1.25f },
            scaleMode = { scaleMode },
            onSeek = { seeks += it },
            onTogglePlay = {},
            onSetRate = { rates += it },
            onSetScaleMode = { scaleMode = it },
            onToggleHud = { hudToggles++ },
            onHideHud = {},
            now = { clock },
        ).apply { updateViewport(IntSize(1000, 2000)) }
    }

    @Test
    fun rightSideSwipeChangesVolumeAndFadesWhenReleased() {
        val volume = FakeLevel(0.5f)
        val harness = Harness(volume = volume)
        assertTrue(harness.controller.onVerticalDragStart(Offset(900f, 1000f)))
        harness.controller.onVerticalDrag(-400f)
        assertEquals(0.75f, volume.value, 0.0001f)
        assertEquals(PlayerSwipeTarget.VOLUME, harness.feedback.level?.target)
        harness.controller.onVerticalDragEnd()
        assertFalse(harness.feedback.levelActive)
    }

    @Test
    fun disabledGesturesAreAbsent() {
        val harness = Harness(settings = PlayerGestureSettings(volume = false, brightness = false, swipeSeek = false))
        assertFalse(harness.controller.onVerticalDragStart(Offset(900f, 1000f)))
        assertFalse(harness.controller.onHorizontalDragStart())
    }

    @Test
    fun horizontalSwipeSeeksOnceOnRelease() {
        val harness = Harness()
        assertTrue(harness.controller.onHorizontalDragStart())
        harness.controller.onHorizontalDrag(100f)
        harness.controller.onHorizontalDrag(200f)
        assertTrue(harness.seeks.isEmpty())
        harness.controller.onHorizontalDragEnd()
        assertEquals(1, harness.seeks.size)
        assertTrue(harness.seeks.single() > 60_000L)
    }

    @Test
    fun unseekableMediaShowsANoticeInsteadOfSeeking() {
        val harness = Harness(progress = Progress(time = 0L, length = 0L))
        assertFalse(harness.controller.onHorizontalDragStart())
        harness.controller.onDoubleTap(Offset(950f, 500f))
        assertTrue(harness.seeks.isEmpty())
        assertEquals(PlayerNoticeKind.UNSEEKABLE, harness.feedback.notice?.kind)
    }

    @Test
    fun doubleTapsAccumulateAndSingleTapsContinueTheBurst() {
        val harness = Harness()
        harness.controller.onDoubleTap(Offset(950f, 500f))
        assertEquals(70_000L, harness.seeks.last())
        harness.clock = 300L
        assertTrue(harness.controller.continuesSeekBurst(Offset(900f, 500f)))
        assertFalse(harness.controller.continuesSeekBurst(Offset(50f, 500f)))
        harness.controller.onDoubleTap(Offset(900f, 500f))
        assertEquals(80_000L, harness.seeks.last())
        assertEquals(2, harness.feedback.tapSeek?.burst?.taps)
    }

    @Test
    fun fastPlayRestoresThePreviousRate() {
        val harness = Harness()
        harness.controller.onLongPressStart()
        assertEquals(2f, harness.feedback.fastPlay?.rate)
        harness.controller.onLongPressEnd()
        assertEquals(listOf(2f, 1.25f), harness.rates)
        assertNull(harness.feedback.fastPlay)
    }

    @Test
    fun pinchOutZoomsToFillAndAnnouncesIt() {
        val harness = Harness()
        harness.controller.onPinch(1.3f)
        assertTrue(harness.feedback.pinchScale > 1f)
        harness.controller.onPinchEnd(1.3f)
        assertEquals(1f, harness.feedback.pinchScale)
        assertEquals(VideoScaleMode.FIT_SCREEN, harness.scaleMode)
        assertEquals(PlayerNoticeKind.ZOOM_FILL, harness.feedback.notice?.kind)
    }
}
