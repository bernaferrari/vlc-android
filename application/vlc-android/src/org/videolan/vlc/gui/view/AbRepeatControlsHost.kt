package org.videolan.vlc.gui.view

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
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose-backed A-B repeat controls root. Audio and video hosts keep the
 * shared ab_repeat_container ID and drive marker text through this host.
 */
internal fun VLCComposeView.installAbRepeatControlsHost() {
    val host = AbRepeatControlsHost(this)
    setTag(R.id.ab_repeat_container, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.abRepeatControlsHost(): AbRepeatControlsHost =
    getTag(R.id.ab_repeat_container) as? AbRepeatControlsHost ?: error("Missing AB repeat controls host")

internal class AbRepeatControlsHost(private val view: VLCComposeView) {

    private var markerLabel by mutableStateOf("")

    fun setMarkerText(text: String) {
        markerLabel = text
    }

    @Composable
    fun Content() {
        VLCAbRepeatControls(
            markerText = markerLabel,
            onAddMarkerClick = { view.performClick() }
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
