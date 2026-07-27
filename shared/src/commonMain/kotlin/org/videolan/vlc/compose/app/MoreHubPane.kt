package org.videolan.vlc.compose.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import org.videolan.vlc.compose.components.VLCSettingsCard
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.isPlayableStreamUri

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
    onPlayStream: (MediaItem) -> Unit,
    onOpenStream: (title: String, uri: String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val colors = VLCThemeDefaults.colors
    var renameStreamId by remember { mutableStateOf<Long?>(null) }
    var renameStreamText by remember { mutableStateOf("") }
    var addingStream by remember { mutableStateOf(false) }
    var newStreamName by remember { mutableStateOf("") }
    var newStreamUri by remember { mutableStateOf("") }
    var streamAddressError by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                ShellStrings.more(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            // The shell already owns the VLC title. Group these destinations as a
            // compact directory instead of repeating the brand in a second
            // oversized header and rendering each action as an isolated card.
            VLCSettingsCard(
                rows = buildList {
                    add { MoreAction(MaterialSymbols.Filled.Settings, ShellStrings.settings(), onOpenSettings) }
                    add { MoreAction(MaterialSymbols.Filled.Info, ShellStrings.about(), onOpenAbout) }
                    add { MoreAction(MaterialSymbols.Filled.Star, ShellStrings.donate(), onOpenDonate) }
                    onOpenRemote?.let { remote ->
                        add { MoreAction(MaterialSymbols.Filled.Devices, ShellStrings.remoteAccess(), remote) }
                    }
                },
                dividerInset = 72.dp,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoreSectionTitle(ShellStrings.streams())
                TextButton(onClick = {
                    addingStream = !addingStream
                    streamAddressError = false
                }) {
                    Icon(MaterialSymbols.Filled.Add, contentDescription = null)
                    Text(ShellStrings.newStream(), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        if (addingStream) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = newStreamName,
                            onValueChange = { newStreamName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(ShellStrings.streamName()) },
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                        OutlinedTextField(
                            value = newStreamUri,
                            onValueChange = {
                                newStreamUri = it
                                streamAddressError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = streamAddressError,
                            supportingText = if (streamAddressError) {
                                { Text(ShellStrings.invalidStreamAddress()) }
                            } else {
                                null
                            },
                            label = { Text(ShellStrings.streamAddress()) },
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            TextButton(onClick = {
                                addingStream = false
                                newStreamName = ""
                                newStreamUri = ""
                                streamAddressError = false
                            }) { Text(ShellStrings.cancel()) }
                            if (state.hasStreamRepository) {
                                TextButton(onClick = {
                                    if (isPlayableStreamUri(newStreamUri)) {
                                        vm.addStream(newStreamName.ifBlank { newStreamUri }, newStreamUri.trim())
                                        addingStream = false
                                        newStreamName = ""
                                        newStreamUri = ""
                                    } else {
                                        streamAddressError = true
                                    }
                                }) { Text(ShellStrings.save()) }
                            }
                            TextButton(onClick = {
                                if (isPlayableStreamUri(newStreamUri)) {
                                    onOpenStream(newStreamName, newStreamUri)
                                } else {
                                    streamAddressError = true
                                }
                            }) { Text(ShellStrings.play()) }
                        }
                    }
                }
            }
        }
        if (renameStreamId != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = renameStreamText,
                        onValueChange = { renameStreamText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(ShellStrings.renameStream()) },
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                    TextButton(onClick = {
                        val id = renameStreamId
                        if (id != null && renameStreamText.isNotBlank()) {
                            vm.renameStream(id, renameStreamText.trim())
                        }
                        renameStreamId = null
                        renameStreamText = ""
                    }) { Text(ShellStrings.save()) }
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
                onClick = { onPlayStream(stream) },
                artworkContent = {
                    Icon(MaterialSymbols.Filled.Devices, contentDescription = null, tint = colors.primary)
                },
                primaryActionContent = if (state.hasStreamRepository) {
                    { Icon(MaterialSymbols.Filled.Edit, contentDescription = ShellStrings.renameStream()) }
                } else {
                    null
                },
                onPrimaryActionClick = {
                    renameStreamId = stream.id
                    renameStreamText = stream.title
                },
                moreActionContent = if (state.hasStreamRepository) {
                    { Icon(MaterialSymbols.Filled.Delete, contentDescription = ShellStrings.deleteStream()) }
                } else {
                    null
                },
                onMoreClick = { vm.deleteStream(stream.id) },
            )
        }
        if (state.streams.isEmpty() && !state.streamsLoading && state.streamsError == null) {
            item { Text(ShellStrings.noStreams(), color = colors.fontLight) }
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
                MoreSectionTitle(ShellStrings.history())
                Row {
                    if (state.historySelection.isNotEmpty()) {
                        TextButton(onClick = vm::removeSelectedHistory) {
                            Text(ShellStrings.selectionCount(ShellStrings.remove(), state.historySelection.size))
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
                        if (entry.item.present) ShellStrings.present() else ShellStrings.missing(),
                        color = if (entry.item.present) colors.fontLight else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                primaryActionContent = {
                    Icon(
                        MaterialSymbols.Filled.CheckCircle,
                        contentDescription = if (selected) ShellStrings.selected() else ShellStrings.selectHistoryEntry(),
                    )
                },
                onPrimaryActionClick = { vm.toggleHistorySelect(entry) },
                moreActionContent = {
                    Icon(MaterialSymbols.Filled.ArrowUpward, contentDescription = ShellStrings.moveUp())
                },
                onMoreClick = { vm.moveUp(entry) },
            )
        }
        state.historyError?.let { error ->
            item { RetryMessage(error = error, onRetry = vm::retryHistory) }
        }
        if (!state.loading && state.history.isEmpty() && state.historyError == null) {
            item { Text(ShellStrings.noRecentMedia(), color = colors.fontLight) }
        }
    }
}

@Composable
private fun MoreAction(icon: MaterialIcon, label: String, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VLCIconChip(size = 40.dp) { tint ->
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

@Composable
private fun MoreSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}
