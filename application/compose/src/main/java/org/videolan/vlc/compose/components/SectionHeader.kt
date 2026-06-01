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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of:
 *   - application/vlc-android/res/layout/recycler_section_header.xml
 *   - application/vlc-android/res/layout/recycler_section_header_tv.xml (simplified; TV uses 48dp wrapper + different padding)
 *
 * High-leverage list header used by RecyclerSectionItemDecoration / GridDecoration
 * across audio browsers, playlists, header media lists, etc.
 *
 * Respects VLCTheme tokens:
 *   - headerBackground (?attr/header_background)
 *   - audioBrowserSeparator (?attr/audio_browser_separator) for text color
 *
 * Typical usage in adapters / decorations (later migration):
 *   VLCSectionHeader(text = sectionName)
 *
 * The TV file/media browser routes now use Compose directly; this component remains
 * the shared section-header leaf for hosts that still draw RecyclerView decorations.
 */
@Composable
fun VLCSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    isTv: Boolean = false
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val height = if (isTv) 56.dp else 36.dp
        val startPadding = if (isTv) 24.dp else 20.dp  // tv_overscan approx + phone 20dp
        val verticalPad = if (isTv) 8.dp else 0.dp

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
                color = colors.audioBrowserSeparator,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
