package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.repository.FakeCatalog
import org.videolan.vlc.repository.FakePlaybackService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

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
    fun playAndTogglePause() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)
        val item = FakeCatalog.items.first()
        vm.play(item)
        val playing = vm.state.first { it.playing }
        assertEquals(item.title, playing.title)
        vm.togglePlayPause()
        val paused = vm.state.first { it.hasMedia && !it.playing }
        assertFalse(paused.playing)
        vm.togglePlayPause()
        assertTrue(vm.state.first { it.playing }.playing)
        vm.onCleared()
    }

    @Test
    fun cycleRepeat() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)
        vm.play(FakeCatalog.items.first())
        assertEquals(RepeatMode.NONE, vm.state.value.repeatMode)
        vm.cycleRepeat()
        assertEquals(RepeatMode.ALL, vm.state.first { it.repeatMode == RepeatMode.ALL }.repeatMode)
        vm.cycleRepeat()
        assertEquals(RepeatMode.ONE, vm.state.first { it.repeatMode == RepeatMode.ONE }.repeatMode)
        vm.cycleRepeat()
        assertEquals(RepeatMode.NONE, vm.state.first { it.repeatMode == RepeatMode.NONE }.repeatMode)
        vm.onCleared()
    }
}
