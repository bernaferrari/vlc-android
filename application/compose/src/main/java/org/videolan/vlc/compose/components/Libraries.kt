package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/license_activity.xml
 * - application/vlc-android/res/layout/library_item.xml
 *
 * Also provides the LibrariesActivity-owned Compose detail sheet that replaces
 * that screen's use of LicenseDialog. The legacy LicenseDialog remains available
 * for other legacy callers until those screens migrate.
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
                    contentPadding = PaddingValues(bottom = 54.dp)
                ) {
                    items(libraries) { library ->
                        LibraryLicenseRow(
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LibraryLicenseRow(
    library: VLCLibraryLicense,
    sourceIconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .focusable()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                sourceIconContent()
            }
        }

        Spacer(Modifier.width(32.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = library.title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
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

            Text(
                text = library.licenseTitle,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun LibraryLicenseDetail(
    library: VLCLibraryLicense,
    openLinkContentDescription: String,
    onOpenLicenseLink: (String) -> Unit,
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
                    text = library.licenseTitle,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                )

                Text(
                    text = library.copyright,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (library.licenseLink.isNotBlank()) {
                IconButton(
                    onClick = { onOpenLicenseLink(library.licenseLink) },
                    modifier = Modifier.semantics {
                        contentDescription = openLinkContentDescription
                    }
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
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

@Preview(name = "Libraries Light", showBackground = true)
@Composable
private fun VLCLibrariesScreenPreview() {
    VLCLibrariesScreen(
        title = "Libraries",
        closeContentDescription = "Close",
        openLinkContentDescription = "Open in browser",
        onClose = {},
        onOpenLicenseLink = {},
        libraries = previewLibraries()
    )
}

@Preview(name = "Libraries Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCLibrariesScreenDarkPreview() {
    VLCLibrariesScreen(
        title = "Libraries",
        closeContentDescription = "Close",
        openLinkContentDescription = "Open in browser",
        onClose = {},
        onOpenLicenseLink = {},
        libraries = previewLibraries()
    )
}

private fun previewLibraries() = listOf(
    VLCLibraryLicense(
        title = "libVLC",
        copyright = "Copyright (C) VideoLAN and VLC authors",
        licenseTitle = "LGPL v2.1 or later",
        licenseDescription = "GNU Lesser General Public License text appears here.",
        licenseLink = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
    ),
    VLCLibraryLicense(
        title = "Moshi",
        copyright = "Copyright (C) Square, Inc.",
        licenseTitle = "Apache License 2.0",
        licenseDescription = "Apache License text appears here.",
        licenseLink = "https://www.apache.org/licenses/LICENSE-2.0"
    )
)
