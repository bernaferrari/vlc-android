/*****************************************************************************
 * HistoryAdapter.java
 *
 * Copyright © 2012-2015 VLC authors and VideoLAN
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
 */
package org.videolan.vlc.gui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.UPDATE_SELECTION
import org.videolan.tools.MultiSelectAdapter
import org.videolan.tools.MultiSelectHelper
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBrowserItemCard
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.*
import org.videolan.vlc.interfaces.IListEventsHandler
import org.videolan.vlc.interfaces.SwipeDragHelperAdapter
import org.videolan.vlc.util.isOTG
import org.videolan.vlc.util.isSD
import org.videolan.vlc.util.isSchemeNetwork


class HistoryAdapter(private val inCards: Boolean = false, private val listEventsHandler: IListEventsHandler? = null) : DiffUtilAdapter<MediaWrapper, HistoryAdapter.ViewHolder>(),
        MultiSelectAdapter<MediaWrapper>, IEventsSource<Click> by EventsSource(), SwipeDragHelperAdapter {

    val updateEvt : LiveData<Unit> = MutableLiveData()
    var multiSelectHelper: MultiSelectHelper<MediaWrapper> = MultiSelectHelper(this, UPDATE_SELECTION)

    inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

        fun bind(media: MediaWrapper, selected: Boolean) {
            val contentDescription = historyContentDescription(composeView.context, media)
            composeView.setContent {
                if (inCards) {
                    VLCBrowserItemCard(
                        title = media.title.orEmpty(),
                        subtitle = media.description,
                        modifier = Modifier.widthIn(min = 160.dp),
                        selected = selected,
                        contentDescription = contentDescription,
                        titleMaxLines = 1,
                        onClick = ::onClick,
                        onLongClick = ::onLongClick,
                        artworkContent = {
                            HistoryArtwork(
                                media = media,
                                large = true,
                                onImageClick = ::onImageClick
                            )
                        }
                    )
                } else {
                    VLCBrowserItemRow(
                        title = media.title.orEmpty(),
                        subtitle = media.description,
                        selected = selected,
                        contentDescription = contentDescription,
                        titleMaxLines = 1,
                        onClick = ::onClick,
                        onLongClick = ::onLongClick,
                        artworkContent = {
                            HistoryArtwork(
                                media = media,
                                onImageClick = ::onImageClick
                            )
                        }
                    )
                }
            }
        }

        private fun onClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let {
                eventsChannel.trySend(SimpleClick(it))
            }
        }

        private fun onLongClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let {
                eventsChannel.trySend(LongClick(it))
            }
        }

        private fun onImageClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let {
                eventsChannel.trySend(if (inCards) SimpleClick(it) else ImageClick(it))
            }
        }

        fun onClick(@Suppress("UNUSED_PARAMETER") v: View) {
            onClick()
        }

        fun onLongClick(@Suppress("UNUSED_PARAMETER") v: View): Boolean {
            onLongClick()
            return true
        }

        fun onImageClick(@Suppress("UNUSED_PARAMETER") v: View) {
            onImageClick()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ComposeView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    if (inCards) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val media = getItem(position)
        holder.bind(media, multiSelectHelper.isSelected(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNullOrEmpty())
            onBindViewHolder(holder, position)
        else
            holder.bind(getItem(position), multiSelectHelper.isSelected(position))
    }

    override fun getItemId(arg0: Int): Long {
        return 0
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onUpdateFinished() {
        (updateEvt as MutableLiveData).value = Unit
    }

    companion object {

        const val TAG = "VLC/HistoryAdapter"
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {    }

    override fun onItemDismiss(position: Int) {
        val item = getItem(position)
        listEventsHandler?.onRemove(position, item)
    }

    override fun onItemMoved(dragFrom: Int, dragTo: Int) {    }

    override fun createCB(): DiffCallback<MediaWrapper> = object : DiffCallback<MediaWrapper>() {
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition].title == newList[newItemPosition].title &&
                        oldList[oldItemPosition].description == newList[newItemPosition].description
    }
}

@Composable
private fun BoxScope.HistoryArtwork(
    media: MediaWrapper,
    large: Boolean = false,
    onImageClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val enabledAlpha = if (media.isPresent) 1f else 0.45f
    Box(
        modifier = Modifier
            .size(if (large) 48.dp else 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundDefaultDarker)
            .clickable(onClick = onImageClick)
            .alpha(enabledAlpha),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(if (media.type == MediaWrapper.TYPE_VIDEO) R.drawable.ic_video_big else R.drawable.ic_song_big),
            contentDescription = null,
            tint = colors.fontLight,
            modifier = Modifier.size(if (large) 34.dp else 30.dp)
        )
    }
    HistoryBadge(media = media, modifier = Modifier.align(Alignment.BottomStart))
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
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(3.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun historyContentDescription(context: Context, media: MediaWrapper): String {
    return when (media.type) {
        MediaWrapper.TYPE_VIDEO -> TalkbackUtil.getVideo(context, media)
        MediaWrapper.TYPE_AUDIO -> TalkbackUtil.getAudioTrack(context, media)
        MediaWrapper.TYPE_STREAM -> TalkbackUtil.getStream(context, media)
        MediaWrapper.TYPE_DIR, MediaWrapper.TYPE_SUBTITLE, MediaWrapper.TYPE_PLAYLIST -> TalkbackUtil.getDir(context, media, false)
        MediaWrapper.TYPE_ALL -> TalkbackUtil.getAll(media)
        else -> media.title
    }
}
