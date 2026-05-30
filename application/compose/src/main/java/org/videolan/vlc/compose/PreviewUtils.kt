package org.videolan.vlc.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCDropdownItem
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Preview utilities for the VLC Compose module.
 *
 * These @Preview functions make the basic theme + components immediately
 * visible and testable inside Android Studio (no device required).
 *
 * Add more previews here as leaf components are added.
 */

@Preview(
    name = "VLC Theme - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 200
)
@Composable
fun VLCThemeLightPreview() {
    VLCTheme(darkTheme = false) {
        PreviewContent()
    }
}

@Preview(
    name = "VLC Theme - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 200
)
@Composable
fun VLCThemeDarkPreview() {
    VLCTheme(darkTheme = true) {
        PreviewContent()
    }
}

@Composable
private fun PreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "VLC Compose Bootstrap")
        Spacer(modifier = Modifier.height(8.dp))
        VLCDropdownItem(text = "Example dropdown item")
        Spacer(modifier = Modifier.height(4.dp))
        VLCDropdownItem(text = "Another item")
    }
}
