@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption

/** Browser-native equivalents for the shell actions that a Wasm host can perform today. */
class WasmShellHostCallbacks : ShellHostCallbacks {
    override fun onContextAction(item: MediaItem, option: ContextOption) = Unit

    override fun supportsContextAction(option: ContextOption): Boolean = when (option) {
        ContextOption.CTX_INFORMATION,
        ContextOption.CTX_SHARE,
        -> true
        else -> false
    }

    override fun onOpenInfo(item: MediaItem) {
        showBrowserDialog(
            listOfNotNull(
                item.displayTitle.takeIf(String::isNotBlank),
                item.artist?.takeIf(String::isNotBlank),
                item.album?.takeIf(String::isNotBlank),
                item.uri.takeIf(String::isNotBlank),
            ).joinToString("\n"),
        )
    }

    override fun onShare(item: MediaItem) {
        // A prompt is deliberately universal: Web Share and Clipboard APIs are both optional.
        promptForCopy("Copy this media location to share it:", item.uri.ifBlank { item.displayTitle })
    }

    override fun onOpenAbout() {
        showBrowserDialog("VLC Web uses the shared Compose Multiplatform media-library shell.")
    }

    override fun onOpenDonate() {
        openExternalUrl("https://www.videolan.org/contribute.html")
    }
}

private fun showBrowserDialog(message: String): Unit = js("{ globalThis.alert?.(message); }")

private fun promptForCopy(message: String, value: String): Unit = js(
    "{ globalThis.prompt?.(message, value); }",
)

private fun openExternalUrl(url: String): Unit = js(
    "{ globalThis.open?.(url, '_blank', 'noopener,noreferrer'); }",
)
