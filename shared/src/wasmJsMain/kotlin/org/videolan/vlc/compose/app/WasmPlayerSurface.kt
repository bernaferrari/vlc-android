@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import androidx.compose.ui.unit.dp
import org.videolan.vlc.app.BrowserMediaElementHost
import org.videolan.vlc.app.isBrowserPlayableUri
import org.videolan.vlc.compose.player.PlayerArtworkFallback
import org.videolan.vlc.compose.player.PlayerSurface
import org.w3c.dom.HTMLElement

/**
 * Native browser decoder island for the common Compose player route.
 *
 * The HTML element is deliberately control-less and pointer-transparent: shared
 * [org.videolan.vlc.compose.player.VideoSurfaceWithHud] remains the only visual
 * and interaction layer on Android, iOS, and the web.
 */
val WasmPlayerSurface: PlayerSurface = { state, chromeVisible ->
    Box(Modifier.fillMaxSize()) {
        if (!state.uri.isBrowserPlayableUri() || !state.hasVideoOutput) {
            PlayerArtworkFallback()
        }
        if (state.uri.isBrowserPlayableUri() && state.hasVideoOutput) {
            key(state.hasVideoOutput) {
                WebElementView(
                    factory = { createBrowserMediaElement(state.hasVideoOutput) },
                    modifier = Modifier.fillMaxSize(),
                    update = { element ->
                        configureBrowserMediaElement(element, state.hasVideoOutput, chromeVisible)
                        BrowserMediaElementHost.attachSurface(element)
                    },
                    onRelease = BrowserMediaElementHost::detachSurface,
                )
            }
        }
    }
}

/**
 * A permanent audio decoder anchor for the browser host.
 *
 * The player route can safely disappear while audio continues under the shared mini-player;
 * a visible video surface temporarily replaces this element and hands back on route close.
 */
@Composable
fun WasmPersistentAudioAnchor() {
    WebElementView(
        factory = { createBrowserMediaElement(video = false) },
        modifier = Modifier.size(1.dp),
        update = { element ->
            configureBrowserMediaElement(element, video = false, chromeVisible = false)
            BrowserMediaElementHost.attachFallback(element)
        },
        onRelease = BrowserMediaElementHost::detachFallback,
    )
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun createBrowserMediaElement(video: Boolean): HTMLElement = js(
    "globalThis.document.createElement(video ? 'video' : 'audio')",
)

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun configureBrowserMediaElement(
    element: HTMLElement,
    video: Boolean,
    chromeVisible: Boolean,
): Unit = js(
    """{
        element.controls = false;
        element.preload = 'metadata';
        element.style.width = '100%';
        element.style.height = '100%';
        element.style.objectFit = 'contain';
        element.style.background = 'transparent';
        element.style.opacity = video && chromeVisible ? '0.18' : '1';
        element.style.pointerEvents = 'none';
        if (element.parentElement) element.parentElement.style.pointerEvents = 'none';
        element.playsInline = true;
        element.setAttribute('playsinline', '');
        element.style.display = video ? 'block' : 'none';
    }""",
)
