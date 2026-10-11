package org.videolan.vlc.compose.player

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TvPlayerKeyPolicyTest {

    @Test
    fun dpadDrivesPlaybackWhileChromeIsHidden() {
        assertEquals(TvPlayerKeyAction.SEEK_BACK, tvPlayerKeyAction(Key.DirectionLeft, chromeVisible = false, chromeHideable = true))
        assertEquals(TvPlayerKeyAction.SEEK_FORWARD, tvPlayerKeyAction(Key.DirectionRight, chromeVisible = false, chromeHideable = true))
        assertEquals(TvPlayerKeyAction.TOGGLE_PLAY_AND_SHOW, tvPlayerKeyAction(Key.DirectionCenter, chromeVisible = false, chromeHideable = true))
        assertEquals(TvPlayerKeyAction.TOGGLE_PLAY_AND_SHOW, tvPlayerKeyAction(Key.Enter, chromeVisible = false, chromeHideable = true))
        assertEquals(TvPlayerKeyAction.SHOW_HUD, tvPlayerKeyAction(Key.DirectionUp, chromeVisible = false, chromeHideable = true))
        assertEquals(TvPlayerKeyAction.SHOW_HUD, tvPlayerKeyAction(Key.DirectionDown, chromeVisible = false, chromeHideable = true))
    }

    @Test
    fun dpadBelongsToFocusWhileChromeIsShown() {
        listOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown, Key.DirectionCenter, Key.Enter).forEach { key ->
            assertNull(tvPlayerKeyAction(key, chromeVisible = true, chromeHideable = true), "$key")
        }
    }

    @Test
    fun backHidesChromeBeforeLeaving() {
        assertEquals(TvPlayerKeyAction.HIDE_HUD, tvPlayerKeyAction(Key.Back, chromeVisible = true, chromeHideable = true))
        assertEquals(TvPlayerKeyAction.CLOSE, tvPlayerKeyAction(Key.Back, chromeVisible = false, chromeHideable = true))
        // Audio keeps its chrome: Back leaves directly.
        assertEquals(TvPlayerKeyAction.CLOSE, tvPlayerKeyAction(Key.Back, chromeVisible = true, chromeHideable = false))
    }

    @Test
    fun mediaAndMenuKeysAlwaysAct() {
        for (visible in listOf(true, false)) {
            assertEquals(TvPlayerKeyAction.TOGGLE_PLAY, tvPlayerKeyAction(Key.MediaPlayPause, visible, true))
            assertEquals(TvPlayerKeyAction.PLAY, tvPlayerKeyAction(Key.MediaPlay, visible, true))
            assertEquals(TvPlayerKeyAction.PAUSE, tvPlayerKeyAction(Key.MediaPause, visible, true))
            assertEquals(TvPlayerKeyAction.NEXT, tvPlayerKeyAction(Key.MediaNext, visible, true))
            assertEquals(TvPlayerKeyAction.PREVIOUS, tvPlayerKeyAction(Key.MediaPrevious, visible, true))
            assertEquals(TvPlayerKeyAction.SEEK_FORWARD, tvPlayerKeyAction(Key.MediaFastForward, visible, true))
            assertEquals(TvPlayerKeyAction.SEEK_BACK, tvPlayerKeyAction(Key.MediaRewind, visible, true))
            assertEquals(TvPlayerKeyAction.CLOSE, tvPlayerKeyAction(Key.MediaStop, visible, true))
            assertEquals(TvPlayerKeyAction.OPEN_TOOLS, tvPlayerKeyAction(Key.Menu, visible, true))
        }
    }

    @Test
    fun repeatedScrubsAccelerate() {
        assertEquals(10_000L, tvScrubStepMs(10_000L, 0))
        assertEquals(10_000L, tvScrubStepMs(10_000L, 3))
        assertEquals(30_000L, tvScrubStepMs(10_000L, 4))
        assertEquals(60_000L, tvScrubStepMs(10_000L, 12))
        // A user step larger than the acceleration is kept.
        assertEquals(90_000L, tvScrubStepMs(90_000L, 20))
        assertEquals(1_000L, tvScrubStepMs(0L, 0))
    }
}
