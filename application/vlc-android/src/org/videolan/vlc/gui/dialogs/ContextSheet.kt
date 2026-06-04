/*****************************************************************************
 * ContextSheet.kt
 *****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
 *
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
 *****************************************************************************/

package org.videolan.vlc.gui.dialogs

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.tools.isStarted
import org.videolan.vlc.R
import org.videolan.vlc.VlcMigrationHelper
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.ContextOption.CTX_ADD_FOLDER_AND_SUB_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_ADD_FOLDER_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_ADD_GROUP
import org.videolan.vlc.util.ContextOption.CTX_ADD_SCANNED
import org.videolan.vlc.util.ContextOption.CTX_ADD_SHORTCUT
import org.videolan.vlc.util.ContextOption.CTX_ADD_TO_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_APPEND
import org.videolan.vlc.util.ContextOption.CTX_BAN_FOLDER
import org.videolan.vlc.util.ContextOption.CTX_COPY
import org.videolan.vlc.util.ContextOption.CTX_CUSTOM_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_DELETE
import org.videolan.vlc.util.ContextOption.CTX_DOWNLOAD_SUBTITLES
import org.videolan.vlc.util.ContextOption.CTX_FAV_ADD
import org.videolan.vlc.util.ContextOption.CTX_FAV_EDIT
import org.videolan.vlc.util.ContextOption.CTX_FAV_REMOVE
import org.videolan.vlc.util.ContextOption.CTX_FIND_METADATA
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ALBUM
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ALBUM_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_ARTIST
import org.videolan.vlc.util.ContextOption.CTX_GO_TO_FOLDER
import org.videolan.vlc.util.ContextOption.CTX_GROUP_SIMILAR
import org.videolan.vlc.util.ContextOption.CTX_INFORMATION
import org.videolan.vlc.util.ContextOption.CTX_MARK_ALL_AS_PLAYED
import org.videolan.vlc.util.ContextOption.CTX_MARK_ALL_AS_UNPLAYED
import org.videolan.vlc.util.ContextOption.CTX_MARK_AS_PLAYED
import org.videolan.vlc.util.ContextOption.CTX_MARK_AS_UNPLAYED
import org.videolan.vlc.util.ContextOption.CTX_PLAY
import org.videolan.vlc.util.ContextOption.CTX_PLAY_ALL
import org.videolan.vlc.util.ContextOption.CTX_PLAY_AS_AUDIO
import org.videolan.vlc.util.ContextOption.CTX_PLAY_FROM_START
import org.videolan.vlc.util.ContextOption.CTX_PLAY_NEXT
import org.videolan.vlc.util.ContextOption.CTX_PLAY_SHUFFLE
import org.videolan.vlc.util.ContextOption.CTX_QUICK_PLAY
import org.videolan.vlc.util.ContextOption.CTX_REMOVE_FROM_PLAYLIST
import org.videolan.vlc.util.ContextOption.CTX_REMOVE_GROUP
import org.videolan.vlc.util.ContextOption.CTX_RENAME
import org.videolan.vlc.util.ContextOption.CTX_RENAME_GROUP
import org.videolan.vlc.util.ContextOption.CTX_SET_RINGTONE
import org.videolan.vlc.util.ContextOption.CTX_SHARE
import org.videolan.vlc.util.ContextOption.CTX_STOP_AFTER_THIS
import org.videolan.vlc.util.ContextOption.CTX_UNGROUP
import org.videolan.vlc.util.FlagSet
import org.videolan.vlc.util.isTalkbackIsEnabled

const val CTX_TITLE_KEY = "CTX_TITLE_KEY"
const val CTX_POSITION_KEY = "CTX_POSITION_KEY"
const val CTX_FLAGS_KEY = "CTX_FLAGS_KEY"
const val CTX_MEDIA_KEY = "CTX_MEDIA_KEY"

private class ContextSheetComposeDialog(
    private val activity: ComponentActivity,
    private val receiver: CtxActionReceiver,
    private val position: Int,
    private val title: String,
    private val media: MediaLibraryItem?,
    private val menuItems: List<CtxMenuItem>
) {
    private val dialog = if (Settings.showTvUi) {
        BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
    } else {
        BottomSheetDialog(activity)
    }
    private var rootView: ComposeView? = null

    fun show() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    ContextSheetContent(
                        title = title,
                        media = media,
                        menuItems = menuItems,
                        onItemSelected = ::onItemSelected
                    )
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnShowListener { configureBottomSheet() }
        dialog.setOnDismissListener { rootView = null }
        dialog.show()
    }

    private fun onItemSelected(item: CtxMenuItem) {
        receiver.onCtxAction(position, item.id)
        dialog.dismiss()
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
        }
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
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (AndroidDevices.isChromeBook) behavior.isDraggable = false
        }
    }
}

@Composable
private fun ContextSheetContent(
    title: String,
    media: MediaLibraryItem?,
    menuItems: List<CtxMenuItem>,
    onItemSelected: (CtxMenuItem) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ContextSheetHeader(
                    title = title,
                    media = media
                )
            }
            items(menuItems) { item ->
                ContextActionRow(
                    item = item,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun ContextSheetHeader(
    title: String,
    media: MediaLibraryItem?
) {
    val coverMedia = media?.takeIf { it.contextArtwork?.isNotBlank() == true }
    if (coverMedia != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 56.dp)
        ) {
            ContextCover(media = coverMedia)
            Text(
                text = title,
                color = VLCThemeDefaults.colors.fontDefault,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                color = VLCThemeDefaults.colors.fontDefault,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContextCover(media: MediaLibraryItem) {
    val colors = VLCThemeDefaults.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundDefaultDarker)
    ) {
        VlcMediaImage(
            item = media,
            width = 48.dp,
            fallbackPainter = painterResource(contextCoverIcon(media)),
            fallbackModifier = Modifier.size(32.dp),
            fallbackColorFilter = ColorFilter.tint(colors.fontDefault),
            contentScale = ContentScale.Crop,
            fallbackContentScale = ContentScale.Fit,
            reloadKey = media.contextArtwork,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun ContextActionRow(
    item: CtxMenuItem,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        if (item.icon != 0) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                tint = if (focused) colors.primary else colors.fontDefault,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
        Text(
            text = item.title,
            color = if (focused) colors.primary else colors.listTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private val MediaLibraryItem.contextArtwork: String?
    get() = when (this) {
        is MediaWrapper -> artworkURL
        else -> artworkMrl
    }

@DrawableRes
private fun contextCoverIcon(media: MediaLibraryItem): Int {
    return when (media) {
        is MediaWrapper -> when (media.type) {
            MediaWrapper.TYPE_VIDEO -> R.drawable.ic_video
            MediaWrapper.TYPE_DIR -> R.drawable.ic_folder
            else -> R.drawable.ic_playlist_audio
        }
        else -> R.drawable.ic_no_media
    }
}

private fun populateMenuItems(activity: ComponentActivity, flags: FlagSet<ContextOption>) = mutableListOf<CtxMenuItem>().apply {

    if (flags.contains(CTX_PLAY)) add(Simple(CTX_PLAY, activity.getString(R.string.play), R.drawable.ic_play))
    if (flags.contains(CTX_QUICK_PLAY)) add(Simple(CTX_QUICK_PLAY, activity.getString(R.string.quick_play), R.drawable.ic_play))
    if (flags.contains(CTX_PLAY_SHUFFLE)) add(Simple(CTX_PLAY_SHUFFLE, activity.getString(R.string.shuffle_play), R.drawable.ic_shuffle))
    if (flags.contains(CTX_PLAY_FROM_START)) add(Simple(CTX_PLAY_FROM_START, activity.getString(R.string.play_from_start), R.drawable.ic_play_from_start))
    if (flags.contains(CTX_PLAY_ALL)) add(Simple(CTX_PLAY_ALL, activity.getString(R.string.play_all), R.drawable.ic_play_all))
    if (flags.contains(CTX_PLAY_AS_AUDIO)) add(Simple(CTX_PLAY_AS_AUDIO, activity.getString(R.string.play_as_audio), R.drawable.ic_play_as_audio))
    if (flags.contains(CTX_APPEND)) add(Simple(CTX_APPEND, activity.getString(R.string.append), R.drawable.ic_play_append))
    if (flags.contains(CTX_PLAY_NEXT)) add(Simple(CTX_PLAY_NEXT, activity.getString(R.string.insert_next), R.drawable.ic_play_next))
    if (flags.contains(CTX_DOWNLOAD_SUBTITLES) && VlcMigrationHelper.isLolliPopOrLater) add(Simple(CTX_DOWNLOAD_SUBTITLES, activity.getString(R.string.download_subtitles), R.drawable.ic_download_subtitles))
    if (flags.contains(CTX_INFORMATION)) add(Simple(CTX_INFORMATION, activity.getString(R.string.info), R.drawable.ic_information))
    if (flags.contains(CTX_GO_TO_ALBUM)) add(Simple(CTX_GO_TO_ALBUM, activity.getString(R.string.go_to_album), R.drawable.ic_album))
    if (flags.contains(CTX_GO_TO_ARTIST)) add(Simple(CTX_GO_TO_ARTIST, activity.getString(R.string.go_to_artist), R.drawable.ic_no_artist))
    if (flags.contains(CTX_GO_TO_ALBUM_ARTIST)) add(Simple(CTX_GO_TO_ALBUM_ARTIST, activity.getString(R.string.go_to_album_artist), R.drawable.ic_no_artist))
    if (flags.contains(CTX_ADD_TO_PLAYLIST)) add(Simple(CTX_ADD_TO_PLAYLIST, activity.getString(R.string.add_to_playlist), R.drawable.ic_add_to_playlist))
    if (flags.contains(CTX_SET_RINGTONE) && AndroidDevices.isPhone) add(Simple(CTX_SET_RINGTONE, activity.getString(R.string.set_song), R.drawable.ic_set_ringtone))
    if (flags.contains(CTX_FAV_ADD)) add(Simple(CTX_FAV_ADD, activity.getString(R.string.favorites_add), R.drawable.ic_fav_add))
    if (flags.contains(CTX_ADD_SCANNED)) add(Simple(CTX_ADD_SCANNED, activity.getString(R.string.add_to_scanned), R.drawable.ic_add_to_scan))
    if (flags.contains(CTX_FAV_EDIT)) add(Simple(CTX_FAV_EDIT, activity.getString(R.string.favorites_edit), R.drawable.ic_edit))
    if (flags.contains(CTX_FAV_REMOVE)) add(Simple(CTX_FAV_REMOVE, activity.getString(R.string.favorites_remove), R.drawable.ic_fav_remove))
    if (flags.contains(CTX_REMOVE_FROM_PLAYLIST)) add(Simple(CTX_REMOVE_FROM_PLAYLIST, activity.getString(R.string.remove), R.drawable.ic_remove_from_playlist))
    if (flags.contains(CTX_STOP_AFTER_THIS)) add(Simple(CTX_STOP_AFTER_THIS, activity.getString(R.string.stop_after_this), R.drawable.ic_stop_after_this))
    if (flags.contains(CTX_RENAME)) add(Simple(CTX_RENAME, activity.getString(R.string.rename), R.drawable.ic_edit))
    if (flags.contains(CTX_COPY)) add(Simple(CTX_COPY, activity.getString(R.string.copy_to_clipboard), R.drawable.ic_link))
    if (flags.contains(CTX_DELETE)) add(Simple(CTX_DELETE, activity.getString(R.string.delete), R.drawable.ic_delete))
    if (flags.contains(CTX_SHARE)) add(Simple(CTX_SHARE, activity.getString(R.string.share), R.drawable.ic_share))
    if (flags.contains(CTX_ADD_SHORTCUT) && ShortcutManagerCompat.isRequestPinShortcutSupported(activity)) add(Simple(CTX_ADD_SHORTCUT, activity.getString(R.string.create_shortcut), R.drawable.ic_app_shortcut))
    if (flags.contains(CTX_FIND_METADATA)) add(Simple(CTX_FIND_METADATA, activity.getString(R.string.find_metadata), R.drawable.ic_delete))
    if (flags.contains(CTX_ADD_FOLDER_PLAYLIST)) add(Simple(CTX_ADD_FOLDER_PLAYLIST, activity.getString(R.string.this_folder), R.drawable.ic_add_to_playlist))
    if (flags.contains(CTX_ADD_FOLDER_AND_SUB_PLAYLIST)) add(Simple(CTX_ADD_FOLDER_AND_SUB_PLAYLIST, activity.getString(R.string.all_subfolders), R.drawable.ic_add_to_playlist))
    if (flags.contains(CTX_ADD_GROUP)) add(Simple(CTX_ADD_GROUP, activity.getString(R.string.add_to_group), R.drawable.ic_add_to_group))
    if (flags.contains(CTX_REMOVE_GROUP)) add(Simple(CTX_REMOVE_GROUP, activity.getString(R.string.remove_from_group), R.drawable.ic_remove_from_group))
    if (flags.contains(CTX_RENAME_GROUP)) add(Simple(CTX_RENAME_GROUP, activity.getString(R.string.rename_group), R.drawable.ic_edit))
    if (flags.contains(CTX_UNGROUP)) add(Simple(CTX_UNGROUP, activity.getString(R.string.ungroup), R.drawable.ic_delete))
    if (flags.contains(CTX_GROUP_SIMILAR)) add(Simple(CTX_GROUP_SIMILAR, activity.getString(R.string.group_similar), R.drawable.ic_group_auto))
    if (flags.contains(CTX_MARK_AS_PLAYED)) add(Simple(CTX_MARK_AS_PLAYED, activity.getString(R.string.mark_as_played), R.drawable.ic_mark_as_played))
    if (flags.contains(CTX_MARK_AS_UNPLAYED)) add(Simple(CTX_MARK_AS_UNPLAYED, activity.getString(R.string.mark_as_not_played), R.drawable.ic_mark_as_not_played))
    if (flags.contains(CTX_MARK_ALL_AS_PLAYED)) add(Simple(CTX_MARK_ALL_AS_PLAYED, activity.getString(R.string.mark_all_as_played), R.drawable.ic_mark_all_as_played))
    if (flags.contains(CTX_MARK_ALL_AS_UNPLAYED)) add(Simple(CTX_MARK_ALL_AS_UNPLAYED, activity.getString(R.string.mark_all_as_not_played), R.drawable.ic_mark_all_as_not_played))
    if (flags.contains(CTX_GO_TO_FOLDER)) add(Simple(CTX_GO_TO_FOLDER, activity.getString(R.string.go_to_folder), R.drawable.ic_browse_parent))
    if (flags.contains(CTX_CUSTOM_REMOVE)) add(Simple(CTX_CUSTOM_REMOVE, activity.getString(R.string.remove_custom_path), R.drawable.ic_delete))
    if (flags.contains(CTX_BAN_FOLDER)) add(Simple(CTX_BAN_FOLDER, activity.getString(R.string.group_ban_folder), R.drawable.ic_hide_source))
}

sealed class CtxMenuItem(val id: ContextOption, val title: String, @DrawableRes val icon: Int)
class Simple(id: ContextOption, title: String, @DrawableRes icon: Int = 0) : CtxMenuItem(id, title, icon)

interface CtxActionReceiver {
    fun onCtxAction(position: Int, option: ContextOption)
}

/**
 * Show the bottom sheet containing the context actions. Depending on [media] type, it generates the right title.
 *
 * @param activity the activity to use to launch the BottomSheet
 * @param receiver the `CtxActionReceiver` managing the result
 * @param position the position that the caller may need to manage the result
 * @param media the media used to display the title
 * @param flags the flags describing the actions to be displayed
 */
fun showContext(activity: ComponentActivity, receiver: CtxActionReceiver, position: Int, media: MediaLibraryItem?, flags: FlagSet<ContextOption>) {
    if (!activity.isStarted()) return
    ContextSheetComposeDialog(
        activity = activity,
        receiver = receiver,
        position = position,
        title = media?.title ?: "",
        media = media,
        menuItems = populateMenuItems(activity, flags)
    ).show()
}
