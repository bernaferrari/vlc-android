package org.videolan.vlc.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import org.videolan.vlc.app.WasmKoinBootstrap
import org.videolan.vlc.compose.app.VlcKoinMainShell
import org.videolan.vlc.compose.app.WasmPersistentAudioAnchor
import org.videolan.vlc.compose.app.WasmPlayerSurface
import org.videolan.vlc.compose.app.WasmShellHostCallbacks

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    WasmKoinBootstrap.start()
    val callbacks = WasmShellHostCallbacks()
    ComposeViewport {
        // The HTML bootstrap is useful until Compose paints; keeping it would leave an
        // aria-busy live region behind the canvas for the rest of the session.
        LaunchedEffect(Unit) { removeLoadingScreen() }
        Box(Modifier.fillMaxSize()) {
            VlcKoinMainShell(
                title = "VLC",
                hostCallbacks = callbacks,
                playerSurface = WasmPlayerSurface,
            )
            WasmPersistentAudioAnchor()
        }
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun removeLoadingScreen(): Unit = js("{ globalThis.document?.getElementById('loading')?.remove(); }")
