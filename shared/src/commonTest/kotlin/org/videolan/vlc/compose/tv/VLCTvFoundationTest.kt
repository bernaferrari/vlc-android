package org.videolan.vlc.compose.tv

import org.videolan.vlc.compose.theme.VLCThemeAppearance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VLCTvFoundationTest {
    @Test
    fun tvDefaultsToDarkUnlessLightIsExplicit() {
        assertTrue(resolveVLCTvDarkTheme(VLCThemeAppearance.System))
        assertTrue(resolveVLCTvDarkTheme(VLCThemeAppearance.Dark))
        assertFalse(resolveVLCTvDarkTheme(VLCThemeAppearance.Light))
    }

    @Test
    fun gridDensityFollowsCardShape() {
        assertEquals(4, VLCTvCardShape.Landscape.gridColumns())
        assertEquals(5, VLCTvCardShape.Square.gridColumns())
        assertEquals(6, VLCTvCardShape.Poster.gridColumns())
    }

    @Test
    fun cardsLeaveRoomForAPeekingNextCardOnA960dpCanvas() {
        val safeWidth = 960f - 2 * VLCTvLayout.OverscanHorizontal.value
        VLCTvCardShape.entries.forEach { shape ->
            val pitch = shape.width.value + VLCTvLayout.CardGap.value
            val visible = safeWidth / pitch
            assertTrue(visible % 1f > 0.15f, "$shape shelf should show part of the next card, got $visible")
        }
    }
}
