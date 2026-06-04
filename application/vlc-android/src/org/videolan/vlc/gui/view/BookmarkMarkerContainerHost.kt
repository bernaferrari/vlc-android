package org.videolan.vlc.gui.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBookmarkMarkers
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * XML-friendly Compose replacement for the audio/video bookmark marker overlay.
 * The legacy delegate keeps the media-duration math and pushes normalized
 * timeline fractions into this host instead of adding ImageViews at runtime.
 */
internal fun VLCComposeView.installBookmarkMarkerContainerHost() {
    val host = BookmarkMarkerContainerHost()
    setTag(R.id.bookmark_marker_container, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.bookmarkMarkerContainerHost(): BookmarkMarkerContainerHost =
    getTag(R.id.bookmark_marker_container) as? BookmarkMarkerContainerHost ?: error("Missing bookmark marker container host")

internal class BookmarkMarkerContainerHost {
    private var markers by mutableStateOf(emptyList<Float>())

    fun setMarkerFractions(fractions: List<Float>) {
        markers = fractions
    }

    fun clearMarkers() {
        markers = emptyList()
    }

    @Composable
    fun Content() {
        VLCBookmarkMarkers(markerFractions = markers)
    }
}
