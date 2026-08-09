package org.videolan.vlc.compose.player

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AudioIdentityTest {
    @Test
    fun metadataSeedIsStableAndMediaSpecific() {
        val first = MediaItem(1, "First", "file:///first.mp3", MediaType.AUDIO, artist = "Artist")
        val same = first.copy()
        val second = MediaItem(2, "Second", "file:///second.mp3", MediaType.AUDIO, artist = "Artist")

        assertEquals(rememberAudioIdentitySeed(first), rememberAudioIdentitySeed(same))
        assertNotEquals(rememberAudioIdentitySeed(first), rememberAudioIdentitySeed(second))
    }
}
