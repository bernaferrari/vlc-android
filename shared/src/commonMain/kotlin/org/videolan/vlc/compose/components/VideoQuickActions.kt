package org.videolan.vlc.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose replacement for the video HUD quick-action strip. The host owns
 * visibility and action ids so the video player can keep dispatching the same
 * click paths while this leaf owns the chip layout, scrolling, icon tint, and
 * text treatment.
 */
data class VLCVideoQuickAction(
    val id: Int,
    val iconContent: @Composable () -> Unit,
    val text: String? = null,
    val contentDescription: String? = null
)

@Composable
fun VLCVideoQuickActions(
    actions: List<VLCVideoQuickAction>,
    modifier: Modifier = Modifier,
    onActionClick: (Int) -> Unit = { _ -> },
    onActionLongClick: (Int) -> Unit = { _ -> }
) {
    if (actions.isEmpty()) return
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            VideoQuickActionChip(
                action = action,
                onClick = { onActionClick(action.id) },
                onLongClick = { onActionLongClick(action.id) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoQuickActionChip(
    action: VLCVideoQuickAction,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = action.text.orEmpty()
    val hasLabel = label.isNotBlank()
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .widthIn(max = 144.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VideoQuickActionBackground)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics {
                contentDescription = action.contentDescription ?: label
            }
            .padding(
                start = if (hasLabel) 8.dp else 7.dp,
                top = 6.dp,
                end = if (hasLabel) 12.dp else 7.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        action.iconContent()
        if (hasLabel) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val VideoQuickActionBackground = Color(0xC0141414)
