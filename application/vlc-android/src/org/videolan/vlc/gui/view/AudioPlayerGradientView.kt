package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import org.videolan.vlc.compose.components.VLCAudioPlayerGradient
import org.videolan.vlc.compose.components.VLCAudioPlayerGradientEdge
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * XML-friendly Compose replacements for the former audio player XML shell's decorative gradient
 * overlay Views. The layouts keep the existing IDs and constraints.
 */
class AudioPlayerTopGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    @Composable
    override fun WidgetContent() {
        VLCAudioPlayerGradient(edge = VLCAudioPlayerGradientEdge.Top)
    }
}

class AudioPlayerBottomGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    @Composable
    override fun WidgetContent() {
        VLCAudioPlayerGradient(edge = VLCAudioPlayerGradientEdge.Bottom)
    }
}
