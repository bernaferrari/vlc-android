package org.videolan.vlc.compose.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShellBackNavigationTest {

    @Test
    fun `selection consumes back before root app exit`() {
        assertEquals(
            true,
            shouldInterceptShellBack(
                appLocked = false,
                hasActiveSelection = true,
                canNavigateBack = false,
            ),
        )
    }

    @Test
    fun `app lock keeps back disabled even during selection`() {
        assertEquals(
            false,
            shouldInterceptShellBack(
                appLocked = true,
                hasActiveSelection = true,
                canNavigateBack = true,
            ),
        )
    }

    @Test
    fun `back unwinds nested destinations in visual stack order`() {
        assertEquals(
            ShellBackTarget.OVERLAY,
            shellBackTarget(
                showOverlay = true,
                hasPlaylistDetail = true,
                hasBrowserFolder = true,
                hasAudioEntity = true,
                hasVideoContainer = true,
            ),
        )
        assertEquals(
            ShellBackTarget.PLAYLIST_DETAIL,
            shellBackTarget(
                showOverlay = false,
                hasPlaylistDetail = true,
                hasBrowserFolder = true,
                hasAudioEntity = true,
                hasVideoContainer = true,
            ),
        )
        assertEquals(
            ShellBackTarget.BROWSER_FOLDER,
            shellBackTarget(
                showOverlay = false,
                hasPlaylistDetail = false,
                hasBrowserFolder = true,
                hasAudioEntity = true,
                hasVideoContainer = true,
            ),
        )
    }

    @Test
    fun `back remains available for each single detail state and not root`() {
        assertEquals(
            ShellBackTarget.AUDIO_ENTITY,
            shellBackTarget(
                showOverlay = false,
                hasPlaylistDetail = false,
                hasBrowserFolder = false,
                hasAudioEntity = true,
                hasVideoContainer = false,
            ),
        )
        assertEquals(
            ShellBackTarget.VIDEO_CONTAINER,
            shellBackTarget(
                showOverlay = false,
                hasPlaylistDetail = false,
                hasBrowserFolder = false,
                hasAudioEntity = false,
                hasVideoContainer = true,
            ),
        )
        assertNull(
            shellBackTarget(
                showOverlay = false,
                hasPlaylistDetail = false,
                hasBrowserFolder = false,
                hasAudioEntity = false,
                hasVideoContainer = false,
            ),
        )
    }
}
