package org.videolan.vlc.compose.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_update.xml
 *
 * The app module owns platform actions such as opening the browser, requesting
 * unknown-sources permissions, persisting settings, and running AutoUpdate.
 *
 * Material 3 Expressive redesign: an accent icon disc leads the title; the proposed
 * nightly build sits in a labeled tonal "version" card; the never-ask-again toggle
 * reads as a tappable row; the install/browse actions anchor the bottom.
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
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (iconContent != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    iconContent()
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isDownloading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                Text(
                    text = description,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )

                VersionCard(
                    version = nightlyVersion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
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

@Composable
private fun VersionCard(
    version: String,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = colors.fontDefault
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = version,
                color = colors.fontDefault,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
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

@Composable
private fun PreviewUpdateIcon() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color.Gray)
    )
}
