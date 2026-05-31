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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
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
 * - application/vlc-android/res/layout/dialog_update.xml
 *
 * The app module owns platform actions such as opening the browser, requesting
 * unknown-sources permissions, persisting settings, and running AutoUpdate.
 */
@Composable
fun VLCUpdateDialogContent(
    title: String,
    description: String,
    nightlyVersion: String,
    neverAskAgainText: String,
    neverAskAgain: Boolean,
    openInBrowserText: String,
    installText: String,
    showInstall: Boolean,
    isDownloading: Boolean,
    onNeverAskAgainChange: (Boolean) -> Unit,
    onOpenInBrowser: () -> Unit,
    onInstall: () -> Unit,
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
                            modifier = Modifier.size(24.dp),
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

                if (isDownloading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                Text(
                    text = description,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(8.dp)
                )

                Text(
                    text = nightlyVersion,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(colors.backgroundDefaultDarker, RoundedCornerShape(4.dp))
                        .padding(16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    Checkbox(
                        checked = neverAskAgain,
                        onCheckedChange = onNeverAskAgainChange
                    )
                    Text(
                        text = neverAskAgainText,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                UpdateDialogActions(
                    openInBrowserText = openInBrowserText,
                    installText = installText,
                    showInstall = showInstall,
                    onOpenInBrowser = onOpenInBrowser,
                    onInstall = onInstall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateDialogActions(
    openInBrowserText: String,
    installText: String,
    showInstall: Boolean,
    onOpenInBrowser: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onOpenInBrowser) {
            Text(openInBrowserText)
        }
        if (showInstall) {
            Button(onClick = onInstall) {
                Text(installText)
            }
        }
    }
}

@Preview(name = "Update Dialog", showBackground = true)
@Composable
private fun VLCUpdateDialogContentPreview() {
    VLCUpdateDialogContent(
        title = "Update available",
        description = "A new nightly build is available.\n\nInstall text goes here.",
        nightlyVersion = "Nightly build from 5/31/26 - arm64",
        neverAskAgainText = "Never ask again",
        neverAskAgain = false,
        openInBrowserText = "Open in browser",
        installText = "Install",
        showInstall = true,
        isDownloading = false,
        onNeverAskAgainChange = {},
        onOpenInBrowser = {},
        onInstall = {},
        iconContent = { PreviewUpdateIcon() }
    )
}

@Preview(name = "Update Dialog Downloading Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCUpdateDialogContentDownloadingPreview() {
    VLCUpdateDialogContent(
        title = "Install nightly",
        description = "Install text goes here.",
        nightlyVersion = "Nightly build from 5/31/26 - x86_64",
        neverAskAgainText = "Never ask again",
        neverAskAgain = true,
        openInBrowserText = "Open in browser",
        installText = "Install",
        showInstall = true,
        isDownloading = true,
        onNeverAskAgainChange = {},
        onOpenInBrowser = {},
        onInstall = {},
        iconContent = { PreviewUpdateIcon() }
    )
}

@Composable
private fun PreviewUpdateIcon() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color.Gray)
    )
}
