@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.BrowserListing
import org.videolan.vlc.repository.FakeMediaRepository
import org.videolan.vlc.repository.FakePlaybackService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowserViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun nativeNetworkRootsAndListingsUseTheSharedBrowserFlow() = runTest {
        val server = MediaFolder(
            id = 1L,
            title = "Living room server",
            path = "upnp://living-room",
            uri = "upnp://living-room",
            isRoot = true,
            kind = FolderKind.NETWORK,
        )
        val child = MediaFolder(
            id = 2L,
            title = "Movies",
            path = "upnp://living-room/movies",
            uri = "upnp://living-room/movies",
            kind = FolderKind.NETWORK,
        )
        val movie = MediaItem(
            id = 3L,
            title = "Film",
            uri = "upnp://living-room/movies/film.mkv",
            type = MediaType.STREAM,
        )
        val repo = FakeMediaRepository(
            networkRoots = listOf(server),
            browserListings = mapOf(server.uri to BrowserListing(folders = listOf(child), media = listOf(movie))),
        )
        val vm = BrowserViewModel(
            repo = repo,
            player = PlaybackController(service = FakePlaybackService()),
            capabilities = VlcPlatformCapabilities(nativePlayback = true, networkBrowsing = true),
        )

        assertEquals(listOf(server), vm.state.first { !it.loading }.networkRoots)

        vm.openFolder(server)
        val listing = vm.state.first { !it.loading && it.currentFolder == server }
        assertEquals(listOf(child), listing.folders)
        assertEquals(listOf(movie), listing.media)
        assertTrue(listing.networkRoots.isEmpty())
        vm.onCleared()
    }
}
