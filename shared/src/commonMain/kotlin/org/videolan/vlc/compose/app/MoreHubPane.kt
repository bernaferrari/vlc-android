package org.videolan.vlc.compose.app

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import org.videolan.vlc.compose.components.VLCModalHeader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import org.videolan.vlc.compose.components.VLCConfirmActionDialog
import org.jetbrains.compose.resources.stringResource
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.remove_history_message
import vlc_android.shared.generated.resources.remove_stream_message
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.components.VLCIconChip
import org.videolan.vlc.compose.components.VLCListItemPosition
import org.videolan.vlc.compose.components.VLCNavigationRow
import org.videolan.vlc.compose.components.VLCPageHeader
import org.videolan.vlc.compose.components.VLCSelectionContextBar
import org.videolan.vlc.compose.components.VLCSelectionCheckIndicator
import org.videolan.vlc.compose.components.VLCRenameItemDialog
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.VlcTextUtils
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.isPlayableStreamUri

/** Feature-scoped More hub UI, including independent history/stream retry states. */
@OptIn(ExperimentalMaterial3Api::class)
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
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = VLCThemeDefaults.colors
    var renameStreamId by remember { mutableStateOf<Long?>(null) }
    var addingStream by remember { mutableStateOf(false) }
    var newStreamName by remember { mutableStateOf("") }
    var newStreamUri by remember { mutableStateOf("") }
    var streamAddressError by remember { mutableStateOf(false) }
    var deleteStreamTarget by remember { mutableStateOf<MediaItem?>(null) }
    var confirmHistoryRemoval by remember { mutableStateOf(false) }
    var confirmHistoryClear by remember { mutableStateOf(false) }
    val streamNameFocusRequester = remember { FocusRequester() }
    val streamUriFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun dismissStreamSheet() {
        addingStream = false
        streamAddressError = false
        keyboardController?.hide()
    }
    fun dismissStreamEditor() {
        newStreamName = ""
        newStreamUri = ""
        dismissStreamSheet()
    }
    fun submitStream(save: Boolean) {
        val uri = newStreamUri.trim()
        if (!isPlayableStreamUri(uri)) {
            streamAddressError = true
            streamUriFocusRequester.requestFocus()
            return
        }
        if (save) vm.addStream(newStreamName.trim().ifBlank { uri }, uri)
        else onOpenStream(newStreamName.trim(), uri)
        dismissStreamEditor()
    }
    LaunchedEffect(addingStream) {
        if (addingStream) {
            kotlinx.coroutines.yield()
            streamNameFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val navigationActions = buildList {
        add(
            MoreHubAction(
                MaterialSymbols.Filled.Settings,
                ShellStrings.settings(),
                ShellStrings.settingsSummary(),
                onOpenSettings,
            ),
        )
        add(
            MoreHubAction(
                MaterialSymbols.Filled.Info,
                ShellStrings.about(),
                ShellStrings.aboutSummary(),
                onOpenAbout,
            ),
        )
        add(
            MoreHubAction(
                MaterialSymbols.Filled.Star,
                ShellStrings.donate(),
                ShellStrings.donateSummary(),
                onOpenDonate,
            ),
        )
        onOpenRemote?.let { remote ->
            add(
                MoreHubAction(
                    MaterialSymbols.Filled.Devices,
                    ShellStrings.remoteAccess(),
                    ShellStrings.remoteAccessSummary(),
                    remote,
                ),
            )
        }
    }
    VLCUtilityPane(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            VLCPageHeader(title = ShellStrings.more())
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = VLCLayout.ScreenGutter),
                // Keep the last history/stream row above the mini-player and the adaptive
                // navigation surface on every host. The outer Scaffold owns system insets;
                // this small gutter is the shared visual breathing room between sections.
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = VLCLayout.ScreenGutter,
                ),
                // A group is joined by 2dp; section headers own the breathable gaps between groups.
                // Connected rows keep the navigation group visually distinct from media history.
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
            itemsIndexed(navigationActions) { index, action ->
                MoreAction(
                    icon = action.icon,
                    label = action.label,
                    summary = action.summary,
                    onClick = action.onClick,
                    position = moreActionPosition(index, navigationActions.size),
                )
            }

            item {
                MoreSectionHeader(
                    title = ShellStrings.streams(),
                    modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
                ) {
                    TextButton(onClick = {
                        addingStream = true
                        streamAddressError = false
                    }) {
                        Icon(MaterialSymbols.Filled.Add, contentDescription = null)
                        Text(ShellStrings.newStream(), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (state.streams.isEmpty() && !state.streamsLoading && state.streamsError == null) {
                item {
                    MoreEmptySection(
                        text = ShellStrings.noStreams(),
                        symbol = MaterialSymbols.Filled.Devices,
                    )
                }
            }
            itemsIndexed(state.streams, key = { _, stream -> "s:${stream.id}:${stream.uri}" }) { index, stream ->
                VLCBrowserItemRow(
                    title = stream.title,
                    subtitle = stream.uri,
                    position = moreActionPosition(index, state.streams.size),
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
                    },
                    moreActionContent = if (state.hasStreamRepository) {
                        { Icon(MaterialSymbols.Filled.Delete, contentDescription = ShellStrings.deleteStream()) }
                    } else {
                        null
                    },
                    moreActionContentDescription = ShellStrings.deleteStream().takeIf { state.hasStreamRepository },
                    onMoreClick = { deleteStreamTarget = stream },
                )
            }
            state.streamActionError?.let { error ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = vm::clearStreamActionError) { Text(ShellStrings.clear()) }
                        }
                    }
                }
            }
            state.streamsError?.let { error ->
                item { RetryMessage(error = error, onRetry = vm::retryStreams) }
            }

            item {
                if (state.historySelection.isNotEmpty()) {
                    VLCSelectionContextBar(
                        title = ShellStrings.selectionCount(ShellStrings.selected(), state.historySelection.size),
                        clearContentDescription = ShellStrings.clear(),
                        onClearSelection = vm::clearHistorySelection,
                        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
                    ) {
                        IconButton(onClick = { confirmHistoryRemoval = true }) {
                            Icon(MaterialSymbols.Filled.Delete, contentDescription = ShellStrings.remove())
                        }
                    }
                } else {
                    MoreSectionHeader(
                        title = ShellStrings.history(),
                        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
                    ) {
                        if (state.history.isNotEmpty()) {
                            TextButton(onClick = { confirmHistoryClear = true }) { Text(ShellStrings.clear()) }
                        }
                    }
                }
            }
            if (!state.loading && state.history.isEmpty() && state.historyError == null) {
                item {
                    MoreEmptySection(
                        text = ShellStrings.noRecentMedia(),
                        symbol = MaterialSymbols.Filled.History,
                    )
                }
            }
            itemsIndexed(state.history, key = { _, entry -> "h:${entry.item.id}:${entry.playedAt}" }) { index, entry ->
                val key = "${entry.item.id}:${entry.playedAt}:${entry.item.uri}"
                val selected = key in state.historySelection
                VLCBrowserItemRow(
                    title = entry.item.displayTitle,
                    subtitle = VlcTextUtils.separatedString(
                        arrayOf(entry.item.artist, entry.item.album),
                    ).ifBlank { null },
                    selected = selected,
                    position = moreActionPosition(index, state.history.size),
                    onClick = {
                        if (state.historySelection.isNotEmpty()) vm.toggleHistorySelect(entry)
                        else onPlayHistory(entry)
                    },
                    onLongClick = { vm.toggleHistorySelect(entry) },
                    artworkContent = {
                        if (selected) {
                            VLCSelectionCheckIndicator(modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(
                                if (entry.item.isVideo) MaterialSymbols.Filled.VideoLibrary else MaterialSymbols.Filled.MusicNote,
                                contentDescription = null,
                                tint = colors.primary,
                            )
                        }
                    },
                    badgeContent = {
                        if (!entry.item.present) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    MaterialSymbols.Filled.Warning,
                                    contentDescription = ShellStrings.missing(),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    ShellStrings.missing(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    },
                )
            }
            state.historyError?.let { error ->
                item { RetryMessage(error = error, onRetry = vm::retryHistory) }
            }
            }
        }
    }
    if (addingStream) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = ::dismissStreamSheet,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(
                    start = VLCLayout.SheetHorizontalPadding,
                    top = 8.dp,
                    end = VLCLayout.SheetHorizontalPadding,
                    bottom = VLCLayout.SheetBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VLCModalHeader(title = ShellStrings.newStream(), onDismiss = ::dismissStreamSheet)
                OutlinedTextField(
                    value = newStreamName,
                    onValueChange = { newStreamName = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(streamNameFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { streamUriFocusRequester.requestFocus() }),
                    singleLine = true,
                    label = { Text(ShellStrings.streamName()) },
                    shape = MaterialTheme.shapes.large,
                )
                OutlinedTextField(
                    value = newStreamUri,
                    onValueChange = {
                        newStreamUri = it
                        streamAddressError = false
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(streamUriFocusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submitStream(save = false) }, onDone = { submitStream(save = false) }),
                    singleLine = true,
                    isError = streamAddressError,
                    supportingText = if (streamAddressError) {
                        { Text(ShellStrings.invalidStreamAddress()) }
                    } else {
                        null
                    },
                    label = { Text(ShellStrings.streamAddress()) },
                    shape = MaterialTheme.shapes.large,
                )
                FlowRow(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = ::dismissStreamEditor) { Text(ShellStrings.cancel()) }
                    if (state.hasStreamRepository) {
                        TextButton(enabled = newStreamUri.isNotBlank(), onClick = { submitStream(save = true) }) {
                            Text(ShellStrings.save())
                        }
                    }
                    Button(enabled = newStreamUri.isNotBlank(), onClick = { submitStream(save = false) }) {
                        Text(ShellStrings.play())
                    }
                }
            }
        }
    }
    renameStreamId?.let { streamId ->
        val stream = state.streams.firstOrNull { it.id == streamId }
        if (stream != null) {
            VLCRenameItemDialog(
                title = ShellStrings.renameStream(),
                initialValue = stream.title,
                confirmLabel = ShellStrings.save(),
                cancelLabel = ShellStrings.cancel(),
                onConfirm = { value ->
                    vm.renameStream(streamId, value)
                    renameStreamId = null
                },
                onDismiss = { renameStreamId = null },
            )
        }
    }
    if (confirmHistoryRemoval || confirmHistoryClear) {
        val isBulkRemoval = confirmHistoryRemoval
        VLCConfirmActionDialog(
            title = if (isBulkRemoval) ShellStrings.remove() else ShellStrings.clear(),
            message = stringResource(Res.string.remove_history_message),
            target = if (isBulkRemoval) ShellStrings.itemsCount(state.historySelection.size) else ShellStrings.playbackHistory(),
            confirmLabel = if (isBulkRemoval) ShellStrings.remove() else ShellStrings.clear(),
            cancelLabel = ShellStrings.cancel(),
            onConfirm = {
                if (isBulkRemoval) vm.removeSelectedHistory() else vm.clearHistory()
                confirmHistoryRemoval = false
                confirmHistoryClear = false
            },
            onDismiss = {
                confirmHistoryRemoval = false
                confirmHistoryClear = false
            },
        )
    }
    deleteStreamTarget?.let { stream ->
        VLCConfirmActionDialog(
            title = ShellStrings.deleteStream(),
            message = stringResource(Res.string.remove_stream_message),
            target = stream.displayTitle,
            confirmLabel = ShellStrings.delete(),
            cancelLabel = ShellStrings.cancel(),
            onConfirm = { vm.deleteStream(stream.id); deleteStreamTarget = null },
            onDismiss = { deleteStreamTarget = null },
        )
    }
}

private data class MoreHubAction(
    val icon: MaterialIcon,
    val label: String,
    val summary: String,
    val onClick: () -> Unit,
)

private fun moreActionPosition(index: Int, size: Int): VLCListItemPosition = when {
    size <= 1 -> VLCListItemPosition.Single
    index == 0 -> VLCListItemPosition.First
    index == size - 1 -> VLCListItemPosition.Last
    else -> VLCListItemPosition.Middle
}

@Composable
private fun MoreAction(
    icon: MaterialIcon,
    label: String,
    summary: String,
    onClick: () -> Unit,
    position: VLCListItemPosition,
) {
    VLCNavigationRow(
        title = label,
        summary = summary,
        position = position,
        onClick = onClick,
    ) { tint ->
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun MoreEmptySection(
    text: String,
    symbol: MaterialIcon,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 80.dp).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(symbol, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoreSectionTitle(title)
        trailingContent()
    }
}

@Composable
private fun MoreSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
