package org.videolan.vlc.gui.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.videolan.vlc.compose.components.VLCAudioPlayerBackground
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * XML-friendly Compose replacement for the former audio player XML shell's blurred cover
 * ImageView. AudioPlayerAnimator keeps the existing cover-loading contract and
 * pushes already-blurred bitmaps into this view.
 */
class AudioPlayerBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var backgroundBitmap by mutableStateOf<Bitmap?>(null)
    private var overlayColor by mutableStateOf(Color.Transparent)

    fun setBackgroundBitmap(bitmap: Bitmap?, colorFilter: Int) {
        backgroundBitmap = bitmap
        overlayColor = Color(colorFilter)
    }

    fun clearBackgroundBitmap() {
        backgroundBitmap = null
    }

    @Composable
    override fun WidgetContent() {
        VLCAudioPlayerBackground(
            bitmap = backgroundBitmap,
            overlayColor = overlayColor
        )
    }
}
