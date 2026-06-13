package org.videolan.vlc.compose.components

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

data class VLCSearchResultRow(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnailWide: Boolean
)

data class VLCSearchSection(
    val title: String,
    val rows: List<VLCSearchResultRow>
)

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/search_activity.xml
 * - application/vlc-android/res/layout/search_item.xml
 *
 * SearchActivity keeps ownership of the medialibrary query, media playback,
 * ACTION_SEARCH intents, and resource-specific icons. This screen owns the
 * query input, clear/back controls, sectioned results, and empty state.
 */
@Composable
fun VLCSearchScreen(
    query: String,
    hint: String,
    emptyText: String,
    backContentDescription: String,
    clearContentDescription: String,
    sections: List<VLCSearchSection>,
    showEmpty: Boolean,
    autoFocus: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onResultClick: (sectionIndex: Int, rowIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    backIconContent: @Composable () -> Unit = { DefaultSearchIconPlaceholder() },
    clearIconContent: @Composable () -> Unit = { DefaultSearchIconPlaceholder() },
    emptyIconContent: @Composable () -> Unit = { DefaultSearchIconPlaceholder() },
    thumbnailContent: @Composable (sectionIndex: Int, rowIndex: Int, row: VLCSearchResultRow) -> Unit = { _, _, row ->
        SearchThumbnail(
            width = if (row.thumbnailWide) SearchResultThumbnailWideWidth else SearchResultThumbnailSquareSize,
            height = if (row.thumbnailWide) SearchResultThumbnailWideHeight else SearchResultThumbnailSquareSize
        )
    }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchTopBar(
                    query = query,
                    hint = hint,
                    backContentDescription = backContentDescription,
                    clearContentDescription = clearContentDescription,
                    autoFocus = autoFocus,
                    onQueryChange = onQueryChange,
                    onSearchAction = onSearchAction,
                    onBack = onBack,
                    onClear = onClear,
                    backIconContent = backIconContent,
                    clearIconContent = clearIconContent
                )

                if (showEmpty) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(LocalContentColor provides colors.fontLight) {
                                emptyIconContent()
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = emptyText,
                            color = colors.fontDefault,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    SearchResults(
                        sections = sections,
                        onResultClick = onResultClick,
                        thumbnailContent = thumbnailContent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    hint: String,
    backContentDescription: String,
    clearContentDescription: String,
    autoFocus: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    backIconContent: @Composable () -> Unit,
    clearIconContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundDefault)
            .padding(start = 4.dp, top = 4.dp, end = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics {
                contentDescription = backContentDescription
            }
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                backIconContent()
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(hint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearchAction()
                }
            ),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.semantics {
                            contentDescription = clearContentDescription
                        }
                    ) {
                        CompositionLocalProvider(LocalContentColor provides colors.fontLight) {
                            clearIconContent()
                        }
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
    }
}

@Composable
private fun SearchResults(
    sections: List<VLCSearchSection>,
    onResultClick: (sectionIndex: Int, rowIndex: Int) -> Unit,
    thumbnailContent: @Composable (sectionIndex: Int, rowIndex: Int, row: VLCSearchResultRow) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleSections = sections.filter { it.rows.isNotEmpty() }

    LazyColumn(
        modifier = modifier.background(VLCThemeDefaults.colors.backgroundDefault),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        visibleSections.forEachIndexed { sectionIndex, section ->
            item(key = "title-${section.title}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = section.title,
                        color = VLCThemeDefaults.colors.primary,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = section.rows.size.toString(),
                        color = VLCThemeDefaults.colors.fontLight,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            item(key = "section-${section.title}") {
                SearchSectionCard {
                    section.rows.forEachIndexed { rowIndex, row ->
                        if (rowIndex > 0) SearchRowDivider()
                        SearchResultRow(
                            row = row,
                            onClick = { onResultClick(sectionIndex, rowIndex) },
                            thumbnailContent = { thumbnailContent(sectionIndex, rowIndex, row) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = VLCThemeDefaults.colors.fontDefault
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun SearchRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 76.dp, end = 12.dp)
            .height(1.dp)
            .background(VLCThemeDefaults.colors.defaultDivider)
    )
}

@Composable
private fun SearchResultRow(
    row: VLCSearchResultRow,
    onClick: () -> Unit,
    thumbnailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    val thumbnailWidth = if (row.thumbnailWide) SearchResultThumbnailWideWidth else SearchResultThumbnailSquareSize
    val thumbnailHeight = if (row.thumbnailWide) SearchResultThumbnailWideHeight else SearchResultThumbnailSquareSize

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = thumbnailHeight + 16.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(thumbnailWidth)
                .height(thumbnailHeight),
            contentAlignment = Alignment.Center
        ) {
            thumbnailContent()
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = row.title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!row.description.isNullOrBlank()) {
                Text(
                    text = row.description,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchThumbnail(width: Dp, height: Dp) {
    val colors = VLCThemeDefaults.colors

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(colors.fontLight.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
        )
    }
}

private val SearchResultThumbnailSquareSize = 48.dp
private val SearchResultThumbnailWideWidth = 100.dp
private val SearchResultThumbnailWideHeight = 56.dp

@Composable
private fun DefaultSearchIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}

@Preview(name = "Search Light", showBackground = true)
@Composable
private fun VLCSearchScreenPreview() {
    VLCSearchScreen(
        query = "beethoven",
        hint = "Search media",
        emptyText = "No media found",
        backContentDescription = "Back",
        clearContentDescription = "Clear",
        autoFocus = false,
        sections = listOf(
            VLCSearchSection(
                title = "Tracks",
                rows = listOf(
                    VLCSearchResultRow("track-1", "Symphony No. 5", "Ludwig van Beethoven", false),
                    VLCSearchResultRow("track-2", "Piano Concerto No. 5", "Classical collection", false)
                )
            ),
            VLCSearchSection(
                title = "Videos",
                rows = listOf(
                    VLCSearchResultRow("video-1", "Live concert", "1:42:17  -  1080p", true)
                )
            )
        ),
        showEmpty = false,
        onQueryChange = {},
        onSearchAction = {},
        onBack = {},
        onClear = {},
        onResultClick = { _, _ -> }
    )
}

@Preview(name = "Search Empty Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCSearchScreenEmptyPreview() {
    VLCSearchScreen(
        query = "missing",
        hint = "Search media",
        emptyText = "No media found",
        backContentDescription = "Back",
        clearContentDescription = "Clear",
        autoFocus = false,
        sections = emptyList(),
        showEmpty = true,
        onQueryChange = {},
        onSearchAction = {},
        onBack = {},
        onClear = {},
        onResultClick = { _, _ -> }
    )
}
