package org.videolan.vlc.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaModelsTest {
    @Test
    fun `machine generated numeric video title becomes a concise label`() {
        val item = MediaItem(
            id = 1,
            title = "1691565334960",
            uri = "file:///movies/1691565334960.mp4",
            type = MediaType.VIDEO,
        )

        assertEquals("Video • 4960", item.displayTitle)
    }

    @Test
    fun `recording prefix is preserved while technical id is shortened`() {
        val item = MediaItem(
            id = 2,
            title = "Call recording 041996932415_25",
            uri = "file:///recordings/call.m4a",
            type = MediaType.AUDIO,
        )

        assertEquals("Call recording • 2415", item.displayTitle)
    }

    @Test
    fun `ordinary media names remain intact apart from filename separators`() {
        val item = MediaItem(
            id = 3,
            title = "A_quiet_song",
            uri = "file:///music/A_quiet_song.flac",
            type = MediaType.AUDIO,
        )

        assertEquals("A quiet song", item.displayTitle)
    }
}
