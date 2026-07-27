package org.videolan.vlc.viewmodels.browser

import android.os.Handler
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.util.MediaBrowser
import org.videolan.tools.CoroutineContextProvider
import org.videolan.vlc.BaseTest
import org.videolan.vlc.providers.BrowserProvider
import org.videolan.vlc.providers.NetworkProvider
import org.videolan.vlc.util.TestCoroutineContextProvider

/** Native network-browser bridge selection. Shared browsing state is tested in commonTest. */
class NetworkModelTest : BaseTest() {
    @Before
    fun configureNativeBrowserFactory() {
        val libVlc = mockk<LibVLC>(relaxed = true)
        val handler = mockk<Handler>(relaxed = true)
        BrowserProvider.overrideCreator = false
        BrowserProvider.registerCreator { MediaBrowser(libVlc, null, handler) }
        BrowserProvider.registerCreator(clazz = CoroutineContextProvider::class.java) { TestCoroutineContextProvider() }
    }

    @Test
    fun networkModelSelectsTheNativeNetworkProvider() {
        val model = NetworkModel(application, null, coroutineContextProvider = TestCoroutineContextProvider())

        assertTrue(model.provider is NetworkProvider)
    }
}
