@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.videolan.vlc.compose.app

import org.koin.mp.KoinPlatform
import org.videolan.vlc.app.BrowserMediaRepository
import org.videolan.vlc.app.openBrowserMediaPicker
import org.videolan.vlc.app.openBrowserSubtitlePicker
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption

/** Browser-native equivalents for the shell actions that a Wasm host can perform today. */
class WasmShellHostCallbacks : ShellHostCallbacks {
    override fun onContextAction(item: MediaItem, option: ContextOption) = Unit

    override fun supportsMediaImport(): Boolean = true

    override fun onImportMedia() {
        // Resolve only for the user action. This keeps capability discovery and
        // callback tests independent from a fully bootstrapped Koin graph.
        val mediaRepository = KoinPlatform.getKoin().get<BrowserMediaRepository>()
        openBrowserMediaPicker(mediaRepository::importFiles)
    }

    override fun supportsSubtitleImport(): Boolean = true

    override fun onImportSubtitle(onPicked: (String) -> Unit) = openBrowserSubtitlePicker(onPicked)

    override fun supportsContextAction(option: ContextOption): Boolean = when (option) {
        ContextOption.CTX_INFORMATION,
        ContextOption.CTX_SHARE,
        -> true
        else -> false
    }

    override fun onOpenInfo(item: MediaItem) {
        showBrowserDialog(item.infoPresentation().dialogMessage())
    }

    override fun onShare(item: MediaItem) {
        KoinPlatform.getKoin().get<BrowserMediaRepository>().share(item)
    }

    override fun onOpenAbout() {
        showBrowserDialog(SHARED_ABOUT_MESSAGE)
    }

    override fun onOpenDonate() {
        openExternalUrl(VLC_DONATION_URL)
    }
}

private fun showBrowserDialog(message: String): Unit = js("{ globalThis.alert?.(message); }")

private fun openExternalUrl(url: String): Unit = js(
    "{ globalThis.open?.(url, '_blank', 'noopener,noreferrer'); }",
)
