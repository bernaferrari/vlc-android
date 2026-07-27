package org.videolan.vlc.repository

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreferenceStreamRepositoryTest {
    @Test
    fun storedStreamCodecPreservesDelimiterCharactersAndRejectsDamage() {
        val original = listOf(
            MediaItem(
                id = 7,
                title = "Radio | São Paulo",
                uri = "https://example.test/live?name=a|b",
                type = MediaType.STREAM,
            )
        )

        assertEquals(original, decodeStoredStreams(encodeStoredStreams(original)))
        assertTrue(decodeStoredStreams(setOf("broken", "7|999|short")).isEmpty())
    }
}
