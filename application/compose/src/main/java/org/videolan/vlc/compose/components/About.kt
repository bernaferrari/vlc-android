package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/src/org/videolan/vlc/gui/AboutFragment.kt
 * - application/vlc-android/res/layout/about.xml
 * - About screen use of LicenseDialog and AboutVersionDialog bottom-sheet fragments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLCAboutScreen(
    title: String,
    appName: String,
    description: String,
    versionInfo: VLCAboutVersionInfo,
    copyright: String,
    licenseTitle: String,
    licenseText: String,
    websiteTitle: String,
    feedbackTitle: String,
    sourcesTitle: String,
    librariesTitle: String,
    authorsTitle: String,
    closeContentDescription: String,
    openLinkContentDescription: String,
    onClose: () -> Unit,
    onOpenWebsite: () -> Unit,
    onSendFeedback: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenLibraries: () -> Unit,
    onOpenAuthors: () -> Unit,
    onOpenLicenseLink: () -> Unit,
    modifier: Modifier = Modifier,
    closeIconContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder() },
    logoContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder(96) },
    websiteIconContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder() },
    feedbackIconContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder() },
    sourcesIconContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder() },
    librariesIconContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder() },
    authorsIconContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder() },
    linkIconContent: @Composable () -> Unit = { DefaultAboutIconPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        var activeSheet by remember { mutableStateOf<AboutSheet?>(null) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(colors.backgroundDefault),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.semantics {
                            contentDescription = closeContentDescription
                        }
                    ) {
                        CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                            closeIconContent()
                        }
                    }

                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        logoContent()
                    }

                    Text(
                        text = appName,
                        color = colors.aboutTextPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .widthIn(max = 600.dp)
                    )

                    Text(
                        text = description,
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .widthIn(max = 600.dp)
                    )

                    AboutVersionCard(
                        version = versionInfo.version,
                        buildDate = versionInfo.buildDate,
                        onClick = { activeSheet = AboutSheet.Version },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .widthIn(max = 600.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    AboutActionRow(
                        title = websiteTitle,
                        iconContent = websiteIconContent,
                        onClick = onOpenWebsite
                    )
                    AboutActionRow(
                        title = feedbackTitle,
                        iconContent = feedbackIconContent,
                        onClick = onSendFeedback
                    )
                    AboutActionRow(
                        title = sourcesTitle,
                        iconContent = sourcesIconContent,
                        onClick = onOpenSources
                    )
                    AboutActionRow(
                        title = librariesTitle,
                        iconContent = librariesIconContent,
                        onClick = onOpenLibraries
                    )
                    AboutActionRow(
                        title = authorsTitle,
                        iconContent = authorsIconContent,
                        onClick = onOpenAuthors
                    )

                    AboutLicenseCard(
                        copyright = copyright,
                        licenseTitle = licenseTitle,
                        onClick = { activeSheet = AboutSheet.License },
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 24.dp)
                            .widthIn(max = 600.dp)
                    )
                }
            }
        }

        activeSheet?.let { sheet ->
            ModalBottomSheet(
                onDismissRequest = { activeSheet = null },
                sheetState = sheetState,
                containerColor = colors.backgroundDefault,
                contentColor = colors.fontDefault
            ) {
                when (sheet) {
                    AboutSheet.Version -> AboutVersionDetails(
                        versionInfo = versionInfo,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AboutSheet.License -> AboutLicenseDetails(
                        licenseTitle = licenseTitle,
                        copyright = copyright,
                        licenseText = licenseText,
                        openLinkContentDescription = openLinkContentDescription,
                        onOpenLicenseLink = onOpenLicenseLink,
                        linkIconContent = linkIconContent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutVersionCard(
    version: String,
    buildDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(4.dp),
        color = colors.backgroundDefaultDarker,
        contentColor = colors.fontDefault
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = version,
                color = colors.aboutTextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = buildDate,
                color = colors.fontLight,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun AboutActionRow(
    title: String,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                iconContent()
            }
        }

        Spacer(Modifier.width(24.dp))

        Text(
            text = title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AboutLicenseCard(
    copyright: String,
    licenseTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(4.dp),
        color = colors.backgroundDefaultDarker,
        contentColor = colors.fontDefault
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = copyright,
                color = colors.fontDefault,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = licenseTitle,
                color = colors.fontLight,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun AboutVersionDetails(
    versionInfo: VLCAboutVersionInfo,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Column(
        modifier = modifier
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
    ) {
        Text(
            text = versionInfo.version,
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
        )
        Text(
            text = versionInfo.buildDate,
            color = colors.fontLight,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = versionInfo.changelog,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(Modifier.height(16.dp))
        DividerLine()

        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            versionInfo.detailRows.forEach { row ->
                DetailRow(row)
            }
        }
    }
}

@Composable
private fun AboutLicenseDetails(
    licenseTitle: String,
    copyright: String,
    licenseText: String,
    openLinkContentDescription: String,
    onOpenLicenseLink: () -> Unit,
    linkIconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Column(
        modifier = modifier
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = licenseTitle,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = copyright,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            IconButton(
                onClick = onOpenLicenseLink,
                modifier = Modifier.semantics {
                    contentDescription = openLinkContentDescription
                }
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                    linkIconContent()
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        DividerLine()

        Text(
            text = licenseText,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun DetailRow(row: VLCAboutDetailRow) {
    val colors = VLCThemeDefaults.colors

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = row.label,
            color = colors.fontLight,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = row.value,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(VLCThemeDefaults.colors.defaultDivider)
    )
}

data class VLCAboutVersionInfo(
    val version: String,
    val buildDate: String,
    val changelog: String,
    val detailRows: List<VLCAboutDetailRow>
)

data class VLCAboutDetailRow(
    val label: String,
    val value: String
)

private enum class AboutSheet {
    Version,
    License
}

@Composable
private fun DefaultAboutIconPlaceholder(size: Int = 24) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}

@Preview(name = "About Light", showBackground = true)
@Composable
private fun VLCAboutScreenPreview() {
    VLCAboutScreen(
        title = "About",
        appName = "VLC for Android",
        description = "VLC for Android is a port of VLC media player.",
        versionInfo = previewAboutVersionInfo(),
        copyright = "Copyleft (C) 1996-2026 by VideoLAN",
        licenseTitle = "GNU General Public License v2.0",
        licenseText = "GNU General Public License text appears here.",
        websiteTitle = "Official website",
        feedbackTitle = "Send feedback",
        sourcesTitle = "Source code",
        librariesTitle = "Libraries",
        authorsTitle = "Authors",
        closeContentDescription = "Close",
        openLinkContentDescription = "Open in web browser",
        onClose = {},
        onOpenWebsite = {},
        onSendFeedback = {},
        onOpenSources = {},
        onOpenLibraries = {},
        onOpenAuthors = {},
        onOpenLicenseLink = {}
    )
}

@Preview(name = "About Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCAboutScreenDarkPreview() {
    VLCAboutScreen(
        title = "About",
        appName = "VLC for Android",
        description = "VLC for Android is a port of VLC media player.",
        versionInfo = previewAboutVersionInfo(),
        copyright = "Copyleft (C) 1996-2026 by VideoLAN",
        licenseTitle = "GNU General Public License v2.0",
        licenseText = "GNU General Public License text appears here.",
        websiteTitle = "Official website",
        feedbackTitle = "Send feedback",
        sourcesTitle = "Source code",
        librariesTitle = "Libraries",
        authorsTitle = "Authors",
        closeContentDescription = "Close",
        openLinkContentDescription = "Open in web browser",
        onClose = {},
        onOpenWebsite = {},
        onSendFeedback = {},
        onOpenSources = {},
        onOpenLibraries = {},
        onOpenAuthors = {},
        onOpenLicenseLink = {}
    )
}

private fun previewAboutVersionInfo() = VLCAboutVersionInfo(
    version = "3.7.0",
    buildDate = "2026-05-31",
    changelog = "\u2022 Compose migration\n\u2022 Bug fixes",
    detailRows = listOf(
        VLCAboutDetailRow("Revision", "abc1234"),
        VLCAboutDetailRow("Compiled by", "VideoLAN"),
        VLCAboutDetailRow("Signed by", "VideoLAN"),
        VLCAboutDetailRow("libvlc", "4.0.0")
    )
)
