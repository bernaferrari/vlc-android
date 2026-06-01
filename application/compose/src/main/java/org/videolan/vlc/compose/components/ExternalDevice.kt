package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * DialogActivity owns the external storage intent handling and timeout. This
 * content renders the prompt and exposes the Browse / Scan / Cancel actions
 * without legacy dialog XML or data binding.
 */
@Composable
fun VLCExternalDeviceDialogContent(
    title: String,
    message: String,
    browseText: String,
    scanText: String,
    cancelText: String,
    showScan: Boolean,
    onBrowse: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(3.dp),
            color = colors.backgroundDefaultDarker,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefaultDarker)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = message,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                ExternalDeviceActions(
                    browseText = browseText,
                    scanText = scanText,
                    cancelText = cancelText,
                    showScan = showScan,
                    onBrowse = onBrowse,
                    onScan = onScan,
                    onCancel = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExternalDeviceActions(
    browseText: String,
    scanText: String,
    cancelText: String,
    showScan: Boolean,
    onBrowse: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val browseFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        browseFocusRequester.requestFocus()
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextButton(onClick = onCancel) {
            Text(cancelText)
        }
        if (showScan) {
            TextButton(onClick = onScan) {
                Text(scanText)
            }
        }
        TextButton(
            onClick = onBrowse,
            modifier = Modifier.focusRequester(browseFocusRequester)
        ) {
            Text(browseText)
        }
    }
}

@Preview(name = "External Device", showBackground = true)
@Composable
private fun VLCExternalDeviceDialogContentPreview() {
    VLCExternalDeviceDialogContent(
        title = "External device inserted",
        message = "You just plugged a new storage device to your TV box, do you want to open it with VLC?",
        browseText = "Browse",
        scanText = "Scan",
        cancelText = "Cancel",
        showScan = true,
        onBrowse = {},
        onScan = {},
        onCancel = {}
    )
}

@Preview(name = "External Device Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCExternalDeviceDialogContentDarkPreview() {
    VLCExternalDeviceDialogContent(
        title = "External device inserted",
        message = "You just plugged a new storage device to your TV box, do you want to open it with VLC?",
        browseText = "Browse",
        scanText = "Scan",
        cancelText = "Cancel",
        showScan = false,
        onBrowse = {},
        onScan = {},
        onCancel = {}
    )
}
