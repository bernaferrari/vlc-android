package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose equivalent of the former debug log row TextView.
 *
 * Extremely simple leaf: a single-line (or wrapping) monospace TextView.
 *
 * Used by DebugLogActivity to display raw debug / logcat-style output lines.
 *
 * Perfect candidate for direct replacement in log viewer lists.
 */
@Composable
fun VLCDebugLogLine(
    text: String,
    modifier: Modifier = Modifier
) {
    VLCTheme {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
