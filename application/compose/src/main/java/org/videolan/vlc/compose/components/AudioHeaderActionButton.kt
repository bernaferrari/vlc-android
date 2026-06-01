package org.videolan.vlc.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose equivalent of the collapsed audio-player header action ImageViews:
 *   - playlist_search
 *   - playlist_switch
 *   - adv_function
 *   - ab_repeat_reset
 *   - ab_repeat_stop
 *
 * The host supplies icon content so app drawable resources can remain in
 * :application:vlc-android while this leaf owns size, click target, and
 * semantics for the audio-player header chrome.
 */
@Composable
fun VLCAudioHeaderActionButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    iconContent: @Composable () -> Unit
) {
    VLCTheme {
        Box(
            modifier = modifier
                .size(40.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }
    }
}

@Preview(
    name = "VLCAudioHeaderActionButton - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 80,
    heightDp = 64
)
@Composable
fun VLCAudioHeaderActionButtonLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioHeaderActionButton(contentDescription = "Search") {
            PreviewHeaderActionIcon()
        }
    }
}

@Preview(
    name = "VLCAudioHeaderActionButton - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 80,
    heightDp = 64
)
@Composable
fun VLCAudioHeaderActionButtonDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioHeaderActionButton(contentDescription = "More options") {
            PreviewHeaderActionIcon()
        }
    }
}

@Composable
private fun PreviewHeaderActionIcon() {
    Box(Modifier.size(24.dp))
}
