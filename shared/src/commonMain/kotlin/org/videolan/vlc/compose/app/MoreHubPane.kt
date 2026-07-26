package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
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
        item { MoreAction(ShellStrings.settings(), onOpenSettings) }
        item { MoreAction(ShellStrings.about(), onOpenAbout) }
        item { MoreAction("Donate", onOpenDonate) }
        if (onOpenRemote != null) {
            item { MoreAction("Remote", onOpenRemote) }
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { MediaRow(stream) { vm.playStream(stream) } }
                if (state.hasStreamRepository) {
                    TextButton(onClick = {
                        renameStreamId = stream.id
                        renameStreamText = stream.title
                    }) { Text("Ren") }
                    TextButton(onClick = { vm.deleteStream(stream.id) }) { Text("Del") }
                }
            }
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (selected) colors.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .clickable { onPlayHistory(entry) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { MediaRow(entry.item) { onPlayHistory(entry) } }
                Column(horizontalAlignment = Alignment.End) {
                    if (!entry.item.present) {
                        Text("missing", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text("present", color = colors.fontLight, style = MaterialTheme.typography.labelSmall)
                    }
                    Row {
                        TextButton(onClick = { vm.toggleHistorySelect(entry) }) { Text(if (selected) "✓" else "Sel") }
                        TextButton(onClick = { vm.moveUp(entry) }) { Text("↑") }
                    }
                }
            }
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
private fun MoreAction(label: String, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(label, color = colors.listTitle, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MediaRow(item: MediaItem, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    item.isVideo -> "VID"
                    item.isAudio -> "AUD"
                    else -> "•"
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.listTitle,
                fontWeight = FontWeight.Medium,
            )
            val subtitle = listOfNotNull(item.artist, item.album).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.listSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
