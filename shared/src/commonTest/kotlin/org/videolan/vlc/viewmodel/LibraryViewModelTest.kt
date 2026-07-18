package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.repository.FakeCatalog
import org.videolan.vlc.repository.FakeMediaRepository
import org.videolan.vlc.repository.FakePlaybackService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsVideoTab() = runTest {
        val media = FakeMediaRepository()
        val playback = FakePlaybackService()
        val vm = LibraryViewModel(media, playback)
        val state = vm.state.first { !it.loading }
        assertEquals(LibraryTab.VIDEO, state.tab)
        assertTrue(state.items.all { it.type == MediaType.VIDEO })
        assertTrue(state.count >= 1)
        vm.onCleared()
    }

    @Test
    fun searchFilters() = runTest {
        val media = FakeMediaRepository()
        val playback = FakePlaybackService()
        val vm = LibraryViewModel(media, playback)
        vm.selectTab(LibraryTab.ALL)
        vm.setQuery("Focus")
        val state = vm.state.first { !it.loading && it.query == "Focus" }
        assertEquals(1, state.items.size)
        assertEquals("Deep Focus", state.items.first().title)
        vm.onCleared()
    }

    @Test
    fun playQueuesPlaylist() = runTest {
        val media = FakeMediaRepository()
        val playback = FakePlaybackService()
        val vm = LibraryViewModel(media, playback)
        vm.selectTab(LibraryTab.ALL)
        val state = vm.state.first { !it.loading && it.tab == LibraryTab.ALL }
        val item = state.items.first()
        vm.play(item)
        val pl = playback.currentPlaylist.first { it.items.isNotEmpty() }
        assertTrue(pl.items.isNotEmpty())
        assertEquals(item.uri, pl.current?.uri)
        vm.onCleared()
    }

    @Test
    fun catalogHasBothTypes() {
        assertTrue(FakeCatalog.items.any { it.isAudio })
        assertTrue(FakeCatalog.items.any { it.isVideo })
    }
}
