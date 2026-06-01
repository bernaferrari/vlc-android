package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAbRepeatChipIcon
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * XML-friendly Compose replacement for ab_repeat_controls.xml's decorative
 * ImageView. The shared include is used by both audio and video playback HUDs.
 */
class AbRepeatChipIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    @Composable
    override fun WidgetContent() {
        VLCAbRepeatChipIcon {
            Icon(
                painter = painterResource(R.drawable.ic_abrepeat_chips),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
