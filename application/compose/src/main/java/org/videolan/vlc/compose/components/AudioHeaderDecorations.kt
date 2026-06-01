package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalents of the decorative audio-player header background and
 * divider views from:
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_background
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_divider
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/header_background
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/header_divider
 *
 * The host keeps the existing IDs because AudioPlayerAnimator still drives
 * their alpha during bottom-sheet slide transitions.
 */
@Composable
fun VLCAudioHeaderBackground(modifier: Modifier = Modifier.fillMaxSize()) {
    VLCTheme {
        Box(
            modifier = modifier.background(VLCThemeDefaults.colors.audioHeaderBackground)
        )
    }
}

@Composable
fun VLCAudioHeaderDivider(modifier: Modifier = Modifier.fillMaxSize()) {
    VLCTheme {
        Box(
            modifier = modifier.background(VLCThemeDefaults.colors.audioHeaderDivider)
        )
    }
}

@Preview(
    name = "VLCAudioHeaderDecorations - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 72
)
@Composable
fun VLCAudioHeaderDecorationsLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioHeaderDecorationsPreviewContent()
    }
}

@Preview(
    name = "VLCAudioHeaderDecorations - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 72
)
@Composable
fun VLCAudioHeaderDecorationsDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioHeaderDecorationsPreviewContent()
    }
}

@Composable
private fun VLCAudioHeaderDecorationsPreviewContent() {
    Box(Modifier.width(360.dp).height(68.dp)) {
        VLCAudioHeaderBackground()
        VLCAudioHeaderDivider(Modifier.width(360.dp).height(1.dp))
    }
}
