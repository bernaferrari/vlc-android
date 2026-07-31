package org.videolan.vlc.util

import kotlin.test.Test
import kotlin.test.assertEquals

class VlcTextUtilsTest {

    @Test
    fun separated_string_omits_empty_metadata_without_stray_separators() {
        assertEquals(
            "Artist · Album",
            VlcTextUtils.separatedString(arrayOf(" Artist ", "", null, "Album")),
        )
        assertEquals("", VlcTextUtils.separatedString(arrayOf("", null, "  ")))
    }
}
