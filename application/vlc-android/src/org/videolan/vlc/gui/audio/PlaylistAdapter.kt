/*
 * *************************************************************************
 *  PlaylistAdapter.java
 * **************************************************************************
 *  Copyright © 2015-2017 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.audio

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
import org.videolan.vlc.R
import org.videolan.vlc.gui.DiffUtilAdapter
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.isTablet
import org.videolan.vlc.gui.view.PlaylistItemView
import org.videolan.vlc.interfaces.SwipeDragHelperAdapter
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.LifecycleAwareScheduler
import org.videolan.vlc.util.MediaItemDiffCallback
import org.videolan.vlc.util.SchedulerCallback
import org.videolan.vlc.viewmodels.PlaylistModel
import java.util.Collections

private const val ACTION_MOVE = "action_move"
private const val ACTION_MOVED = "action_moved"

class PlaylistAdapter(private val player: IPlayer) : DiffUtilAdapter<MediaWrapper, PlaylistAdapter.ViewHolder>(), SwipeDragHelperAdapter, SchedulerCallback {

    var showTrackNumbers: Boolean = false
    var showReorderButtons: Boolean = true
    private var model: PlaylistModel? = null
    private var currentPlayingItem: PlaylistItemView? = null
    lateinit var scheduler: LifecycleAwareScheduler
    var stopAfter: Int = -1
        set(value) {
            val old = field
            field = value
            if (old in 0 until itemCount) notifyItemChanged(old)
            if (value in 0 until itemCount) notifyItemChanged(value)

        }

    init {
        scheduler =  LifecycleAwareScheduler(this)
    }

    var currentIndex = 0
        set(position) {
            if (position >= itemCount) return
            val former = currentIndex
            field = position
            if (former >= 0) notifyItemChanged(former)
            if (position >= 0) {
                notifyItemChanged(position)
                player.onSelectionSet(position)
            }
        }

    interface IPlayer {
        fun onPopupMenu(view: View, position: Int, item: MediaWrapper?)
        fun onSelectionSet(position: Int)
        fun playItem(position: Int, item: MediaWrapper)
        fun getLifeCycle(): Lifecycle
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.playlist_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val media = getItem(position)
        val tablet = holder.playlistItem.context.isTablet() || AndroidDevices.isTv
        val current = currentIndex == position
        holder.playlistItem.bind(
            media = media,
            subtitle = MediaUtils.getMediaSubtitle(media),
            showTrackNumbers = showTrackNumbers,
            showReorderButtons = showReorderButtons && tablet,
            showDeleteButton = tablet,
            stopAfterThis = stopAfter == position,
            current = current,
            playing = model?.playing != false
        )
        if (current) currentPlayingItem = holder.playlistItem
        else if (currentPlayingItem === holder.playlistItem) currentPlayingItem = null
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        currentPlayingItem?.setPlaying(false)
        currentPlayingItem = null
    }

    override fun onViewRecycled(holder: ViewHolder) {
        if (currentPlayingItem === holder.playlistItem) currentPlayingItem = null
        holder.playlistItem.setPlaying(false)
        super.onViewRecycled(holder)
    }

    override fun getItemCount() = dataset.size

    @MainThread
    override fun getItem(position: Int) = dataset[position]

    override fun onUpdateFinished() {
        model?.run { currentIndex = selection }
    }

    @MainThread
    fun remove(position: Int) {
        model?.run { remove(position) }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(dataset, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        scheduler.startAction(ACTION_MOVE, bundleOf("from" to fromPosition, "to" to toPosition))
    }

    override fun onItemMoved(dragFrom: Int, dragTo: Int) {
    }

    override fun onItemDismiss(position: Int) {
        model?.let {
            val media = getItem(position)
            val message = String.format(AppContextProvider.appResources.getString(R.string.remove_playlist_item), media.title)
            val originalPosition = it.getOriginalPosition(position)
            if (player is Fragment) {
                UiTools.snackerWithCancel(player.requireActivity(), message, overAudioPlayer = true, action = {}) {
                    model?.run { insertMedia(originalPosition, media) }
                }
            } else if (player is Activity) {
                UiTools.snackerWithCancel(player, message, action = {}) {
                    model?.run { insertMedia(originalPosition, media) }
                }
            }
            remove(position)
        }
    }

    fun setModel(model: PlaylistModel) {
        this.model = model
    }

    inner class ViewHolder @TargetApi(Build.VERSION_CODES.M)
    constructor(v: View) : RecyclerView.ViewHolder(v) {
        val playlistItem = v as PlaylistItemView

        init {
            playlistItem.setCallbacks(
                onRowClick = { onClick() },
                onMoveUpClick = { onMoveUpClick() },
                onMoveDownClick = { onMoveDownClick() },
                onDeleteClick = { onDeleteClick() },
                onMoreClick = { onMoreClick(playlistItem) }
            )
            if (AndroidUtil.isMarshMallowOrLater)
                itemView.setOnContextClickListener { view ->
                    onMoreClick(view)
                    true
                }
        }

        fun onClick() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) player.playItem(position, getItem(position))
        }

        fun onMoreClick(v: View) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) player.onPopupMenu(v, position, getItem(position))
        }

        fun onDeleteClick() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onItemDismiss(position)
        }

        fun onMoveUpClick() {
            val position = bindingAdapterPosition
            if (position > 0) onItemMove(position, position - 1)
        }

        fun onMoveDownClick() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION && position != itemCount - 1) onItemMove(position, position + 1)
        }
    }

    override fun createCB(): DiffCallback<MediaWrapper> = MediaItemDiffCallback()

    fun setCurrentlyPlaying(playing: Boolean) {
        currentPlayingItem?.setPlaying(playing)
    }

    var from = -1
    var to = -1
    override fun onTaskTriggered(id: String, data: Bundle) {
        when (id) {
            ACTION_MOVE -> {
                scheduler.cancelAction(ACTION_MOVED)
                if (from == -1) from = data.getInt("from")
                to = data.getInt("to")
                scheduler.scheduleAction(ACTION_MOVED, 1000)
            }
            ACTION_MOVED -> {
                val model = model ?: return
                if (to > from) ++to
                model.move(from, to)
                to = -1
                from = to
            }
        }
    }

    override val lifecycle: Lifecycle
        get() = player.getLifeCycle()
}
