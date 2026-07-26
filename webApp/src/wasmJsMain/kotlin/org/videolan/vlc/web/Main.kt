package org.videolan.vlc.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.videolan.vlc.app.WasmKoinBootstrap
import org.videolan.vlc.compose.app.VlcKoinMainShell
import org.videolan.vlc.compose.app.WasmShellHostCallbacks

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    WasmKoinBootstrap.start()
    val callbacks = WasmShellHostCallbacks()
    ComposeViewport {
        VlcKoinMainShell(title = "VLC Web", hostCallbacks = callbacks)
    }
}
