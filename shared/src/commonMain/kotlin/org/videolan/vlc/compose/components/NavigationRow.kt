package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * The shared destination row for screens that navigate deeper into the app.
 *
 * Utility destinations previously each had their own card geometry and press behavior. Keeping
 * this one row across More, About, and future setting directories makes those screens read as
 * one product: asymmetric grouped corners, a full row target, and native Material press feedback.
 */
@Composable
fun VLCNavigationRow(
    title: String,
    summary: String,
    position: VLCListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (Color) -> Unit,
) {
    val colors = VLCThemeDefaults.colors

    Surface(
        onClick = onClick,
        shape = position.segmentShape(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VLCLayout.MediaRowHeight),
    ) {
        Row(
            // Even 16dp on every side; the end is 12dp because the chevron glyph carries ~4dp of
            // its own whitespace, so it optically lands on the same 16dp line.
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VLCIconChip(
                size = VLCLayout.DestinationIconChip,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                content = leadingContent,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    color = colors.listTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                summary.takeIf(String::isNotBlank)?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                MaterialSymbols.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
