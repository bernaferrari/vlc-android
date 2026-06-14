package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_rename.xml
 *
 * The app module owns media type detection and FragmentResult delivery. This
 * content keeps the rename title, current media name, editable new name field,
 * initial focus/selection, and OK action from the former bottom sheet layout.
 */
@Composable
fun VLCRenameDialogContent(
    title: String,
    mediaTitle: String,
    newTitleHint: String,
    okText: String,
    newName: TextFieldValue,
    onNewNameChange: (TextFieldValue) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val confirm = {
            if (newName.text.isNotEmpty()) {
                keyboardController?.hide()
                onConfirm()
            }
        }

        LaunchedEffect(autoFocus) {
            if (autoFocus) {
                delay(100)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefault)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = mediaTitle,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = onNewNameChange,
                    placeholder = { Text(newTitleHint) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.fontDefault),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { confirm() },
                        onDone = { confirm() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .focusRequester(focusRequester)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = confirm, enabled = newName.text.isNotEmpty()) {
                        Text(okText)
                    }
                }
            }
        }
    }
}
