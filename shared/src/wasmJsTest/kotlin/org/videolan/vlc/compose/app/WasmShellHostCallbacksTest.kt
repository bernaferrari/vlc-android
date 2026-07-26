package org.videolan.vlc.compose.app

import org.videolan.vlc.util.ContextOption
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WasmShellHostCallbacksTest {
    private val callbacks = WasmShellHostCallbacks()

    @Test
    fun advertises_only_browser_supported_media_actions() {
        assertTrue(callbacks.supportsContextAction(ContextOption.CTX_INFORMATION))
        assertTrue(callbacks.supportsContextAction(ContextOption.CTX_SHARE))
        assertFalse(callbacks.supportsContextAction(ContextOption.CTX_ADD_TO_PLAYLIST))
        assertFalse(callbacks.supportsContextAction(ContextOption.CTX_DOWNLOAD_SUBTITLES))
        assertFalse(callbacks.supportsContextAction(ContextOption.CTX_SET_RINGTONE))
    }
}
