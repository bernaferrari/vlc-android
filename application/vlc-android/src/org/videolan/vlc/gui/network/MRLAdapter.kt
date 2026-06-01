/*****************************************************************************
 * MRLAdapter.java
 *
 * Copyright © 2014-2015 VLC authors and VideoLAN
 * Author: Geoffrey Métais
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
package org.videolan.vlc.gui.network

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.SendChannel
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBrowserItemCard
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.databinding.MrlDummyItemBinding
import org.videolan.vlc.databinding.MrlItemBinding
import org.videolan.vlc.gui.DiffUtilAdapter
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.enableMarqueeEffect

private const val TYPE_LIST = 0
private const val TYPE_CARD = 1
private const val TYPE_DUMMY = 2

internal class MRLAdapter(private val eventActor: SendChannel<MrlAction>, private val inCards: Boolean = false) : DiffUtilAdapter<MediaWrapper, RecyclerView.ViewHolder>() {
    private var dummyClickListener: (() -> Unit)? = null
    private val handler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CARD -> CardComposeViewHolder(
                ComposeView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            )
            TYPE_LIST -> ListViewHolder(DataBindingUtil.inflate(inflater, R.layout.mrl_item, parent, false))
            else -> DummyViewHolder(DataBindingUtil.inflate(inflater, R.layout.mrl_dummy_item, parent, false))
        }
    }

    override fun getItemViewType(position: Int) = when {
        dataset.getOrNull(position)?.id ?: 0 < 0 -> TYPE_DUMMY
        inCards -> TYPE_CARD
        else -> TYPE_LIST
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataset.getOrNull(position) ?: return
        when (holder) {
            is ListViewHolder -> {
                holder.binding.mrlItemUri.text = Uri.decode(item.location)
                holder.binding.mrlItemTitle.text = Uri.decode(item.title)
                holder.binding.item = item
            }
            is CardComposeViewHolder -> {
                holder.bind(item)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (Settings.listTitleEllipsize == 4) enableMarqueeEffect(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (Settings.listTitleEllipsize == 4) handler.removeCallbacksAndMessages(null)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (Settings.listTitleEllipsize == 4) handler.removeCallbacksAndMessages(null)
        when (holder) {
            is ListViewHolder -> holder.recycle()
        }

        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        for (payload in payloads) {
            if (payload is String) when (holder) {
                is ListViewHolder -> holder.binding.mrlItemTitle.text = payload
                is CardComposeViewHolder -> onBindViewHolder(holder, position)
            }
        }
    }

    inner class DummyViewHolder(val binding: MrlDummyItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.container.setOnClickListener {
                dummyClickListener?.invoke()
            }
        }

        fun recycle() {}
    }

    inner class ListViewHolder(val binding: MrlItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener { eventActor.trySend(ShowContext(layoutPosition)).isSuccess }
            binding.mrlCtx.setOnClickListener { eventActor.trySend(ShowContext(layoutPosition)) }
            binding.selector.setOnClickListener { onClick(it) }
        }

        override fun onClick(v: View) {
            dataset.getOrNull(layoutPosition)?.let { eventActor.trySend(Playmedia(it)) }
        }

        fun recycle() {}
    }

    inner class CardComposeViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

        fun bind(item: MediaWrapper) {
            composeView.setContent {
                VLCBrowserItemCard(
                    title = Uri.decode(item.title),
                    subtitle = Uri.decode(item.location),
                    modifier = Modifier.widthIn(min = 160.dp),
                    contentDescription = TalkbackUtil.getStream(composeView.context, item),
                    titleMaxLines = 1,
                    onClick = ::play,
                    onLongClick = ::showContext,
                    artworkContent = { StreamCardIcon() },
                    moreActionContent = { StreamMoreIcon() },
                    onMoreClick = ::showContext
                )
            }
        }

        private fun play() {
            dataset.getOrNull(bindingAdapterPosition)?.let { eventActor.trySend(Playmedia(it)) }
        }

        private fun showContext() {
            val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
            eventActor.trySend(ShowContext(position))
        }
    }

    @Composable
    private fun StreamCardIcon() {
        Icon(
            painter = painterResource(R.drawable.ic_more_stream),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = VLCThemeDefaults.colors.primary
        )
    }

    @Composable
    private fun StreamMoreIcon() {
        Icon(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = stringResource(R.string.more_actions),
            tint = VLCThemeDefaults.colors.listSubtitle
        )
    }

    override fun createCB() = object : DiffCallback<MediaWrapper>() {
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
                    && oldList[oldItemPosition].title == newList[newItemPosition].title
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int) = newList[newItemPosition].title
    }

    fun setOnDummyClickListener(dummyClickLisener: () -> Unit) {
        this.dummyClickListener = dummyClickLisener
    }
}

sealed class MrlAction
class Playmedia(val media: MediaWrapper) : MrlAction()
class ShowContext(val position: Int) : MrlAction()
