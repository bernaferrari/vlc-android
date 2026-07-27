package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
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

    @Test
    fun onlyVisualMediaRequestsANativeVideoSurface() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)

        vm.play(FakeCatalog.items.first { it.isAudio })
        assertFalse(vm.state.first { it.hasMedia }.hasVideoOutput)

        val video = FakeCatalog.items.first { it.isVideo }
        vm.play(video)
        assertTrue(vm.state.first { it.title == video.title }.hasVideoOutput)

        val networkStream = MediaItem(99, "Live TV", "https://example.test/live", MediaType.STREAM)
        vm.play(networkStream)
        assertTrue(vm.state.first { it.title == networkStream.title }.hasVideoOutput)
        vm.onCleared()
    }

    @Test
    fun playbackRateIsSharedWithTheNativeService() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)
        vm.play(FakeCatalog.items.first())

        vm.setPlaybackRate(1.5f)

        assertEquals(1.5f, playback.getRate())
        assertEquals(1.5f, vm.state.value.rate)

        vm.setPlaybackRate(Float.NaN)
        assertEquals(1f, playback.getRate())
        assertEquals(1f, vm.state.value.rate)
        vm.onCleared()
    }

    @Test
    fun queueCanBeSelectedReorderedAndRemovedFromSharedState() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)
        val queue = FakeCatalog.items.take(3)
        vm.play(queue.first(), queue)
        vm.state.first { it.queue.size == 3 }

        vm.playQueueItem(2)
        assertEquals(queue[2].uri, vm.state.first { it.currentQueueIndex == 2 }.uri)

        vm.moveQueueItem(2, 0)
        val reordered = vm.state.first { it.queue.first().uri == queue[2].uri }
        assertEquals(0, reordered.currentQueueIndex)
        assertEquals(queue[2].uri, reordered.uri)

        vm.removeQueueItem(0)
        val removed = vm.state.first { it.queue.size == 2 && it.uri == queue[0].uri }
        assertEquals(queue[0].uri, removed.uri)
        assertEquals(0, removed.currentQueueIndex)
        vm.onCleared()
    }

    @Test
    fun abRepeatMarkersAreDrivenFromSharedPlayerProgress() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)
        vm.play(FakeCatalog.items.first())

        vm.toggleABRepeat()
        runCurrent()
        assertTrue(vm.state.value.abRepeatEnabled)

        vm.seekTo(1_000)
        runCurrent()
        assertEquals(1_000, vm.state.value.progress.time)
        vm.setABRepeatMarker()
        runCurrent()
        assertEquals(1_000, vm.state.value.abRepeat.start)

        vm.seekTo(3_000)
        runCurrent()
        assertEquals(3_000, vm.state.value.progress.time)
        vm.setABRepeatMarker()
        runCurrent()
        val active = vm.state.value
        assertEquals(3_000, active.abRepeat.stop)

        vm.clearABRepeat()
        runCurrent()
        val cleared = vm.state.value
        assertFalse(cleared.abRepeat.isActive)
        vm.onCleared()
    }
}
