/*
 * ************************************************************************
 *  AddToGroupDialog.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 *
 *
 */

package org.videolan.vlc.gui.dialogs

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.VideoGroup
import org.videolan.medialibrary.media.DummyItem
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.resources.DUMMY_NEW_GROUP
import org.videolan.tools.AppScope
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.util.isTalkbackIsEnabled
import org.videolan.vlc.viewmodels.mobile.VideoGroupingType
import org.videolan.vlc.viewmodels.mobile.VideosViewModel
import java.util.LinkedList

object AddToGroupDialog {
    const val TAG = "VLC/AddToGroupDialog"

    const val FORBID_NEW_GROUP = "FORBID_NEW_GROUP"
}

private var isAddToGroupComposeDialogShowing = false

fun ComponentActivity.showAddToGroupComposeDialog(
    tracks: List<MediaWrapper>,
    forbidNewGroup: Boolean,
    onNewGroupRequested: (Array<MediaWrapper>) -> Unit = {}
) {
    if (isAddToGroupComposeDialogShowing) return
    isAddToGroupComposeDialogShowing = true
    lifecycleScope.launch {
        if (showPinIfNeeded()) {
            isAddToGroupComposeDialogShowing = false
            return@launch
        }
        AddToGroupComposeDialog(
            activity = this@showAddToGroupComposeDialog,
            tracks = tracks.toTypedArray(),
            forbidNewGroup = forbidNewGroup,
            onNewGroupRequested = onNewGroupRequested,
            onDismissed = { isAddToGroupComposeDialogShowing = false }
        ).show()
    }
}

private class AddToGroupComposeDialog(
    private val activity: ComponentActivity,
    private val tracks: Array<MediaWrapper>,
    private val forbidNewGroup: Boolean,
    private val onNewGroupRequested: (Array<MediaWrapper>) -> Unit,
    private val onDismissed: () -> Unit
) {
    private val medialibrary = Medialibrary.getInstance()
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private val groups = mutableStateOf<List<MediaLibraryItem>>(emptyList())
    private val isLoading = mutableStateOf(true)
    private val viewModel = VideosViewModel.Factory(activity, VideoGroupingType.NAME, null, null)
        .create(VideosViewModel::class.java)
    private var rootView: ComposeView? = null
    private val groupsObserver = Observer<List<MediaLibraryItem>?> { items ->
        groups.value = items.orEmpty()
            .filterIsInstance<VideoGroup>()
            .filter { it.mediaCount() > 1 }
            .onEach { group ->
                group.description = activity.resources.getQuantityString(
                    R.plurals.media_quantity,
                    group.tracksCount,
                    group.tracksCount
                )
            }
            .map { it as MediaLibraryItem }
            .toMutableList()
            .apply {
                if (tracks.size > 1 && !forbidNewGroup) {
                    add(0, DummyItem(DUMMY_NEW_GROUP, activity.getString(R.string.new_group), activity.getString(R.string.new_group_desc)))
                }
            }
        isLoading.value = false
    }

    fun show() {
        setupContent()
        viewModel.provider.pagedList.observe(activity, groupsObserver)
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    AddToGroupContent(
                        title = activity.getString(R.string.add_to_group),
                        emptyText = activity.getString(R.string.no_group_found),
                        groups = groups.value,
                        isLoading = isLoading.value,
                        onGroupSelected = ::onItemSelected
                    )
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            viewModel.provider.pagedList.removeObserver(groupsObserver)
            rootView = null
            onDismissed()
        }
    }

    private fun onItemSelected(item: MediaLibraryItem) {
        when (item) {
            is DummyItem -> {
                onNewGroupRequested(tracks)
                dialog.dismiss()
            }
            is VideoGroup -> addToGroup(item)
        }
    }

    private fun addToGroup(videoGroup: VideoGroup) {
        AppScope.launch(Dispatchers.IO) {
            if (tracks.isEmpty()) return@launch
            val ids = LinkedList<Long>()
            for (mediaWrapper in tracks) {
                val id = mediaWrapper.id
                if (id == 0L) {
                    var media = medialibrary.getMedia(mediaWrapper.uri)
                    if (media != null) {
                        ids.add(media.id)
                    } else {
                        media = medialibrary.addMedia(mediaWrapper.location, -1L)
                        if (media != null) ids.add(media.id)
                    }
                } else {
                    ids.add(id)
                }
            }
            ids.forEach { videoGroup.add(it) }
        }
        dialog.dismiss()
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusable = false
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusableInTouchMode = false
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
}

@Composable
private fun AddToGroupContent(
    title: String,
    emptyText: String,
    groups: List<MediaLibraryItem>,
    isLoading: Boolean,
    onGroupSelected: (MediaLibraryItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .heightIn(min = 72.dp, max = 520.dp)
            ) {
                if (!isLoading && groups.isEmpty()) {
                    Text(
                        text = emptyText,
                        color = colors.fontLight,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(groups, key = { "${it.itemType}-${it.id}-${it.title}" }) { item ->
                            AddToGroupRow(item = item, onClick = { onGroupSelected(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddToGroupRow(
    item: MediaLibraryItem,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AddToGroupThumbnail(item)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = colors.listTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    color = colors.listSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AddToGroupThumbnail(item: MediaLibraryItem) {
    val colors = VLCThemeDefaults.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (item is DummyItem) colors.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (item is DummyItem) {
            Icon(
                painter = painterResource(R.drawable.ic_add_to_group),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(28.dp)
            )
        } else {
            VlcMediaImage(
                item = item,
                width = 48.dp,
                fallbackPainter = painterResource(R.drawable.ic_group),
                fallbackModifier = Modifier.size(32.dp),
                fallbackContentScale = ContentScale.Fit,
                contentScale = ContentScale.Crop,
                reloadKey = item.title,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
