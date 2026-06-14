package org.videolan.vlc.compose.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose equivalent of audio_player.xml's blurred cover background ImageView:
 *   - application/vlc-android/res/layout/audio_player.xml @id/backgroundView
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/backgroundView
 *
 * The host still owns cover loading and blur generation because those depend on
 * app-side artwork utilities. This leaf owns drawing the already-blurred bitmap
 * and the theme tint overlay
 */
@Composable
fun VLCAudioPlayerBackground(
    bitmap: ImageBitmap?,
    overlayColor: Color,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    VLCTheme {
        if (bitmap != null) {
            Box(modifier = modifier) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                )
            }
        }
    }
}
