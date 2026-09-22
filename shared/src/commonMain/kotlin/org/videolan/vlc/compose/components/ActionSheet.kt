@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.videolan.vlc.compose.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCThemeDefaults

data class VLCActionSheetItem(
    val label: String,
    val icon: MaterialIcon,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** Consistent touch-first replacement for tiny anchored overflow menus. */
@Composable
fun VLCActionSheet(
    visible: Boolean,
    title: String,
    subtitle: String? = null,
    headerIcon: MaterialIcon,
    actions: List<VLCActionSheetItem>,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VLCThemeDefaults.colors.backgroundDefault,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VLCLayout.SheetHorizontalPadding)
                .navigationBarsPadding()
                .padding(bottom = VLCLayout.SheetBottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VLCModalHeader(
                title = title,
                subtitle = subtitle,
                onDismiss = onDismiss,
                icon = { VLCIconChip { tint -> Icon(headerIcon, contentDescription = null, tint = tint) } },
            )
            Column(verticalArrangement = Arrangement.spacedBy(VLCLayout.GroupGap)) {
                actions.forEachIndexed { index, action ->
                    val position = when {
                        actions.size == 1 -> VLCListItemPosition.Single
                        index == 0 -> VLCListItemPosition.First
                        index == actions.lastIndex -> VLCListItemPosition.Last
                        else -> VLCListItemPosition.Middle
                    }
                    val contentColor = if (action.destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = position.segmentShape(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = contentColor,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(action.icon, contentDescription = null, tint = contentColor)
                            Text(action.label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                        }
                    }
                }
            }
        }
    }
}
