package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.compose.components.VLCAbRepeatAddMarkerButton
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * XML-friendly Compose replacement for ab_repeat_controls.xml's add-marker
 * Button. Hosts keep using the shared view ID and regular View click listener.
 */
class AbRepeatAddMarkerButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var addMarkerText by mutableStateOf("")

    fun setMarkerText(text: String) {
        addMarkerText = text
    }

    @Composable
    override fun WidgetContent() {
        VLCAbRepeatAddMarkerButton(
            text = addMarkerText,
            onClick = { performClick() }
        )
    }
}
