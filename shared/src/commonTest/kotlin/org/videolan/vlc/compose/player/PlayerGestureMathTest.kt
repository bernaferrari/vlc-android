package org.videolan.vlc.compose.player

import androidx.compose.ui.input.key.Key
import org.videolan.vlc.player.VideoScaleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerGestureMathTest {

    @Test
    fun verticalSwipeFollowsVlcSidesWithACentreDeadZone() {
        assertEquals(PlayerSwipeTarget.BRIGHTNESS, verticalSwipeTarget(100f, 700f, true, true))
        assertEquals(PlayerSwipeTarget.VOLUME, verticalSwipeTarget(600f, 700f, true, true))
        assertNull(verticalSwipeTarget(350f, 700f, true, true))
    }

    @Test
    fun aSingleAvailableLevelTakesBothSides() {
        assertEquals(PlayerSwipeTarget.VOLUME, verticalSwipeTarget(100f, 700f, false, true))
        assertEquals(PlayerSwipeTarget.BRIGHTNESS, verticalSwipeTarget(600f, 700f, true, false))
        assertNull(verticalSwipeTarget(100f, 700f, false, false))
    }

    @Test
    fun verticalDragUsesVlcSensitivityAndClamps() {
        // Dragging up 40% of the height raises the level by 50%.
        assertEquals(0.7f, levelAfterVerticalDrag(0.2f, -400f, 1000f, 1f), 0.0001f)
        assertEquals(0f, levelAfterVerticalDrag(0.2f, 1000f, 1000f, 1f))
        assertEquals(1f, levelAfterVerticalDrag(0.9f, -1000f, 1000f, 1f))
    }

    @Test
    fun boostNeedsASecondSwipe() {
        // Starting below 100% stops at 100% even when boost is available...
        val first = levelAfterVerticalDrag(0.8f, -1000f, 1000f, 2f)
        assertEquals(1f, first)
        assertTrue(isHoldingAtUnity(0.8f, first, 2f))
        // ...and a new swipe from 100% may amplify.
        assertEquals(1.5f, levelAfterVerticalDrag(1f, -400f, 1000f, 2f), 0.0001f)
        assertFalse(isHoldingAtUnity(1f, 1.5f, 2f))
    }

    @Test
    fun swipeSeekKeepsTheBicubicProgressionAndClamps() {
        // Under one centimetre (~63dp) nothing happens.
        assertEquals(0L, swipeSeekOffsetMs(40f, 60_000L, 600_000L))
        // One centimetre is roughly three seconds.
        val small = swipeSeekOffsetMs(63f, 60_000L, 600_000L)
        assertTrue(small in 3_000L..3_300L, "was $small")
        // Eight centimetres is ten minutes plus the base, clamped to the media end.
        assertEquals(540_000L, swipeSeekOffsetMs(504f, 60_000L, 600_000L))
        assertEquals(-60_000L, swipeSeekOffsetMs(-504f, 60_000L, 600_000L))
        assertEquals(0L, swipeSeekOffsetMs(200f, 0L, 0L))
    }

    @Test
    fun doubleTapZonesSplitTheWidthIntoSidesAndCentre() {
        assertEquals(DoubleTapZone.REWIND, doubleTapZone(50f, 400f))
        assertEquals(DoubleTapZone.PLAY_PAUSE, doubleTapZone(200f, 400f))
        assertEquals(DoubleTapZone.FORWARD, doubleTapZone(350f, 400f))
    }

    @Test
    fun quickTapsOnOneSideAccumulateIntoOneBurst() {
        val first = accumulateSeekBurst(null, forward = true, stepMs = 10_000L, currentTimeMs = 30_000L, nowMs = 0L)
        val second = accumulateSeekBurst(first, forward = true, stepMs = 10_000L, currentTimeMs = 40_000L, nowMs = 400L)
        assertEquals(2, second.taps)
        assertEquals(20_000L, second.totalMs)
        // The burst keeps its original base so a lagging clock never double-counts.
        assertEquals(50_000L, second.targetMs(lengthMs = 100_000L))

        val late = accumulateSeekBurst(second, forward = true, stepMs = 10_000L, currentTimeMs = 50_000L, nowMs = 2_000L)
        assertEquals(1, late.taps)
        val otherSide = accumulateSeekBurst(second, forward = false, stepMs = 10_000L, currentTimeMs = 50_000L, nowMs = 500L)
        assertEquals(1, otherSide.taps)
        assertEquals(40_000L, otherSide.targetMs(lengthMs = 100_000L))
    }

    @Test
    fun burstTargetsStayInsideTheMedia() {
        val back = SeekBurst(forward = false, baseTimeMs = 4_000L, totalMs = 10_000L, taps = 1, lastTapAtMs = 0L)
        assertEquals(0L, back.targetMs(60_000L))
        val forward = SeekBurst(forward = true, baseTimeMs = 55_000L, totalMs = 10_000L, taps = 1, lastTapAtMs = 0L)
        assertEquals(60_000L, forward.targetMs(60_000L))
    }

    @Test
    fun pinchTogglesBetweenFitAndFillOnlyPastAThreshold() {
        assertEquals(VideoScaleMode.FIT_SCREEN, pinchScaleMode(1.3f, VideoScaleMode.BEST_FIT))
        assertEquals(VideoScaleMode.BEST_FIT, pinchScaleMode(0.7f, VideoScaleMode.FIT_SCREEN))
        assertNull(pinchScaleMode(1.05f, VideoScaleMode.BEST_FIT))
        assertNull(pinchScaleMode(1.3f, VideoScaleMode.FIT_SCREEN))
        assertEquals(1.12f, pinchPreviewScale(3f))
    }

    @Test
    fun keysOnlyClaimArrowsForVideo() {
        assertEquals(PlayerKeyAction.TOGGLE_PLAY, playerKeyAction(Key.Spacebar, hasVideoOutput = false, volumeAvailable = false))
        assertEquals(PlayerKeyAction.SEEK_FORWARD, playerKeyAction(Key.DirectionRight, hasVideoOutput = true, volumeAvailable = false))
        assertNull(playerKeyAction(Key.DirectionRight, hasVideoOutput = false, volumeAvailable = true))
        assertEquals(PlayerKeyAction.VOLUME_UP, playerKeyAction(Key.DirectionUp, hasVideoOutput = true, volumeAvailable = true))
        assertEquals(PlayerKeyAction.SHOW_HUD, playerKeyAction(Key.DirectionUp, hasVideoOutput = true, volumeAvailable = false))
        // Escape stays with the shell, which already closes the player.
        assertNull(playerKeyAction(Key.Escape, hasVideoOutput = true, volumeAvailable = true))
    }
}
