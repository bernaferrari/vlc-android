package org.videolan.vlc.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.viewmodel.ViewMode

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
 * optional browser/audio/video toggles. Hosts supply localized labels via [title]
 * and option strings; no Android R.* dependency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsSheet(
    state: DisplaySettingsState,
    title: String = "Display settings",
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
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (state.supportsViewMode) {
                Text("Layout", style = MaterialTheme.typography.titleSmall, color = colors.primary)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.viewMode == ViewMode.LIST,
                        onClick = { onViewMode(ViewMode.LIST) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("List") },
                    )
                    SegmentedButton(
                        selected = state.viewMode == ViewMode.GRID,
                        onClick = { onViewMode(ViewMode.GRID) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("Grid") },
                    )
                }
            }

            if (state.supportsFavorites) {
                SwitchRow("Favorites only", state.onlyFavorites, onOnlyFavorites)
            }

            state.showAllArtists?.let { SwitchRow("Show all artists", it, onShowAllArtists) }
            state.showTrackNumbers?.let { SwitchRow("Show track numbers", it, onShowTrackNumbers) }
            state.showOnlyMultimedia?.let { SwitchRow("Multimedia files only", it, onShowOnlyMultimedia) }
            state.showHiddenFiles?.let { SwitchRow("Show hidden files", it, onShowHiddenFiles) }

            if (state.groupingOptions.isNotEmpty()) {
                Text(
                    state.groupingLabel ?: "Grouping",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.groupingOptions.forEach { option ->
                        FilterChip(
                            selected = option == state.selectedGrouping,
                            onClick = { onGrouping(option) },
                            label = { Text(option) },
                        )
                    }
                }
            }

            if (state.defaultActionOptions.isNotEmpty()) {
                Text(
                    state.defaultActionLabel ?: "Default action",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.defaultActionOptions.forEach { option ->
                        FilterChip(
                            selected = option == state.selectedDefaultAction,
                            onClick = { onDefaultAction(option) },
                            label = { Text(option) },
                        )
                    }
                }
            }

            if (state.supportsSorting) {
                Text("Sort", style = MaterialTheme.typography.titleSmall, color = colors.primary)
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
                            sort.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                            color = if (selected) colors.primary else colors.fontDefault,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (selected) {
                            Text(
                                if (state.sortDesc) "Desc ▼" else "Asc ▲",
                                color = colors.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Done")
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
