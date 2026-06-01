package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of application/vlc-android/res/layout/info_item.xml
 *
 * Used by InfoActivity for media track details (audio/video/text tracks in
 * the media information screen).
 *
 * Structure from XML:
 *   - Horizontal row: leading icon (ImageView) + vertical (bold title + subtitle)
 *   - Colors: title uses ?attr/font_audio_light (18sp bold), subtitle uses ?attr/list_subtitle
 *
 * This leaf is intentionally presentational and low-coupling (no DataBinding).
 * Icon is provided via leadingContent slot for full flexibility (callers supply
 * their own Icon, Image, or custom during migration; no drawable coupling here).
 *
 * @param title Primary label (e.g. "Audio", "Video", "Text" or track type)
 * @param subtitle Detailed info string (bitrate, codec, channels, resolution, etc.)
 * @param leadingContent Optional composable for the leading icon area (e.g. Icon(...))
 */
@Composable
fun VLCInfoItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Row(
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon area - caller provides (or omit for no-icon variant)
            if (leadingContent != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    leadingContent()
                }
            } else {
                Box(modifier = Modifier.size(20.dp).padding(end = 8.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = colors.fontAudioLight,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = subtitle,
                    color = colors.listSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
