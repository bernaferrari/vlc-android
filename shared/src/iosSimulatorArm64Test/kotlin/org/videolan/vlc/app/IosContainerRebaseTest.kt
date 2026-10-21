package org.videolan.vlc.app

import kotlin.test.Test
import kotlin.test.assertEquals

class IosContainerRebaseTest {
    private val home = "/var/mobile/Containers/Data/Application/E70085EB-1D0D-429F-BA97-2A73CF62CED1"

    @Test
    fun storedUrisMoveToTheCurrentContainer() {
        val stored = """{"uri":"file:///var/mobile/Containers/Data/Application/2FDB09B8-6CE1-4674-AC05-57E60C924DFF/Documents/a.mp4"}"""
        assertEquals(
            """{"uri":"file:///var/mobile/Containers/Data/Application/E70085EB-1D0D-429F-BA97-2A73CF62CED1/Documents/a.mp4"}""",
            rebaseContainerPaths(stored, home),
        )
    }

    @Test
    fun foreignPathsAndUnknownHomesAreLeftAlone() {
        val stored = """{"uri":"https://example.org/Containers/stream.m3u8"}"""
        assertEquals(stored, rebaseContainerPaths(stored, home))
        val local = """{"uri":"file:///var/mobile/Containers/Data/Application/2FDB09B8-6CE1-4674-AC05-57E60C924DFF/Documents/a.mp4"}"""
        assertEquals(local, rebaseContainerPaths(local, "/Users/preview"))
    }

    @Test
    fun rebasedDuplicatesCollapseToTheFirstRow() {
        val first = StoredMediaItem(id = 1, title = "A", uri = "file:///x/a.mp4", type = "VIDEO")
        val second = first.copy(id = 2)
        val snapshot = IosCatalogSnapshot(media = listOf(first, second))
        assertEquals(listOf(first), snapshot.withoutDuplicateMedia().media)
    }
}
