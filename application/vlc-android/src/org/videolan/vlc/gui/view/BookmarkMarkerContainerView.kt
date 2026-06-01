package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.compose.components.VLCBookmarkMarkers
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * XML-friendly Compose replacement for the audio/video bookmark marker overlay.
 * The legacy delegate keeps the media-duration math and pushes normalized
 * timeline fractions into this view instead of adding ImageViews at runtime.
 */
class BookmarkMarkerContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var markers by mutableStateOf(emptyList<Float>())

    fun setMarkerFractions(fractions: List<Float>) {
        markers = fractions
    }

    fun clearMarkers() {
        markers = emptyList()
    }

    @Composable
    override fun WidgetContent() {
        VLCBookmarkMarkers(markerFractions = markers)
    }
}
