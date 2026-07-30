package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon

/**
 * One action in a compact, connected control group. Library screens use this
 * instead of scattering unrelated icon buttons across a header.
 */
data class VLCConnectedIconAction(
    val icon: MaterialIcon,
    val contentDescription: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * QuietGuard-style connected utility controls: a small outer silhouette,
 * compact inner joins, and a single clear selected state. It deliberately
 * keeps every target at the Material-recommended 48dp touch size.
 */
@Composable
fun VLCConnectedIconActionBar(
    actions: List<VLCConnectedIconAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        actions.forEachIndexed { index, action ->
            val selected = action.selected
            Surface(
                onClick = action.onClick,
                modifier = Modifier.size(48.dp),
                shape = connectedActionShape(index = index, count = actions.size),
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon = action.icon,
                        contentDescription = action.contentDescription,
                    )
                }
            }
        }
    }
}

private fun connectedActionShape(index: Int, count: Int): RoundedCornerShape = when {
    count <= 1 -> RoundedCornerShape(20.dp)
    index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 20.dp)
    index == count - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
    else -> RoundedCornerShape(6.dp)
}
