package org.videolan.vlc.repository

import kotlinx.coroutines.test.runTest
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals

class StubPlaylistRepositoryTest {

    @Test
    fun removalAndReorderUsePlaylistPositionsForDuplicateMediaIds() = runTest {
        val repository = StubPlaylistRepository()
        val playlist = repository.createPlaylist("Duplicates")
        val first = MediaItem(id = 1, title = "First copy", uri = "file:///same.mp3", type = MediaType.AUDIO)
        val second = MediaItem(id = 1, title = "Second copy", uri = "file:///same.mp3", type = MediaType.AUDIO)
        val third = MediaItem(id = 2, title = "Third", uri = "file:///third.mp3", type = MediaType.AUDIO)
        repository.addToPlaylist(playlist.id, listOf(first, second, third))

        repository.removeFromPlaylistAt(playlist.id, 1)
        assertEquals(listOf("First copy", "Third"), repository.getPlaylist(playlist.id)?.items?.map { it.title })

        repository.moveInPlaylist(playlist.id, fromIndex = 1, toIndex = 0)
        assertEquals(listOf("Third", "First copy"), repository.getPlaylist(playlist.id)?.items?.map { it.title })
    }
}
