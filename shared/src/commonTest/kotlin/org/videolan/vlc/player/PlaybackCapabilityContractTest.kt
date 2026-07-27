@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.platform.RendererInfo
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.repository.FakePlaybackService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PlaybackCapabilityContractTest {
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
    fun unsupportedNativeActionsNeverReachTheirBridges() {
        val pip = RecordingPipController()
        val renderers = RecordingRendererBridge()
        val controller = PlaybackController(
            service = FakePlaybackService(),
            pip = pip,
            renderers = renderers,
            capabilities = VlcPlatformCapabilities(nativePlayback = true),
        )

        assertFalse(controller.enterPip())
        controller.exitPip()
        controller.startRendererDiscovery()
        controller.stopRendererDiscovery()
        assertFalse(controller.selectRenderer("living-room"))

        assertEquals(0, pip.enterCalls)
        assertEquals(0, pip.exitCalls)
        assertEquals(0, renderers.startCalls)
        assertEquals(0, renderers.stopCalls)
        assertEquals(0, renderers.selectCalls)
    }

    private class RecordingPipController : PipController {
        var enterCalls = 0
        var exitCalls = 0
        override val isSupported = true
        override fun enterPip(): Boolean {
            enterCalls += 1
            return true
        }
        override fun exitPip() {
            exitCalls += 1
        }
        override fun isInPip(): Boolean = false
    }

    private class RecordingRendererBridge : RendererBridge {
        var startCalls = 0
        var stopCalls = 0
        var selectCalls = 0
        override fun startDiscovery() {
            startCalls += 1
        }
        override fun stopDiscovery() {
            stopCalls += 1
        }
        override fun listRenderers(): List<RendererInfo> = emptyList()
        override fun selectRenderer(id: String?): Boolean {
            selectCalls += 1
            return true
        }
        override fun currentRendererId(): String? = null
    }
}
