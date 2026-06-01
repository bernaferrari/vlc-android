package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAbRepeatControls
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * XML-friendly Compose replacement for ab_repeat_controls.xml. Audio and video
 * hosts keep the shared ab_repeat_container include ID and drive marker text
 * directly through this root view.
 */
class AbRepeatControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var markerLabel by mutableStateOf("")

    fun setMarkerText(text: String) {
        markerLabel = text
    }

    @Composable
    override fun WidgetContent() {
        VLCAbRepeatControls(
            markerText = markerLabel,
            onAddMarkerClick = { performClick() }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_abrepeat_chips),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
