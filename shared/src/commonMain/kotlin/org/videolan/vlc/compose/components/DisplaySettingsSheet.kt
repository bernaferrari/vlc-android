package org.videolan.vlc.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                title ?: stringResource(Res.string.display_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            if (state.supportsViewMode) {
                Text(stringResource(Res.string.layout), style = MaterialTheme.typography.titleSmall, color = colors.primary)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.viewMode == ViewMode.LIST,
                        onClick = { onViewMode(ViewMode.LIST) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text(stringResource(Res.string.list)) },
                    )
                    SegmentedButton(
                        selected = state.viewMode == ViewMode.GRID,
                        onClick = { onViewMode(ViewMode.GRID) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text(stringResource(Res.string.grid)) },
                    )
                }
            }

            if (state.supportsFavorites) {
                SwitchRow(stringResource(Res.string.favorites_only), state.onlyFavorites, onOnlyFavorites)
            }

            state.showAllArtists?.let { SwitchRow(stringResource(Res.string.show_all_artists), it, onShowAllArtists) }
            state.showTrackNumbers?.let { SwitchRow(stringResource(Res.string.show_track_numbers), it, onShowTrackNumbers) }
            state.showOnlyMultimedia?.let { SwitchRow(stringResource(Res.string.multimedia_files_only), it, onShowOnlyMultimedia) }
            state.showHiddenFiles?.let { SwitchRow(stringResource(Res.string.show_hidden_files), it, onShowHiddenFiles) }

            if (state.groupingOptions.isNotEmpty()) {
                Text(
                    state.groupingLabel ?: stringResource(Res.string.grouping),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.groupingOptions.forEach { option ->
                        FilterChip(
                            selected = option == state.selectedGrouping,
                            onClick = { onGrouping(option) },
                            label = { Text(option.displayLabel()) },
                        )
                    }
                }
            }

            if (state.defaultActionOptions.isNotEmpty()) {
                Text(
                    state.defaultActionLabel ?: stringResource(Res.string.default_action),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.defaultActionOptions.forEach { option ->
                        FilterChip(
                            selected = option == state.selectedDefaultAction,
                            onClick = { onDefaultAction(option) },
                            label = { Text(option.displayLabel()) },
                        )
                    }
                }
            }

            if (state.supportsSorting) {
                Text(stringResource(Res.string.sort), style = MaterialTheme.typography.titleSmall, color = colors.primary)
                state.availableSorts.forEach { sort ->
                    val selected = sort == state.sort
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selected) onSortDesc(!state.sortDesc) else onSort(sort)
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            sort.displayLabel(),
                            color = if (selected) colors.primary else colors.fontDefault,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (selected) {
                            Text(
                                if (state.sortDesc) stringResource(Res.string.descending) else stringResource(Res.string.ascending),
                                color = colors.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
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
