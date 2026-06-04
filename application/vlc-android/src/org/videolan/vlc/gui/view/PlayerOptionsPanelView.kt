package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
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
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.compose.components.VLCPlayerOptionItem
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.PlayerOption

interface PlayerOptionsPanelHost {
    val visible: Boolean
    fun show()
    fun hide()
    fun setOptions(options: List<PlayerOption>)
    fun setOnDismissClickListener(listener: () -> Unit)
    fun setOnOptionClickListener(listener: (PlayerOption) -> Unit)
    fun requestInitialFocus()
    fun setOptionIcon(
        optionId: Long,
        @DrawableRes icon: Int,
        contentDescription: String? = null
    )
}

/**
 * Direct Compose-backed shared player options overlay. The audio/video hosts
 * place this view at the former stub bounds while this root owns the former
 * BrowseFrameLayout option list content.
 */
class PlayerOptionsPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr), PlayerOptionsPanelHost {

    private var panelOptions by mutableStateOf<List<PlayerOption>>(emptyList())
    private var focusRequestToken by mutableStateOf(0)
    private var onDismissClick: () -> Unit = {}
    private var onOptionClick: (PlayerOption) -> Unit = {}

    init {
        isClickable = true
        isFocusable = false
    }

    override val visible: Boolean
        get() = visibility == View.VISIBLE

    override fun show() {
        setVisible()
    }

    override fun hide() {
        setGone()
    }

    override fun setOptions(options: List<PlayerOption>) {
        panelOptions = options
    }

    override fun setOnDismissClickListener(listener: () -> Unit) {
        onDismissClick = listener
    }

    override fun setOnOptionClickListener(listener: (PlayerOption) -> Unit) {
        onOptionClick = listener
    }

    override fun requestInitialFocus() {
        focusRequestToken += 1
    }

    override fun setOptionIcon(
        optionId: Long,
        @DrawableRes icon: Int,
        contentDescription: String?
    ) {
        panelOptions = panelOptions.map { option ->
            if (option.id == optionId) option.copy(icon = icon, contentDescription = contentDescription ?: option.contentDescription)
            else option
        }
    }

    @Composable
    override fun WidgetContent() {
        VLCPlayerOptionsPanelContent(
            options = panelOptions,
            focusRequestToken = focusRequestToken,
            onDismissClick = onDismissClick,
            onOptionClick = onOptionClick
        )
    }
}

@Composable
fun VLCPlayerOptionsPanelContent(
    options: List<PlayerOption>,
    focusRequestToken: Int,
    onDismissClick: () -> Unit,
    onOptionClick: (PlayerOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val dismissInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(focusRequestToken, options) {
        if (focusRequestToken > 0 && options.isNotEmpty()) {
            firstItemFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
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
                items = options,
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
