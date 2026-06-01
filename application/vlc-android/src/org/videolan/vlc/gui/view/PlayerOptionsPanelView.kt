package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCPlayerOptionItem
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.PlayerOption

/**
 * Compose replacement for the shared player_options.xml overlay. The existing
 * ViewStub hosts keep their IDs/constraints, while this root owns the former
 * BrowseFrameLayout + RecyclerView option list content.
 */
class PlayerOptionsPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var panelOptions by mutableStateOf<List<PlayerOption>>(emptyList())
    private var focusRequestToken by mutableStateOf(0)
    private var onDismissClick: () -> Unit = {}
    private var onOptionClick: (PlayerOption) -> Unit = {}

    init {
        isClickable = true
        isFocusable = false
    }

    fun setOptions(options: List<PlayerOption>) {
        panelOptions = options
    }

    fun setOnDismissClickListener(listener: () -> Unit) {
        onDismissClick = listener
    }

    fun setOnOptionClickListener(listener: (PlayerOption) -> Unit) {
        onOptionClick = listener
    }

    fun requestInitialFocus() {
        focusRequestToken += 1
    }

    fun setOptionIcon(
        optionId: Long,
        @DrawableRes icon: Int,
        contentDescription: String? = null
    ) {
        panelOptions = panelOptions.map { option ->
            if (option.id == optionId) option.copy(icon = icon, contentDescription = contentDescription ?: option.contentDescription)
            else option
        }
    }

    @Composable
    override fun WidgetContent() {
        val firstItemFocusRequester = remember { FocusRequester() }
        val dismissInteractionSource = remember { MutableInteractionSource() }

        LaunchedEffect(focusRequestToken, panelOptions) {
            if (focusRequestToken > 0 && panelOptions.isNotEmpty()) {
                firstItemFocusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = dismissInteractionSource,
                    indication = null,
                    onClick = { onDismissClick() }
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                        clip = false
                    )
                    .background(
                        color = VLCThemeDefaults.colors.backgroundDefault,
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                    )
                    .padding(vertical = 16.dp)
            ) {
                itemsIndexed(
                    items = panelOptions,
                    key = { _, option -> option.id }
                ) { index, option ->
                    val focusModifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                    VLCPlayerOptionItem(
                        title = option.title,
                        contentDescription = option.contentDescription,
                        onClick = { onOptionClick(option) },
                        modifier = focusModifier.focusable()
                    ) {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                            tint = VLCThemeDefaults.colors.playerIconColor
                        )
                    }
                }
            }
        }
    }
}
