package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the A-B repeat add-marker button from:
 *   - application/vlc-android/res/layout/ab_repeat_controls.xml
 *
 * The playback service still owns the state machine that decides whether the
 * label asks for the first or second marker. Hosts pass the computed label
 * through the shared Compose-backed include root.
 */
@Composable
fun VLCAbRepeatAddMarkerButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    VLCTheme {
        TextButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.textButtonColors(
                contentColor = VLCThemeDefaults.colors.primary
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(
    name = "VLCAbRepeatAddMarkerButton - First Marker",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 180,
    heightDp = 56
)
@Composable
fun VLCAbRepeatAddMarkerButtonFirstPreview() {
    VLCTheme(darkTheme = false) {
        VLCAbRepeatAddMarkerButton(text = "Set start point", onClick = {})
    }
}

@Preview(
    name = "VLCAbRepeatAddMarkerButton - Second Marker Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 180,
    heightDp = 56
)
@Composable
fun VLCAbRepeatAddMarkerButtonSecondDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAbRepeatAddMarkerButton(text = "Set end point", onClick = {})
    }
}
