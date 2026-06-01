package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_duplication_warning.xml
 *
 * The app module owns result delivery to SavePlaylistDialog. This content
 * renders the duplicate playlist warning and exposes the same two-option or
 * three-option actions as the former DataBinding layout.
 */
@Composable
fun VLCDuplicationWarningDialogContent(
    title: String,
    message: AnnotatedString,
    cancelText: String,
    addText: String,
    addAllText: String,
    addNewOnlyText: String,
    showThreeOptions: Boolean,
    onCancel: () -> Unit,
    onAddAll: () -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
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
                    .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 16.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                )

                Text(
                    text = message,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 8.dp)
                )

                DuplicationWarningActions(
                    cancelText = cancelText,
                    addText = addText,
                    addAllText = addAllText,
                    addNewOnlyText = addNewOnlyText,
                    showThreeOptions = showThreeOptions,
                    onCancel = onCancel,
                    onAddAll = onAddAll,
                    onAddNew = onAddNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuplicationWarningActions(
    cancelText: String,
    addText: String,
    addAllText: String,
    addNewOnlyText: String,
    showThreeOptions: Boolean,
    onCancel: () -> Unit,
    onAddAll: () -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onCancel) {
            Text(cancelText)
        }
        if (showThreeOptions) {
            TextButton(onClick = onAddNew) {
                Text(addNewOnlyText)
            }
            Button(onClick = onAddAll) {
                Text(addAllText)
            }
        } else {
            Button(onClick = onAddAll) {
                Text(addText)
            }
        }
    }
}

@Preview(name = "Duplication Warning", showBackground = true)
@Composable
private fun VLCDuplicationWarningDialogContentPreview() {
    VLCDuplicationWarningDialogContent(
        title = "Add duplicated item(s)?",
        message = AnnotatedString("This item is already in \"Road trip\" playlist.\nThese items are already in \"Favorites\" playlist.\n"),
        cancelText = "Cancel",
        addText = "Add",
        addAllText = "Add all",
        addNewOnlyText = "Add new only",
        showThreeOptions = true,
        onCancel = {},
        onAddAll = {},
        onAddNew = {}
    )
}

@Preview(name = "Duplication Warning Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCDuplicationWarningDialogContentDarkPreview() {
    VLCDuplicationWarningDialogContent(
        title = "Add duplicated item(s)?",
        message = AnnotatedString("This item is already in \"Road trip\" playlist.\n"),
        cancelText = "Cancel",
        addText = "Add",
        addAllText = "Add all",
        addNewOnlyText = "Add new only",
        showThreeOptions = false,
        onCancel = {},
        onAddAll = {},
        onAddNew = {}
    )
}
