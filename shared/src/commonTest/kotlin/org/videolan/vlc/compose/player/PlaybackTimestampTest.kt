package org.videolan.vlc.compose.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackTimestampTest {
    @Test
    fun parsesVlcSeekTimestampFormats() {
        assertEquals(42_000L, parsePlaybackTimestamp("42"))
        assertEquals(125_000L, parsePlaybackTimestamp("02:05"))
        assertEquals(3_726_000L, parsePlaybackTimestamp("1:02:06"))
        assertEquals(3_726_000L, parsePlaybackTimestamp(" 01:02:06 "))
    }

    @Test
    fun rejectsMalformedOrInvalidClockValues() {
        assertNull(parsePlaybackTimestamp(""))
        assertNull(parsePlaybackTimestamp("1:60"))
        assertNull(parsePlaybackTimestamp("1:60:00"))
        assertNull(parsePlaybackTimestamp("1:02:60"))
        assertNull(parsePlaybackTimestamp("1::2"))
        assertNull(parsePlaybackTimestamp("one minute"))
    }
}
