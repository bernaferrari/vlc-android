package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.compose.components.VLCAudioMiniProgressBar
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * XML-friendly Compose replacement for audio_player.xml's collapsed mini
 * ProgressBar. It deliberately exposes max/progress so existing AudioPlayer
 * code can keep the same small imperative contract during the migration.
 */
class AudioMiniProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    var max by mutableIntStateOf(100)

    var progress by mutableIntStateOf(0)

    @Composable
    override fun WidgetContent() {
        val fraction = if (max > 0) progress.coerceIn(0, max).toFloat() / max else 0f
        VLCAudioMiniProgressBar(progressFraction = fraction)
    }
}
