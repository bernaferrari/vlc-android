package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
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
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose-backed replacement for the audio/video A-B repeat marker guideline
 * containers. Playback delegates only provide media positions; Compose owns
 * normalized marker placement and visibility inside the strip.
 */
class AbRepeatMarkerContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var startFraction by mutableFloatStateOf(-1f)
    private var stopFraction by mutableFloatStateOf(-1f)

    private var markerIconRes by mutableIntStateOf(R.drawable.ic_abrepeat_marker_audio)

    init {
        clipToPadding = false
        layoutDirection = LAYOUT_DIRECTION_LTR
    }

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
    override fun WidgetContent() {
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
