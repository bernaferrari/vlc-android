package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCIconChip
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.viewmodel.MoreHubViewModel

/** Feature-scoped More hub UI, including independent history/stream retry states. */
@Composable
internal fun MorePane(
    modifier: Modifier,
    vm: MoreHubViewModel,
    onOpenSettings: () -> Unit,
    onOpenRemote: (() -> Unit)?,
    onOpenAbout: () -> Unit = {},
    onOpenDonate: () -> Unit = {},
    onPlayHistory: (HistoryEntry) -> Unit,
) {
    val state by vm.state.collectAsState()
    val colors = VLCThemeDefaults.colors
    var renameStreamId by remember { mutableStateOf<Long?>(null) }
    var renameStreamText by remember { mutableStateOf("") }
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("VLC", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (state.platformName.isNotBlank()) {
                Text(state.platformName, color = colors.fontLight, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { MoreAction(MaterialSymbols.Filled.Settings, ShellStrings.settings(), onOpenSettings) }
        item { MoreAction(MaterialSymbols.Filled.Info, ShellStrings.about(), onOpenAbout) }
        item { MoreAction(MaterialSymbols.Filled.Star, "Donate", onOpenDonate) }
        if (onOpenRemote != null) {
            item { MoreAction(MaterialSymbols.Filled.Devices, "Remote", onOpenRemote) }
        }

        item { Text("Streams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (renameStreamId != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = renameStreamText,
                        onValueChange = { renameStreamText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Rename stream") },
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                    TextButton(onClick = {
                        val id = renameStreamId
                        if (id != null && renameStreamText.isNotBlank()) {
                            vm.renameStream(id, renameStreamText.trim())
                        }
                        renameStreamId = null
                        renameStreamText = ""
                    }) { Text("Save") }
                    TextButton(onClick = {
                        renameStreamId = null
                        renameStreamText = ""
                    }) { Text(ShellStrings.cancel()) }
                }
            }
        }
        items(state.streams, key = { "s:${it.id}:${it.uri}" }) { stream ->
            VLCBrowserItemRow(
                title = stream.title,
                subtitle = stream.uri,
                onClick = { vm.playStream(stream) },
                artworkContent = {
                    Icon(MaterialSymbols.Filled.Devices, contentDescription = null, tint = colors.primary)
                },
                primaryActionContent = if (state.hasStreamRepository) {
                    { Icon(MaterialSymbols.Filled.Edit, contentDescription = "Rename stream") }
                } else {
                    null
                },
                onPrimaryActionClick = {
                    renameStreamId = stream.id
                    renameStreamText = stream.title
                },
                moreActionContent = if (state.hasStreamRepository) {
                    { Icon(MaterialSymbols.Filled.Delete, contentDescription = "Delete stream") }
                } else {
                    null
                },
                onMoreClick = { vm.deleteStream(stream.id) },
            )
        }
        if (state.streams.isEmpty() && !state.streamsLoading && state.streamsError == null) {
            item { Text("No streams", color = colors.fontLight) }
        }
        state.streamsError?.let { error ->
            item { RetryMessage(error = error, onRetry = vm::retryStreams) }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ShellStrings.history(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    if (state.historySelection.isNotEmpty()) {
                        TextButton(onClick = vm::removeSelectedHistory) {
                            Text("${ShellStrings.remove()} (${state.historySelection.size})")
                        }
                        TextButton(onClick = vm::clearHistorySelection) { Text(ShellStrings.clear()) }
                    }
                    TextButton(onClick = vm::clearHistory) { Text(ShellStrings.clear()) }
                }
            }
        }
        items(state.history, key = { "h:${it.item.id}:${it.playedAt}" }) { entry ->
            val key = "${entry.item.id}:${entry.playedAt}:${entry.item.uri}"
            val selected = key in state.historySelection
            VLCBrowserItemRow(
                title = entry.item.displayTitle,
                subtitle = listOfNotNull(entry.item.artist, entry.item.album).joinToString(" · ").ifBlank { null },
                selected = selected,
                onClick = { onPlayHistory(entry) },
                artworkContent = {
                    Icon(
                        if (entry.item.isVideo) MaterialSymbols.Filled.VideoLibrary else MaterialSymbols.Filled.MusicNote,
                        contentDescription = null,
                        tint = colors.primary,
                    )
                },
                badgeContent = {
                    Text(
                        if (entry.item.present) "present" else "missing",
                        color = if (entry.item.present) colors.fontLight else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                primaryActionContent = {
                    Icon(
                        MaterialSymbols.Filled.CheckCircle,
                        contentDescription = if (selected) "Selected" else "Select history entry",
                    )
                },
                onPrimaryActionClick = { vm.toggleHistorySelect(entry) },
                moreActionContent = {
                    Icon(MaterialSymbols.Filled.ArrowUpward, contentDescription = "Move to top")
                },
                onMoreClick = { vm.moveUp(entry) },
            )
        }
        state.historyError?.let { error ->
            item { RetryMessage(error = error, onRetry = vm::retryHistory) }
        }
        if (!state.loading && state.history.isEmpty() && state.historyError == null) {
            item { Text("No recent media", color = colors.fontLight) }
        }
    }
}

@Composable
private fun MoreAction(icon: MaterialIcon, label: String, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VLCIconChip(size = 44.dp) { tint ->
                Icon(icon, contentDescription = null, tint = tint)
            }
            Text(
                label,
                color = colors.listTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
