package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of:
 *   - the former phone recycler section header layout
 *   - the former TV recycler section header layout
 *
 * High-leverage list header used across audio browsers, playlists, header media
 * lists, and TV grid/list surfaces.
 *
 * Respects VLCTheme tokens:
 *   - headerBackground (?attr/header_background)
 *   - audioBrowserSeparator (?attr/audio_browser_separator) for text color
 *
 * Typical usage in Compose list content:
 *   VLCSectionHeader(text = sectionName)
 *
 * The migrated file/media browser routes now use Compose directly; this component
 * remains the shared section-header leaf for list hosts.
 */
@Composable
fun VLCSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    isTv: Boolean = false
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val height = if (isTv) 48.dp else 36.dp
        val startPadding = if (isTv) 48.dp else 20.dp
        val verticalPad = if (isTv) 8.dp else 0.dp
        val textColor = if (isTv) Color.White else colors.audioBrowserSeparator

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .background(colors.headerBackground)
                .padding(
                    start = startPadding,
                    end = 28.dp,
                    top = verticalPad,
                    bottom = verticalPad
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                color = textColor,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
