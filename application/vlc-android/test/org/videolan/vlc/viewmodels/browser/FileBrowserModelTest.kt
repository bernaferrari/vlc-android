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
import org.videolan.vlc.providers.FileBrowserProvider
import org.videolan.vlc.util.TestCoroutineContextProvider

/** Android host-provider selection only; browser state and routes live in commonTest. */
class FileBrowserModelTest : BaseTest() {
    @Before
    fun configureNativeBrowserFactory() {
        val libVlc = mockk<LibVLC>(relaxed = true)
        val handler = mockk<Handler>(relaxed = true)
        BrowserProvider.overrideCreator = false
        BrowserProvider.registerCreator { MediaBrowser(libVlc, null, handler) }
        BrowserProvider.registerCreator(clazz = CoroutineContextProvider::class.java) { TestCoroutineContextProvider() }
    }

    @Test
    fun fileModeSelectsTheNativeFileProvider() {
        val model = BrowserModel(application, null, TYPE_FILE, showDummyCategory = false, coroutineContextProvider = TestCoroutineContextProvider())

        assertTrue(model.provider is FileBrowserProvider)
    }
}
