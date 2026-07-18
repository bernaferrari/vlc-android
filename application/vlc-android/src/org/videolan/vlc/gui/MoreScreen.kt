/*
 * ************************************************************************
 *  MoreScreen.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 */

package org.videolan.vlc.gui

import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.videolan.libvlc.Dialog
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.ACTIVITY_RESULT_PREFERENCES
import org.videolan.resources.TAG_ITEM
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.resources.REMOTE_ACCESS_CLIENT_ACTIVITY
import org.videolan.resources.SHARED_APP_ACTIVITY
import org.videolan.tools.HotPlaybackSettings
import org.videolan.tools.Settings
import org.videolan.tools.copy
import org.videolan.tools.retrieveParent
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.gui.browser.KEY_JUMP_TO
import org.videolan.vlc.gui.browser.KEY_MEDIA
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.FloatingActionButtonBehavior
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.showDonations
import org.videolan.vlc.gui.preferences.PreferencesActivity
import org.videolan.vlc.gui.video.VideoPlayerActivity
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_SHORTCUT
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_COPY
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.DialogDelegate
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.util.IDialogManager
import org.videolan.vlc.util.isOTG
import org.videolan.vlc.util.isSD
import org.videolan.vlc.util.isSchemeNetwork
import org.videolan.vlc.util.showVlcDialog
import org.videolan.vlc.viewmodels.HistoryModel
import org.videolan.vlc.viewmodels.StreamsModel

class MoreScreenController(private val activity: MainActivity) : CtxActionReceiver, IDialogManager {

    private val settings = Settings.getInstance(activity)
    private val dialogsDelegate = DialogDelegate()
    private val historyModel = ViewModelProvider(activity, HistoryModel.Factory(activity))[HistoryModel::class.java]
    private val streamsModel = ViewModelProvider(activity, StreamsModel.Factory(activity, showDummy = true))[StreamsModel::class.java]

    private var historyItems by mutableStateOf<List<MediaWrapper>>(emptyList())
    private var streams by mutableStateOf<List<MediaWrapper>>(emptyList())
    private var historyLoading by mutableStateOf(false)
    private var streamsLoading by mutableStateOf(false)
    private var selectedHistoryPositions by mutableStateOf<Set<Int>>(emptySet())
    private var playbackHistoryEnabled by mutableStateOf(HotPlaybackSettings.playbackHistory)
    private var historyActionMode: ActionMode? = null

    init {
        dialogsDelegate.observeDialogs(activity, this)
        historyModel.dataset.observe(activity) { items ->
            historyItems = items.orEmpty()
            selectedHistoryPositions = selectedHistoryPositions.filter { it < historyItems.size }.toSet()
            historyActionMode?.invalidate()
        }
        historyModel.loading.observe(activity) { loading ->
            historyLoading = loading == true
            activity.refreshing = historyLoading || streamsLoading
        }
        streamsModel.dataset.observe(activity) { items ->
            streams = items.orEmpty()
        }
        streamsModel.loading.observe(activity) { loading ->
            streamsLoading = loading == true
            activity.refreshing = historyLoading || streamsLoading
        }
    }

    fun onVisible() {
        playbackHistoryEnabled = HotPlaybackSettings.playbackHistory
        activity.title = activity.getString(R.string.more)
        hideFloatingActionButtons()
        activity.setTabLayoutVisibility(false)
        activity.invalidateOptionsMenu()
        refresh()
    }

    fun onHidden() {
        historyActionMode?.finish()
    }

    fun refresh() {
        playbackHistoryEnabled = HotPlaybackSettings.playbackHistory
        historyModel.refresh()
        streamsModel.refresh()
    }

    private fun hideFloatingActionButtons() {
        listOf(R.id.fab, R.id.fab_large).forEach { id ->
            val fab = activity.findViewById<FloatingActionButton?>(id) ?: return@forEach
            ((fab.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? FloatingActionButtonBehavior)?.shouldNeverShow = true
            fab.hide()
        }
    }

    @Composable
    fun Content() {
        MoreScreenContent(
            historyItems = historyItems,
            streams = streams,
            historyLoading = historyLoading,
            streamsLoading = streamsLoading,
            selectedHistoryPositions = selectedHistoryPositions,
            playbackHistoryEnabled = playbackHistoryEnabled,
            onSettingsClicked = {
                activity.startActivityForResult(Intent(activity, PreferencesActivity::class.java), ACTIVITY_RESULT_PREFERENCES)
            },
            onRemoteClientClicked = {
                activity.startActivity(Intent().setClassName(activity, REMOTE_ACCESS_CLIENT_ACTIVITY))
            },
            onSharedAppClicked = {
                activity.startActivity(Intent().setClassName(activity, SHARED_APP_ACTIVITY))
            },
            onAboutClicked = {
                activity.startActivity(Intent(activity, AboutActivity::class.java))
            },
            onDonateClicked = {
                activity.showDonations()
            },
            onOpenStreams = ::openStreamsScreen,
            onOpenHistory = ::openHistoryScreen,
            onStreamClicked = ::playStream,
            onStreamLongClicked = ::showContext,
            onStreamMoreClicked = ::showContext,
            onHistoryClicked = ::onHistoryItemClicked,
            onHistoryLongClicked = ::onHistoryItemLongClicked
        )
    }

    private fun openStreamsScreen() {
        activity.startActivityForResult(
            Intent(activity, SecondaryActivity::class.java).putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.STREAMS),
            SecondaryActivity.ACTIVITY_RESULT_SECONDARY
        )
    }

    private fun openHistoryScreen() {
        activity.startActivityForResult(
            Intent(activity, SecondaryActivity::class.java).putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.HISTORY),
            SecondaryActivity.ACTIVITY_RESULT_SECONDARY
        )
    }

    private fun playStream(media: MediaWrapper) {
        if (media.id < 0) {
            openStreamsScreen()
            return
        }
        media.type = MediaWrapper.TYPE_STREAM
        if (media.uri.scheme?.startsWith("rtsp") == true) VideoPlayerActivity.start(activity, media.uri)
        else MediaUtils.openMedia(activity, media)
        activity.invalidateOptionsMenu()
    }

    private fun showContext(position: Int) {
        val media = streams.getOrNull(position) ?: return
        if (media.id < 0) {
            openStreamsScreen()
            return
        }
        val flags = FlagSet(ContextOption.entries.toList()).apply {
            addAll(CTX_ADD_SHORTCUT, CTX_ADD_TO_PLAYLIST, CTX_APPEND, CTX_COPY, CTX_DELETE, CTX_RENAME)
        }
        showContext(activity, this, position, media, flags)
    }

    override fun onCtxAction(position: Int, option: ContextOption) {
        val media = streams.getOrNull(position) ?: return
        when (option) {
            CTX_RENAME -> activity.showRenameComposeDialog(media) { renamedMedia, name ->
                streamsModel.rename(renamedMedia as MediaWrapper, name)
            }
            CTX_APPEND -> MediaUtils.appendMedia(activity, media)
            CTX_ADD_TO_PLAYLIST -> activity.addToPlaylist(media.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            CTX_COPY -> {
                activity.copy(media.title, media.location)
                Snackbar.make(activity.window.decorView.findViewById(android.R.id.content), R.string.url_copied_to_clipboard, Snackbar.LENGTH_LONG).show()
            }
            CTX_DELETE -> {
                streamsModel.deletingMedia = media
                UiTools.snackerWithCancel(activity, activity.getString(R.string.stream_deleted), action = { streamsModel.delete() }) {
                    streamsModel.deletingMedia = null
                    streamsModel.refresh()
                }
                streamsModel.refresh()
            }
            CTX_ADD_SHORTCUT -> activity.lifecycleScope.launch { activity.createShortcut(media) }
            else -> {}
        }
    }

    private fun onHistoryItemClicked(position: Int, item: MediaWrapper) {
        if (org.videolan.tools.KeyHelper.isShiftPressed && historyActionMode == null) {
            onHistoryItemLongClicked(position, item)
            return
        }
        if (historyActionMode != null) {
            toggleHistorySelection(position)
            return
        }
        if (position != 0) historyModel.moveUp(item)
        MediaUtils.openMedia(activity, item)
    }

    private fun onHistoryItemLongClicked(position: Int, @Suppress("UNUSED_PARAMETER") item: MediaWrapper) {
        toggleHistorySelection(position, forceSelected = true)
    }

    private fun toggleHistorySelection(position: Int, forceSelected: Boolean = false) {
        val selection = selectedHistoryPositions.toMutableSet()
        if (forceSelected) selection.add(position) else if (!selection.add(position)) selection.remove(position)
        selectedHistoryPositions = selection
        if (selection.isNotEmpty() && historyActionMode == null) startHistoryActionMode()
        if (selection.isEmpty()) historyActionMode?.finish() else historyActionMode?.invalidate()
    }

    private fun startHistoryActionMode() {
        historyActionMode = activity.startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                UiTools.addHistoryActionModeMenu(menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val selection = selectedHistoryItems()
                if (selection.isEmpty()) {
                    mode.finish()
                    return false
                }
                mode.title = activity.getString(R.string.selection_count, selection.size)
                menu.findItem(R.id.action_history_info).isVisible = selection.size == 1
                menu.findItem(R.id.action_history_append).isVisible = PlaylistManager.hasMedia()
                menu.findItem(R.id.action_go_to_folder).isVisible = selection.size == 1 && selection.first().uri.retrieveParent() != null
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val selection = selectedHistoryItems()
                if (selection.isNotEmpty()) {
                    when (item.itemId) {
                        R.id.action_history_play -> MediaUtils.openList(activity, selection, 0)
                        R.id.action_history_append -> MediaUtils.appendMedia(activity, selection)
                        R.id.action_history_info -> showHistoryInfo(selection.first())
                        R.id.action_go_to_folder -> showHistoryParentFolder(selection.first())
                        else -> {
                            mode.finish()
                            return false
                        }
                    }
                }
                mode.finish()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                historyActionMode = null
                selectedHistoryPositions = emptySet()
            }
        })
    }

    private fun selectedHistoryItems() = selectedHistoryPositions.sorted().mapNotNull { historyItems.getOrNull(it) }

    private fun showHistoryInfo(media: MediaWrapper) {
        activity.startActivity(Intent(activity, InfoActivity::class.java).putExtra(TAG_ITEM, media))
    }

    private fun showHistoryParentFolder(media: MediaWrapper) {
        val parent = MLServiceLocator.getAbstractMediaWrapper(media.uri.retrieveParent()).apply {
            type = MediaWrapper.TYPE_DIR
        }
        activity.startActivity(Intent(activity, SecondaryActivity::class.java).apply {
            putExtra(KEY_MEDIA, parent)
            putExtra(KEY_JUMP_TO, media)
            putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.FILE_BROWSER)
        })
    }

    override fun fireDialog(dialog: Dialog) {
        activity.showVlcDialog(dialog)
    }

    override fun dialogCanceled(dialog: Dialog?) = Unit
}

@Composable
private fun MoreScreenContent(
    historyItems: List<MediaWrapper>,
    streams: List<MediaWrapper>,
    historyLoading: Boolean,
    streamsLoading: Boolean,
    selectedHistoryPositions: Set<Int>,
    playbackHistoryEnabled: Boolean,
    onSettingsClicked: () -> Unit,
    onRemoteClientClicked: () -> Unit,
    onSharedAppClicked: () -> Unit,
    onAboutClicked: () -> Unit,
    onDonateClicked: () -> Unit,
    onOpenStreams: () -> Unit,
    onOpenHistory: () -> Unit,
    onStreamClicked: (MediaWrapper) -> Unit,
    onStreamLongClicked: (Int) -> Unit,
    onStreamMoreClicked: (Int) -> Unit,
    onHistoryClicked: (Int, MediaWrapper) -> Unit,
    onHistoryLongClicked: (Int, MediaWrapper) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val showDonationCard = false
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MoreTopButton(
                        text = stringResource(R.string.preferences),
                        icon = R.drawable.ic_more_preferences,
                        onClick = onSettingsClicked,
                        modifier = Modifier.weight(1f)
                    )
                    MoreTopButton(
                        text = stringResource(R.string.about),
                        icon = R.drawable.ic_more_about,
                        onClick = onAboutClicked,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                MoreTopButton(
                    text = stringResource(R.string.remote_access_client_entry),
                    icon = R.drawable.ic_more_stream,
                    onClick = onRemoteClientClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                MoreTopButton(
                    text = stringResource(R.string.shared_app_entry),
                    icon = R.drawable.ic_more_preferences,
                    onClick = onSharedAppClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showDonationCard) item {
                MoreDonationCard(onClick = onDonateClicked)
            }
            item {
                MoreSection(
                    title = stringResource(R.string.streams),
                    icon = R.drawable.ic_more_stream,
                    loading = streamsLoading,
                    empty = streams.isEmpty(),
                    onOpen = onOpenStreams
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(streams, key = { index, item -> "${item.id}:${item.uri}:$index" }) { index, stream ->
                            StreamCard(
                                stream = stream,
                                onClick = { onStreamClicked(stream) },
                                onLongClick = { onStreamLongClicked(index) },
                                onMoreClick = { onStreamMoreClicked(index) }
                            )
                        }
                    }
                }
            }
            if (playbackHistoryEnabled && (historyItems.isNotEmpty() || historyLoading)) {
                item {
                    MoreSection(
                        title = stringResource(R.string.history),
                        icon = R.drawable.ic_history,
                        loading = historyLoading,
                        empty = historyItems.isEmpty(),
                        onOpen = onOpenHistory
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(historyItems, key = { index, item -> "${item.id}:${item.uri}:$index" }) { index, media ->
                                HistoryCard(
                                    media = media,
                                    selected = index in selectedHistoryPositions,
                                    onClick = { onHistoryClicked(index, media) },
                                    onLongClick = { onHistoryLongClicked(index, media) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreTopButton(text: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VLCThemeDefaults.colors
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = colors.fontDefault,
        modifier = modifier
            .height(56.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoreDonationCard(onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_donate),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.donate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.donate_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MoreSection(
    title: String,
    icon: Int,
    loading: Boolean,
    empty: Boolean,
    onOpen: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onOpen)
                .padding(vertical = 4.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpen) {
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = stringResource(R.string.talkback_enter_screen, title),
                    tint = colors.fontDefault
                )
            }
        }
        when {
            loading && empty -> {
                Box(modifier = Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            empty -> Unit
            else -> content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StreamCard(
    stream: MediaWrapper,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val isDummy = stream.id < 0
    Column(
        modifier = Modifier
            .width(184.dp)
            .height(132.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(if (isDummy) R.drawable.ic_stream_add else R.drawable.ic_stream_big),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDummy) stringResource(R.string.new_stream) else decode(stream.title),
                color = colors.listTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!isDummy) {
                IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = stringResource(R.string.more_actions),
                        tint = colors.fontDefault
                    )
                }
            }
        }
        if (!isDummy) {
            Text(
                text = decode(stream.location),
                color = colors.listSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    media: MediaWrapper,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val enabledAlpha = if (media.isPresent) 1f else 0.48f
    Column(
        modifier = Modifier
            .width(156.dp)
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) colors.subtleSelection else MaterialTheme.colorScheme.surfaceContainer)
            .then(if (selected) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.large) else Modifier)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp)
    ) {
        HistoryArt(
            media = media,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (media.type == MediaWrapper.TYPE_VIDEO) 16f / 10f else 1f)
                .alpha(enabledAlpha)
        )
        Text(
            text = media.title.orEmpty(),
            color = colors.listTitle,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp).alpha(enabledAlpha)
        )
        if (!media.description.isNullOrEmpty()) {
            Text(
                text = media.description.orEmpty(),
                color = colors.listSubtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(enabledAlpha)
            )
        }
    }
}

@Composable
private fun HistoryArt(media: MediaWrapper, modifier: Modifier = Modifier) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(colors.backgroundDefaultDarker),
        contentAlignment = Alignment.Center
    ) {
        VlcMediaImage(
            item = media,
            width = 156.dp,
            fallbackPainter = painterResource(if (media.type == MediaWrapper.TYPE_VIDEO) R.drawable.ic_video_big else R.drawable.ic_song_big),
            fallbackModifier = Modifier.size(38.dp),
            fallbackColorFilter = ColorFilter.tint(colors.fontLight),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        HistoryBadge(media = media, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun HistoryBadge(media: MediaWrapper, modifier: Modifier = Modifier) {
    val icon = when {
        !media.isPresent -> R.drawable.ic_emoji_absent
        media.uri.scheme.isSchemeNetwork() -> R.drawable.ic_emoji_network
        media.uri.isSD() -> R.drawable.ic_emoji_sd
        media.uri.isOTG() -> R.drawable.ic_emoji_otg
        else -> null
    } ?: return
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
            .padding(3.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun decode(value: String?): String = value?.let { Uri.decode(it) }.orEmpty()
