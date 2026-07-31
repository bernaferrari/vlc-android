@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.videolan.vlc.compose.app

import org.koin.mp.KoinPlatform
import org.videolan.vlc.app.BrowserMediaRepository
import org.videolan.vlc.app.openBrowserMediaPicker
import org.videolan.vlc.app.openBrowserSubtitlePicker
import org.videolan.vlc.compose.components.VLCAboutVersionInfo
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption

/** Browser-native equivalents for the shell actions that a Wasm host can perform today. */
class WasmShellHostCallbacks : ShellHostCallbacks {
    override fun onContextAction(item: MediaItem, option: ContextOption) = Unit

    // The shared hero already says "VLC". Give the Web demo a useful platform label instead of
    // repeating the app name in its version pill.
    override fun aboutVersionInfo() = VLCAboutVersionInfo(
        version = "Web demo",
        buildDate = "Runs locally in your browser",
        changelog = "",
        detailRows = emptyList(),
    )

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

    override fun onOpenDonate() {
        launchExternalUrl(VLC_DONATION_URL)
    }

    override fun onOpenExternalUrl(url: String) {
        launchExternalUrl(url)
    }

    override fun onOpenAboutAction(action: AboutAction) {
        launchExternalUrl(
            when (action) {
                AboutAction.WEBSITE -> VLC_WEBSITE_URL
                AboutAction.FEEDBACK -> "mailto:android@videolan.org"
                AboutAction.SOURCES, AboutAction.LIBRARIES -> VLC_SOURCES_URL
                AboutAction.AUTHORS -> VLC_WEBSITE_URL
                AboutAction.LICENSE -> VLC_LICENSE_URL
            },
        )
    }
}

private fun showBrowserDialog(message: String): Unit = js("{ globalThis.alert?.(message); }")

private fun launchExternalUrl(url: String): Unit = js(
    "{ globalThis.open?.(url, '_blank', 'noopener,noreferrer'); }",
)
