package org.videolan.vlc.gui.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioAbRepeatMarkers
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose-backed replacement for the audio/video A-B repeat marker guideline
 * containers. Playback delegates only provide media positions; Compose owns
 * normalized marker placement and visibility inside the strip.
 */
internal fun VLCComposeView.installAbRepeatMarkerContainerHost() {
    val host = AbRepeatMarkerContainerHost()
    setTag(R.id.ab_repeat_marker_guideline_container, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.abRepeatMarkerContainerHost(): AbRepeatMarkerContainerHost =
    getTag(R.id.ab_repeat_marker_guideline_container) as? AbRepeatMarkerContainerHost ?: error("Missing AB repeat marker container host")

internal class AbRepeatMarkerContainerHost {
    private var startFraction by mutableFloatStateOf(-1f)
    private var stopFraction by mutableFloatStateOf(-1f)

    private var markerIconRes by mutableIntStateOf(R.drawable.ic_abrepeat_marker_audio)

    fun setMarkerIcon(@DrawableRes icon: Int) {
        markerIconRes = icon
    }

    fun setMarkerPositions(start: Long, stop: Long, length: Long) {
        setMarkerFractions(
            startFraction = markerFraction(start, length),
            stopFraction = markerFraction(stop, length)
        )
    }

    fun setMarkerFractions(startFraction: Float, stopFraction: Float) {
        this.startFraction = normalizedFraction(startFraction)
        this.stopFraction = normalizedFraction(stopFraction)
    }

    @Composable
    fun Content() {
        VLCAudioAbRepeatMarkers(
            startFraction = startFraction,
            stopFraction = stopFraction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(markerIconRes),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    private fun markerFraction(position: Long, length: Long): Float {
        return if (position == -1L || length <= 0L) -1f else position / length.toFloat()
    }

    private fun normalizedFraction(value: Float): Float {
        return if (value < 0f || value.isNaN() || value.isInfinite()) -1f else value.coerceIn(0f, 1f)
    }
}
