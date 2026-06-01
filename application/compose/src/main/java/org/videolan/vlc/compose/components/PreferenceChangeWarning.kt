package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_confirm_audio_playqueue.xml
 *
 * The app module owns result delivery back to the active preferences host. This content
 * keeps the warning icon, title, message, and OK/Cancel action surface.
 */
@Composable
fun VLCPreferenceChangeWarningDialogContent(
    title: String,
    message: String,
    okText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable (() -> Unit)? = null
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
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (iconContent != null) {
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            iconContent()
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = message,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )

                PreferenceChangeWarningActions(
                    okText = okText,
                    cancelText = cancelText,
                    onConfirm = onConfirm,
                    onCancel = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceChangeWarningActions(
    okText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
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
        Button(onClick = onConfirm) {
            Text(okText)
        }
    }
}

@Preview(name = "Preference Change Warning", showBackground = true)
@Composable
private fun VLCPreferenceChangeWarningDialogContentPreview() {
    VLCPreferenceChangeWarningDialogContent(
        title = "Resume playback",
        message = "Disabling this option clears the saved audio playback queue.",
        okText = "OK",
        cancelText = "Cancel",
        onConfirm = {},
        onCancel = {},
        iconContent = { PreviewWarningIcon() }
    )
}

@Preview(name = "Preference Change Warning Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCPreferenceChangeWarningDialogContentDarkPreview() {
    VLCPreferenceChangeWarningDialogContent(
        title = "Playback history",
        message = "Disabling this option clears your playback history.",
        okText = "OK",
        cancelText = "Cancel",
        onConfirm = {},
        onCancel = {},
        iconContent = { PreviewWarningIcon() }
    )
}

@Composable
private fun PreviewWarningIcon() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(Color.Gray)
    )
}
