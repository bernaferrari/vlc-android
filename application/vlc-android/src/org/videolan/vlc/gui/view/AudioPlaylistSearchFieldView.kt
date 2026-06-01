package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioPlaylistSearchField
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * XML-friendly Compose replacement for audio_player.xml's TextInputLayout +
 * EditText playlist search field. AudioPlayer keeps the existing imperative
 * show/hide timeout flow through this small bridge.
 */
class AudioPlaylistSearchFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    var onQueryChanged: ((String) -> Unit)? = null

    private var query by mutableStateOf("")
    private var focusRequest by mutableIntStateOf(0)
    private val hint = context.getString(R.string.search_hint)

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun requestSearchFocus() {
        requestFocus()
        focusRequest += 1
    }

    fun clearSearchText() {
        query = ""
    }

    @Composable
    override fun WidgetContent() {
        VLCAudioPlaylistSearchField(
            query = query,
            hint = hint,
            focusRequest = focusRequest,
            onQueryChange = { value ->
                query = value
                onQueryChanged?.invoke(value)
            }
        )
    }
}
