package org.videolan.vlc.compose.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchHighlightTest {
    @Test
    fun `finds every case insensitive match`() {
        assertEquals(
            listOf(0..2, 11..13),
            searchMatchRanges(text = "VLC player VLC", query = "vlc"),
        )
    }

    @Test
    fun `blank query does not create a match`() {
        assertEquals(emptyList(), searchMatchRanges(text = "VLC player", query = "   "))
    }
}
