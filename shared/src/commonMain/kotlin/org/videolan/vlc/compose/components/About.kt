package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout

/** Shared Compose About screen, including license and version bottom sheets. */
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
    websiteSummary: String = "",
    feedbackTitle: String,
    feedbackSummary: String = "",
    sourcesTitle: String,
    sourcesSummary: String = "",
    librariesTitle: String,
    librariesSummary: String = "",
    authorsTitle: String,
    authorsSummary: String = "",
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
    showHeader: Boolean = true,
    closeIconContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.Close) },
    logoContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.PlayArrow, 28) },
    websiteIconContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.Language) },
    feedbackIconContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.Forum) },
    sourcesIconContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.Code) },
    librariesIconContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.Extension) },
    authorsIconContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.Groups) },
    linkIconContent: @Composable () -> Unit = { DefaultAboutIcon(MaterialSymbols.Filled.OpenInNew) }
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
                if (showHeader) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(VLCLayout.RowHeight)
                            .padding(vertical = 8.dp)
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    AboutHeroCard(
                        appName = appName,
                        version = versionInfo.version,
                        logoContent = logoContent,
                        onVersionClick = { activeSheet = AboutSheet.Version },
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )

                    val destinations = listOf(
                        AboutDestinationAction(
                            websiteTitle,
                            websiteSummary,
                            websiteIconContent,
                            onOpenWebsite,
                        ),
                        AboutDestinationAction(
                            feedbackTitle,
                            feedbackSummary,
                            feedbackIconContent,
                            onSendFeedback,
                        ),
                        AboutDestinationAction(
                            sourcesTitle,
                            sourcesSummary,
                            sourcesIconContent,
                            onOpenSources,
                        ),
                        AboutDestinationAction(
                            librariesTitle,
                            librariesSummary,
                            librariesIconContent,
                            onOpenLibraries,
                        ),
                        AboutDestinationAction(
                            authorsTitle,
                            authorsSummary,
                            authorsIconContent,
                            onOpenAuthors,
                        ),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        destinations.forEachIndexed { index, action ->
                            AboutActionRow(
                                title = action.title,
                                summary = action.summary,
                                iconContent = action.iconContent,
                                onClick = action.onClick,
                                position = aboutItemPosition(index, destinations.size),
                            )
                        }
                    }

                    AboutLicenseCard(
                        copyright = copyright,
                        licenseTitle = licenseTitle,
                        onClick = { activeSheet = AboutSheet.License },
                        modifier = Modifier.padding(bottom = 24.dp),
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
private fun AboutHeroCard(
    appName: String,
    version: String,
    logoContent: @Composable () -> Unit,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) { logoContent() }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onVersionClick),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .14f),
                ) {
                    Text(
                        text = version,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

private data class AboutDestinationAction(
    val title: String,
    val summary: String,
    val iconContent: @Composable () -> Unit,
    val onClick: () -> Unit,
)

private fun aboutItemPosition(index: Int, size: Int): VLCListItemPosition = when {
    size <= 1 -> VLCListItemPosition.Single
    index == 0 -> VLCListItemPosition.First
    index == size - 1 -> VLCListItemPosition.Last
    else -> VLCListItemPosition.Middle
}

@Composable
private fun AboutActionRow(
    title: String,
    summary: String,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    position: VLCListItemPosition,
    modifier: Modifier = Modifier
) {
    VLCNavigationRow(
        title = title,
        summary = summary,
        position = position,
        onClick = onClick,
        modifier = modifier,
    ) {
        iconContent()
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
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = VLCListItemPosition.Single.segmentShape(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = colors.fontDefault
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(MaterialSymbols.Filled.Description, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = licenseTitle,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = copyright,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                MaterialSymbols.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun DefaultAboutIcon(icon: MaterialIcon, size: Int = 24) = Icon(
    icon = icon,
    contentDescription = null,
    modifier = Modifier.size(size.dp),
)
