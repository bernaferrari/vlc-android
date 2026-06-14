package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/license_activity.xml
 * - application/vlc-android/res/layout/library_item.xml
 *
 * Also provides the LibrariesActivity-owned Compose detail sheet that replaced
 * that screen's former LicenseDialog usage.
 *
 * Material 3 Expressive redesign: each dependency is a tonal card with an
 * accented source-icon disc, name + copyright, and a license badge pill, matching
 * the About / Search visual language instead of the former flat divider-less rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLCLibrariesScreen(
    title: String,
    libraries: List<VLCLibraryLicense>,
    closeContentDescription: String,
    openLinkContentDescription: String,
    onClose: () -> Unit,
    onOpenLicenseLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    closeIconContent: @Composable () -> Unit = { DefaultLibrariesIconPlaceholder() },
    sourceIconContent: @Composable () -> Unit = { DefaultLibrariesIconPlaceholder() },
    linkIconContent: @Composable () -> Unit = { DefaultLibrariesIconPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        var selectedLibrary by remember { mutableStateOf<VLCLibraryLicense?>(null) }
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 54.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(libraries) { library ->
                        LibraryLicenseCard(
                            library = library,
                            sourceIconContent = sourceIconContent,
                            onClick = { selectedLibrary = library }
                        )
                    }
                }
            }
        }

        selectedLibrary?.let { library ->
            ModalBottomSheet(
                onDismissRequest = { selectedLibrary = null },
                sheetState = sheetState,
                containerColor = colors.backgroundDefault,
                contentColor = colors.fontDefault
            ) {
                LibraryLicenseDetail(
                    library = library,
                    openLinkContentDescription = openLinkContentDescription,
                    onOpenLicenseLink = onOpenLicenseLink,
                    linkIconContent = linkIconContent,
                    sourceIconContent = sourceIconContent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LibraryLicenseCard(
    library: VLCLibraryLicense,
    sourceIconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .focusable(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = colors.fontDefault
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AccentIconDisc(iconContent = sourceIconContent)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = library.title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (library.copyright.isNotBlank()) {
                    Text(
                        text = library.copyright,
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (library.licenseTitle.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    LicenseBadge(text = library.licenseTitle)
                }
            }
        }
    }
}

@Composable
private fun LicenseBadge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AccentIconDisc(
    iconContent: @Composable () -> Unit,
    size: Int = 44
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides VLCThemeDefaults.colors.primary) {
                iconContent()
            }
        }
    }
}

@Composable
private fun LibraryLicenseDetail(
    library: VLCLibraryLicense,
    openLinkContentDescription: String,
    onOpenLicenseLink: (String) -> Unit,
    linkIconContent: @Composable () -> Unit,
    sourceIconContent: @Composable () -> Unit,
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
            AccentIconDisc(iconContent = sourceIconContent, size = 48)

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = library.title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (library.copyright.isNotBlank()) {
                    Text(
                        text = library.copyright,
                        color = colors.fontLight,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                if (library.licenseTitle.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    LicenseBadge(text = library.licenseTitle)
                }
            }

            if (library.licenseLink.isNotBlank()) {
                IconButton(
                    onClick = { onOpenLicenseLink(library.licenseLink) },
                    modifier = Modifier.semantics {
                        contentDescription = openLinkContentDescription
                    }
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.primary) {
                        linkIconContent()
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.defaultDivider)
        )

        Text(
            text = library.licenseDescription,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

data class VLCLibraryLicense(
    val title: String,
    val copyright: String,
    val licenseTitle: String,
    val licenseDescription: String,
    val licenseLink: String
)

@Composable
private fun DefaultLibrariesIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}
