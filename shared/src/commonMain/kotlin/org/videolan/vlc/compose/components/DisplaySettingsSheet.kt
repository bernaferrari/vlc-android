package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.viewmodel.ViewMode
import org.jetbrains.compose.resources.stringResource
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.*

data class DisplaySettingsState(
    val viewMode: ViewMode = ViewMode.LIST,
    val onlyFavorites: Boolean = false,
    val sort: MediaSort = MediaSort.TITLE,
    val sortDesc: Boolean = false,
    val supportsViewMode: Boolean = true,
    val supportsFavorites: Boolean = true,
    val supportsSorting: Boolean = true,
    val showAllArtists: Boolean? = null,
    val showHiddenFiles: Boolean? = null,
    val showOnlyMultimedia: Boolean? = null,
    val showTrackNumbers: Boolean? = null,
    val groupingLabel: String? = null,
    val groupingOptions: List<String> = emptyList(),
    val selectedGrouping: String? = null,
    val defaultActionLabel: String? = null,
    val defaultActionOptions: List<String> = emptyList(),
    val selectedDefaultAction: String? = null,
    val availableSorts: List<MediaSort> = listOf(
        MediaSort.TITLE,
        MediaSort.FILENAME,
        MediaSort.ARTIST,
        MediaSort.ALBUM,
        MediaSort.DURATION,
        MediaSort.RECENT,
    ),
)

/**
 * Shared display-settings bottom sheet — list/grid, favorites, sort±desc,
 * optional browser/audio/video toggles. Internal labels are composeResources-backed;
 * hosts may override the title and supply custom grouping/action option tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsSheet(
    state: DisplaySettingsState,
    title: String? = null,
    onDismiss: () -> Unit,
    onViewMode: (ViewMode) -> Unit = {},
    onOnlyFavorites: (Boolean) -> Unit = {},
    onSort: (MediaSort) -> Unit = {},
    onSortDesc: (Boolean) -> Unit = {},
    onShowAllArtists: (Boolean) -> Unit = {},
    onShowHiddenFiles: (Boolean) -> Unit = {},
    onShowOnlyMultimedia: (Boolean) -> Unit = {},
    onShowTrackNumbers: (Boolean) -> Unit = {},
    onGrouping: (String) -> Unit = {},
    onDefaultAction: (String) -> Unit = {},
) {
    val colors = VLCThemeDefaults.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundDefault,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VLCLayout.SheetHorizontalPadding, vertical = 8.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            VLCModalHeader(
                title = title ?: stringResource(Res.string.display_settings),
                onDismiss = onDismiss,
            )

            if (state.supportsViewMode) {
                DisplaySectionTitle(stringResource(Res.string.layout))
                DisplayChoiceGrid(
                    options = listOf(ViewMode.LIST, ViewMode.GRID),
                    selected = state.viewMode,
                    label = { if (it == ViewMode.LIST) stringResource(Res.string.list) else stringResource(Res.string.grid) },
                    onSelect = onViewMode,
                )
            }

            val filterRows = buildList<@Composable () -> Unit> {
                if (state.supportsFavorites) add { VLCSettingsToggleRow(stringResource(Res.string.favorites_only), state.onlyFavorites, onOnlyFavorites) }
                state.showAllArtists?.let { checked -> add { VLCSettingsToggleRow(stringResource(Res.string.show_all_artists), checked, onShowAllArtists) } }
                state.showTrackNumbers?.let { checked -> add { VLCSettingsToggleRow(stringResource(Res.string.show_track_numbers), checked, onShowTrackNumbers) } }
                state.showOnlyMultimedia?.let { checked -> add { VLCSettingsToggleRow(stringResource(Res.string.multimedia_files_only), checked, onShowOnlyMultimedia) } }
                state.showHiddenFiles?.let { checked -> add { VLCSettingsToggleRow(stringResource(Res.string.show_hidden_files), checked, onShowHiddenFiles) } }
            }
            if (filterRows.isNotEmpty()) {
                DisplaySectionTitle(stringResource(Res.string.filters))
                VLCSettingsSegmentedCard(rows = filterRows)
            }

            if (state.groupingOptions.isNotEmpty()) {
                DisplaySectionTitle(state.groupingLabel ?: stringResource(Res.string.grouping))
                DisplayChoiceGrid(
                    options = state.groupingOptions,
                    selected = state.selectedGrouping,
                    label = { it.displayLabel() },
                    onSelect = onGrouping,
                )
            }

            if (state.defaultActionOptions.isNotEmpty()) {
                DisplaySectionTitle(state.defaultActionLabel ?: stringResource(Res.string.default_action))
                DisplayChoiceGrid(
                    options = state.defaultActionOptions,
                    selected = state.selectedDefaultAction,
                    label = { it.displayLabel() },
                    onSelect = onDefaultAction,
                )
            }

            if (state.supportsSorting) {
                DisplaySectionTitle(stringResource(Res.string.sort))
                DisplayChoiceGrid(
                    options = state.availableSorts,
                    selected = state.sort,
                    label = { it.displayLabel() },
                    onSelect = onSort,
                )
                DisplayChoiceGrid(
                    options = listOf(false, true),
                    selected = state.sortDesc,
                    label = {
                        if (it) stringResource(Res.string.descending) else stringResource(Res.string.ascending)
                    },
                    onSelect = onSortDesc,
                    cellMinHeight = 48.dp,
                )
            }

        }
    }
}

@Composable
private fun <T> DisplayChoiceGrid(
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    cellMinHeight: Dp = 54.dp,
) {
    Column(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                rowOptions.forEachIndexed { columnIndex, option ->
                    val isSelected = option == selected
                    val isFirstRow = rowIndex == 0
                    val isLastRow = rowIndex == (options.size - 1) / 2
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = if (isFirstRow && columnIndex == 0) 22.dp else 6.dp,
                        topEnd = if (isFirstRow && (columnIndex == 1 || rowOptions.size == 1)) 22.dp else 6.dp,
                        bottomStart = if (isLastRow && columnIndex == 0) 22.dp else 6.dp,
                        bottomEnd = if (isLastRow && (columnIndex == 1 || rowOptions.size == 1)) 22.dp else 6.dp,
                    )
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = cellMinHeight)
                            .clip(shape)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            ),
                        shape = shape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ) {
                        Text(
                            text = label(option),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                if (rowOptions.size == 1 && options.size > 1) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DisplaySectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = VLCThemeDefaults.colors.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun String.displayLabel(): String = when (this) {
    "NONE" -> stringResource(Res.string.none)
    "NAME" -> stringResource(Res.string.by_name)
    "FOLDER" -> stringResource(Res.string.by_folder)
    "PLAY" -> stringResource(Res.string.play)
    "PLAY_ALL" -> stringResource(Res.string.play_all)
    "ADD_TO_QUEUE" -> stringResource(Res.string.append)
    "INSERT_NEXT" -> stringResource(Res.string.insert_next)
    else -> this
}

@Composable
private fun MediaSort.displayLabel(): String = when (this) {
    MediaSort.DEFAULT -> stringResource(Res.string.default_sort)
    MediaSort.TITLE -> stringResource(Res.string.sortby_name)
    MediaSort.FILENAME -> stringResource(Res.string.sortby_filename)
    MediaSort.ARTIST -> stringResource(Res.string.sortby_artist_name)
    MediaSort.ALBUM -> stringResource(Res.string.sortby_album_name)
    MediaSort.DURATION -> stringResource(Res.string.sortby_length)
    MediaSort.RELEASE_DATE -> stringResource(Res.string.sortby_date_release)
    MediaSort.LAST_MODIFIED -> stringResource(Res.string.sortby_last_modified_date)
    MediaSort.INSERTION_DATE -> stringResource(Res.string.sortby_insertion)
    MediaSort.FILE_SIZE -> stringResource(Res.string.file_size)
    MediaSort.TRACK_COUNT -> stringResource(Res.string.sortby_number)
    MediaSort.RECENT -> stringResource(Res.string.sortby_date)
}
