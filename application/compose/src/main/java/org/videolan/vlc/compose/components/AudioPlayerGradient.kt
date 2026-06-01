package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of audio_player.xml's top/bottom gradient overlay views:
 *   - application/vlc-android/res/layout/audio_player.xml @id/top_gradient
 *   - application/vlc-android/res/layout/audio_player.xml @id/bottom_gradient
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/top_gradient
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/bottom_gradient
 *
 * The original XML drawables fade from white_transparent_80 in light theme or
 * black_transparent_80 in dark theme to transparent.
 */
@Composable
fun VLCAudioPlayerGradient(
    edge: VLCAudioPlayerGradientEdge,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    VLCTheme {
        val gradientColor = VLCThemeDefaults.colors.audioPlayerGradientColor
        val colors = when (edge) {
            VLCAudioPlayerGradientEdge.Top -> listOf(gradientColor, Color.Transparent)
            VLCAudioPlayerGradientEdge.Bottom -> listOf(Color.Transparent, gradientColor)
        }
        Box(modifier = modifier.background(Brush.verticalGradient(colors)))
    }
}

enum class VLCAudioPlayerGradientEdge {
    Top,
    Bottom
}

@Preview(
    name = "VLCAudioPlayerGradient - Light",
    showBackground = true,
    backgroundColor = 0xFFEEEEEE,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun VLCAudioPlayerGradientLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioPlayerGradientPreviewContent()
    }
}

@Preview(
    name = "VLCAudioPlayerGradient - Dark",
    showBackground = true,
    backgroundColor = 0xFF2A2A2A,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun VLCAudioPlayerGradientDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioPlayerGradientPreviewContent()
    }
}

@Composable
private fun VLCAudioPlayerGradientPreviewContent() {
    Box(
        modifier = Modifier
            .width(360.dp)
            .height(180.dp)
            .background(VLCThemeDefaults.colors.backgroundDefaultDarker)
    ) {
        VLCAudioPlayerGradient(
            edge = VLCAudioPlayerGradientEdge.Top,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(72.dp)
        )
        VLCAudioPlayerGradient(
            edge = VLCAudioPlayerGradientEdge.Bottom,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
        )
    }
}
