package org.videolan.vlc.compose.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose equivalent of audio_player.xml's blurred cover background ImageView:
 *   - application/vlc-android/res/layout/audio_player.xml @id/backgroundView
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/backgroundView
 *
 * The host still owns cover loading and blur generation because those depend on
 * app-side artwork utilities. This leaf owns drawing the already-blurred bitmap
 * and the theme tint overlay.
 */
@Composable
fun VLCAudioPlayerBackground(
    bitmap: Bitmap?,
    overlayColor: Color,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    VLCTheme {
        if (bitmap != null) {
            Box(modifier = modifier) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
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

@Preview(
    name = "VLCAudioPlayerBackground - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun VLCAudioPlayerBackgroundLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioPlayerBackground(
            bitmap = previewBackgroundBitmap(),
            overlayColor = Color(0xBFFFFFFF)
        )
    }
}

@Preview(
    name = "VLCAudioPlayerBackground - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun VLCAudioPlayerBackgroundDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioPlayerBackground(
            bitmap = previewBackgroundBitmap(),
            overlayColor = Color(0x80000000)
        )
    }
}

@Composable
private fun previewBackgroundBitmap(): Bitmap {
    return remember {
        Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            val paint = Paint()
            paint.color = 0xFFFF8800.toInt()
            canvas.drawRect(0f, 0f, 48f, 64f, paint)
            paint.color = 0xFF212121.toInt()
            canvas.drawRect(48f, 0f, 96f, 64f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(48f, 32f, 18f, paint)
        }
    }
}
