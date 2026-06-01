package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
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
import org.videolan.vlc.compose.components.VLCPlayerOptionItem
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.UiTools

/**
 * XML-free playback option row host for PlayerOptionsDelegate. It keeps the
 * legacy View click/focus contract while rendering player_option_item.xml's
 * icon + title row with Compose.
 */
class PlayerOptionItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var title by mutableStateOf("")
    private var iconRes by mutableStateOf(0)

    init {
        isFocusable = true
        minimumWidth = resources.getDimensionPixelSize(R.dimen.player_option_width)
        setBackgroundResource(UiTools.getResourceFromAttribute(context, android.R.attr.selectableItemBackground))
    }

    fun setOption(title: String, @DrawableRes icon: Int) {
        this.title = title
        contentDescription = title
        setIconResource(icon)
    }

    fun setIconResource(@DrawableRes icon: Int) {
        iconRes = icon
    }

    @Composable
    override fun WidgetContent() {
        VLCPlayerOptionItem(title = title) {
            if (iconRes != 0) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = VLCThemeDefaults.colors.playerIconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
