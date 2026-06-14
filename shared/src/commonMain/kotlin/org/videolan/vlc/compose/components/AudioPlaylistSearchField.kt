package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of audio_player.xml's playlist search TextInputLayout:
 *   - application/vlc-android/res/layout/audio_player.xml @id/playlist_search_text
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/playlist_search_text
 *
 * The host keeps visibility and timeout control while this leaf owns the
 * text-field rendering, IME action, and focus/keyboard request hook.
 */
@Composable
fun VLCAudioPlaylistSearchField(
    query: String,
    hint: String,
    focusRequest: Int,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    onSearchAction: () -> Unit = {}
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(focusRequest) {
            if (focusRequest > 0) {
                delay(100)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = hint,
                    color = colors.fontLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.fontDefault),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearchAction()
                }
            ),
            modifier = modifier.focusRequester(focusRequester)
        )
    }
}
