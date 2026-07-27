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
import org.videolan.vlc.player.VideoScaleMode
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.platform.RendererInfo
import org.videolan.vlc.platform.RendererType
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

    @Test
    fun stopAfterCurrentIsObservableAndToggleableFromSharedUiState() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)
        vm.play(FakeCatalog.items.first())

        vm.toggleStopAfterCurrent()
        runCurrent()
        assertTrue(vm.state.value.stopAfterCurrent)

        vm.toggleStopAfterCurrent()
        runCurrent()
        assertFalse(vm.state.value.stopAfterCurrent)
        vm.onCleared()
    }

    @Test
    fun videoScaleModeIsOwnedByTheSharedPlayerState() = runTest {
        val playback = FakePlaybackService()
        val vm = PlayerViewModel(playback)

        vm.setVideoScaleMode(VideoScaleMode.FILL)
        runCurrent()

        assertEquals(VideoScaleMode.FILL, vm.state.value.videoScaleMode)
        vm.onCleared()
    }

    @Test
    fun pictureInPictureIsCapabilityGatedInSharedPlayerState() = runTest {
        val playback = FakePlaybackService()
        val pip = RecordingPipController()
        val controller = PlaybackController(
            service = playback,
            pip = pip,
            capabilities = VlcPlatformCapabilities(nativePlayback = true, pictureInPicture = true),
        )
        val vm = PlayerViewModel(playback, controller)

        assertTrue(vm.state.value.pictureInPictureAvailable)
        assertTrue(vm.enterPictureInPicture())
        assertTrue(pip.entered)
        vm.onCleared()
    }

    @Test
    fun rendererDiscoveryAndSelectionStayInSharedPlayerState() = runTest {
        val playback = FakePlaybackService()
        val renderers = RecordingRendererBridge()
        val controller = PlaybackController(
            service = playback,
            renderers = renderers,
            capabilities = VlcPlatformCapabilities(nativePlayback = true, rendererSelection = true),
        )
        val vm = PlayerViewModel(playback, controller)

        vm.startRendererDiscovery()
        assertTrue(renderers.discoveryStarted)
        assertEquals("living-room", vm.state.value.renderers.single().id)
        assertTrue(vm.selectRenderer("living-room"))
        assertEquals("living-room", vm.state.value.selectedRendererId)
        vm.stopRendererDiscovery()
        assertTrue(renderers.discoveryStopped)
        vm.onCleared()
    }

    private class RecordingPipController : PipController {
        var entered = false
        override val isSupported: Boolean = true
        override fun enterPip(): Boolean {
            entered = true
            return true
        }
        override fun exitPip() = Unit
        override fun isInPip(): Boolean = entered
    }

    private class RecordingRendererBridge : RendererBridge {
        var discoveryStarted = false
        var discoveryStopped = false
        private var selected: String? = null
        override fun startDiscovery() { discoveryStarted = true }
        override fun stopDiscovery() { discoveryStopped = true }
        override fun listRenderers(): List<RendererInfo> = listOf(
            RendererInfo("living-room", "Living Room", RendererType.CHROMECAST),
        )
        override fun selectRenderer(id: String?): Boolean {
            selected = id
            return true
        }
        override fun currentRendererId(): String? = selected
    }
}
