package org.videolan.vlc.kmp

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.vlc.BaseTest

class AndroidPlaylistRepositoryTest : BaseTest() {
    private val library = mockk<Medialibrary>()
    private val playlist = mockk<Playlist>()
    private val repository = AndroidPlaylistRepository(library)

    @Test
    fun renameUsesPersistentNativeNameOperation() = runBlocking {
        every { library.getPlaylist(12L, false, false) } returns playlist
        every { playlist.setName("Renamed") } returns true

        repository.renamePlaylist(12L, "Renamed")

        verify(exactly = 1) { playlist.setName("Renamed") }
        verify(exactly = 0) { playlist.setTitle(any()) }
    }

    @Test
    fun failedNativeRenameIsReportedToCaller() = runBlocking {
        every { library.getPlaylist(12L, false, false) } returns playlist
        every { playlist.setName("Renamed") } returns false

        try {
            repository.renamePlaylist(12L, "Renamed")
            fail("Expected failed persistence to be reported")
        } catch (error: IllegalStateException) {
            assertEquals("Unable to rename playlist", error.message)
        }
    }
}
