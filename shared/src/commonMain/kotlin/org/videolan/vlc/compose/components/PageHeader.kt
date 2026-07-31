package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.theme.VLCLayout

/**
 * Common compact page chrome for root and in-pane detail destinations.
 *
 * It owns the title baseline, safe touch target for an optional Back action, and the exact edge
 * inset used by every root surface. Individual screens only decide which actions belong in the
 * trailing slot, so Browser, Playlists, and More no longer drift over time.
 */
@Composable
fun VLCPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: MaterialIcon? = null,
    navigationContentDescription: String? = null,
    onNavigate: (() -> Unit)? = null,
    horizontalPadding: Dp = VLCLayout.ScreenGutter,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VLCLayout.RowHeight)
            .padding(
                start = if (navigationIcon == null) horizontalPadding else 4.dp,
                top = 8.dp,
                end = horizontalPadding,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (navigationIcon != null && onNavigate != null) {
            IconButton(onClick = onNavigate) {
                Icon(navigationIcon, contentDescription = navigationContentDescription)
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            // Keep every destination on the same M3 top-app-bar title scale. The old headline
            // styles made root pages noticeably taller/louder than Settings and detail routes.
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}
