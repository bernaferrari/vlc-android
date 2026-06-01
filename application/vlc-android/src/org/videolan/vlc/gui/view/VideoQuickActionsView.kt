package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCVideoQuickAction
import org.videolan.vlc.compose.components.VLCVideoQuickActions
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * XML-friendly host for the video HUD quick-action strip. It preserves the
 * action ids used by VideoPlayerActivity click dispatch.
 */
class VideoQuickActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var actions by mutableStateOf(defaultActions(context))
    private var onActionClick by mutableStateOf<(Int) -> Unit>({ _ -> })
    private var onActionLongClick by mutableStateOf<(Int) -> Unit>({ _ -> })

    fun setOnActionClickListener(listener: (Int) -> Unit) {
        onActionClick = listener
    }

    fun setOnActionLongClickListener(listener: (Int) -> Unit) {
        onActionLongClick = listener
    }

    fun setActionVisible(@IdRes actionId: Int, visible: Boolean) {
        updateAction(actionId) { it.copy(visible = visible) }
    }

    fun setActionText(@IdRes actionId: Int, text: CharSequence?) {
        updateAction(actionId) { it.copy(text = text?.toString()) }
    }

    fun setActionContentDescription(@IdRes actionId: Int, contentDescription: CharSequence?) {
        updateAction(actionId) { it.copy(contentDescription = contentDescription?.toString()) }
    }

    fun setActionIcon(@IdRes actionId: Int, @DrawableRes icon: Int) {
        updateAction(actionId) { it.copy(icon = icon) }
    }

    private fun updateAction(@IdRes actionId: Int, transform: (VideoQuickActionEntry) -> VideoQuickActionEntry) {
        actions = actions.map { action ->
            if (action.id == actionId) transform(action) else action
        }
    }

    @Composable
    override fun WidgetContent() {
        VLCVideoQuickActions(
            actions = actions
                .filter { it.visible }
                .map { action ->
                    VLCVideoQuickAction(
                        id = action.id,
                        icon = action.icon,
                        text = action.text,
                        contentDescription = action.contentDescription
                    )
                },
            onActionClick = onActionClick,
            onActionLongClick = onActionLongClick
        )
    }

    private data class VideoQuickActionEntry(
        @IdRes val id: Int,
        @DrawableRes val icon: Int,
        val text: String? = null,
        val contentDescription: String? = null,
        val visible: Boolean = false
    )

    private companion object {
        fun defaultActions(context: Context) = listOf(
            VideoQuickActionEntry(
                id = R.id.orientation_quick_action,
                icon = R.drawable.ic_player_lock_portrait,
                contentDescription = context.getString(R.string.lock_orientation_description)
            ),
            VideoQuickActionEntry(
                id = R.id.playback_speed_quick_action,
                icon = R.drawable.ic_speed,
                contentDescription = context.getString(R.string.playback_speed)
            ),
            VideoQuickActionEntry(
                id = R.id.sleep_quick_action,
                icon = R.drawable.ic_sleep,
                contentDescription = context.getString(R.string.sleep_title)
            ),
            VideoQuickActionEntry(
                id = R.id.spu_delay_quick_action,
                icon = R.drawable.ic_subtitles,
                contentDescription = context.getString(R.string.spu_delay)
            ),
            VideoQuickActionEntry(
                id = R.id.audio_delay_quick_action,
                icon = R.drawable.ic_player_volume,
                contentDescription = context.getString(R.string.audio_delay)
            )
        )
    }
}
