/*
 * *************************************************************************
 *  BaseAdapter.java
 * **************************************************************************
 *  Copyright © 2016-2017 VLC authors and VideoLAN
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
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.MotionEventCompat
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.Artist
import org.videolan.medialibrary.interfaces.media.Genre
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AppContextProvider
import org.videolan.resources.UPDATE_FAVORITE_STATE
import org.videolan.resources.UPDATE_SELECTION
import org.videolan.resources.interfaces.FocusListener
import org.videolan.tools.MultiSelectAdapter
import org.videolan.tools.MultiSelectHelper
import org.videolan.tools.Settings
import org.videolan.vlc.BR
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCBrowserItemCard
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.databinding.AudioBrowserCardItemBinding
import org.videolan.vlc.databinding.AudioBrowserItemBinding
import org.videolan.vlc.gui.helpers.MARQUEE_ACTION
import org.videolan.vlc.gui.helpers.MarqueeViewHolder
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.enableMarqueeEffect
import org.videolan.vlc.gui.helpers.getAudioIconDrawable
import org.videolan.vlc.gui.helpers.loadImage
import org.videolan.vlc.gui.view.FadableImageView
import org.videolan.vlc.gui.view.FastScroller
import org.videolan.vlc.gui.view.MiniVisualizer
import org.videolan.vlc.interfaces.IEventsHandler
import org.videolan.vlc.interfaces.IListEventsHandler
import org.videolan.vlc.interfaces.SwipeDragHelperAdapter
import org.videolan.vlc.util.LifecycleAwareScheduler
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.util.isOTG
import org.videolan.vlc.util.isSD
import org.videolan.vlc.util.isSchemeSMB
import org.videolan.vlc.viewmodels.PlaylistModel

private const val SHOW_IN_LIST = -1

open class AudioBrowserAdapter @JvmOverloads constructor(
        type: Int,
        protected val eventsHandler: IEventsHandler<MediaLibraryItem>,
        protected val listEventsHandler: IListEventsHandler? = null,
        protected val reorderable: Boolean = false,
        internal var cardSize: Int = SHOW_IN_LIST
) : PagedListAdapter<MediaLibraryItem,
        AudioBrowserAdapter.AbstractMediaItemViewHolder>(DIFF_CALLBACK),
        FastScroller.SeparatedAdapter, MultiSelectAdapter<MediaLibraryItem>, SwipeDragHelperAdapter
{
    protected var listImageWidth: Int
    val multiSelectHelper: MultiSelectHelper<MediaLibraryItem> = MultiSelectHelper(this, UPDATE_SELECTION)
    protected val defaultCover: BitmapDrawable?
    private val defaultCoverCard: BitmapDrawable?
    private var focusNext = -1
    private var focusListener: FocusListener? = null
    lateinit var inflater: LayoutInflater
    private var scheduler: LifecycleAwareScheduler? = null
    var stopReorder = false
    var areSectionsEnabled = true
    private var currentPlayingVisu: MiniVisualizer? = null
    private var model: PlaylistModel? = null

    var currentMedia:MediaWrapper? = null
        set(media) {
            if (media == currentMedia) return
            val former = currentMedia
            field = media
            if (former != null) currentList?.indexOf(former)?.let {
                notifyItemChanged(it)
            }
            if (media != null) {
                currentList?.indexOf(media)?.let {
                    notifyItemChanged(it)
                }
            }
            playbackStateChanged(former, currentMedia)
        }
    protected fun inflaterInitialized() = ::inflater.isInitialized

    open fun playbackStateChanged(former: MediaWrapper?, currentMedia: MediaWrapper?) {}

    val isEmpty: Boolean
        get() = currentList.isNullOrEmpty()

    init {
        val ctx = when (eventsHandler) {
            is Context -> eventsHandler
            is Fragment -> eventsHandler.requireContext()
            else -> AppContextProvider.appContext
        }
        listImageWidth = ctx.resources.getDimension(R.dimen.audio_browser_item_size).toInt()
        defaultCover = getAudioIconDrawable(ctx, type, false)
        defaultCoverCard = getAudioIconDrawable(ctx, type, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractMediaItemViewHolder {
        return if (displayInCard()) {
            AudioBrowserComposeViewHolder(
                ComposeView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                },
                inCard = true
            )
        } else {
            AudioBrowserComposeViewHolder(
                ComposeView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                },
                inCard = false
            )
        }
    }

    private fun displayInCard() = cardSize != SHOW_IN_LIST

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (Settings.listTitleEllipsize == 4) scheduler = enableMarqueeEffect(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        scheduler?.cancelAction("")
        currentMedia = null
        currentPlayingVisu = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun setCurrentlyPlaying(playing: Boolean) {
        if (playing) currentPlayingVisu?.start() else currentPlayingVisu?.stop()
        currentMedia?.let { media ->
            currentList?.indexOf(media)?.takeIf { it >= 0 }?.let { notifyItemChanged(it) }
        }
    }

    fun setModel(model: PlaylistModel) {
        this.model = model
    }

    override fun onBindViewHolder(holder: AbstractMediaItemViewHolder, position: Int) {
        if (position >= itemCount) return
        val item = getItem(position)
        if (item is Artist) item.description = holder.itemView.context.resources.getQuantityString(R.plurals.albums_quantity, item.albumsCount, item.albumsCount)
        if (item is Genre) item.description = holder.itemView.context.resources.getQuantityString(R.plurals.track_quantity, item.tracksCount, item.tracksCount)
        val isSelected = multiSelectHelper.isSelected(position)
        val isCurrent = currentMedia == item
        holder.bindItem(
            item = item,
            selected = isSelected,
            inSelection = multiSelectHelper.inActionMode,
            isCurrent = isCurrent,
            playing = model?.playing != false
        )
        if (isCurrent) currentPlayingVisu = holder.getMiniVisu()
        if (position == focusNext) {
            holder.itemView.requestFocus()
            focusNext = -1
        }
    }

    override fun onBindViewHolder(holder: AbstractMediaItemViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNullOrEmpty())
            onBindViewHolder(holder, position)
        else onBindViewHolder(holder, position)
    }

    override fun onViewRecycled(h: AbstractMediaItemViewHolder) {
        scheduler?.cancelAction(MARQUEE_ACTION)
        h.recycle()
        super.onViewRecycled(h)
    }

    private fun isPositionValid(position: Int): Boolean {
        return position in 0 until itemCount
    }

    override fun getItemId(position: Int): Long {
        if (!isPositionValid(position)) return -1
        val item = getItem(position)
        return item?.id ?: -1
    }

    override fun getItem(position: Int): MediaLibraryItem? {
        return if (position in 0 until itemCount) super.getItem(position) else null
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return item?.itemType ?: MediaLibraryItem.TYPE_MEDIA
    }

    fun clear() {
        //        getDataset().clear();
    }


    override fun onCurrentListChanged(previousList: PagedList<MediaLibraryItem>?, currentList: PagedList<MediaLibraryItem>?) {
        eventsHandler.onUpdateFinished(this@AudioBrowserAdapter)
    }

    override fun hasSections(): Boolean {
        return areSectionsEnabled
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemMoved(dragFrom: Int, dragTo: Int) {
        listEventsHandler!!.onMove(dragFrom, dragTo)
        preventNextAnim = true
    }

    override fun onItemDismiss(position: Int) {
        val item = getItem(position)
        listEventsHandler!!.onRemove(position, item!!)
    }


    fun setOnFocusChangeListener(focusListener: FocusListener?) {
        this.focusListener = focusListener
    }

    inner class AudioBrowserComposeViewHolder(
            private val composeView: ComposeView,
            private val inCard: Boolean
    ) : AbstractMediaItemViewHolder(composeView) {

        override fun bindItem(
                item: MediaLibraryItem?,
                selected: Boolean,
                inSelection: Boolean,
                isCurrent: Boolean,
                playing: Boolean
        ) {
            val title = item?.title.orEmpty()
            val subtitle = audioBrowserSubtitle(composeView.context, item)
            val isPresent = item !is MediaWrapper || item.isPresent
            composeView.setContent {
                val colors = VLCThemeDefaults.colors
                val cardModifier = if (inCard && cardSize != SHOW_IN_LIST) {
                    Modifier.width(with(LocalDensity.current) { cardSize.toDp() })
                } else {
                    Modifier
                }
                if (inCard) {
                    VLCBrowserItemCard(
                            title = title,
                            subtitle = subtitle,
                            modifier = cardModifier,
                            selected = selected,
                            contentDescription = audioBrowserContentDescription(composeView.context, item),
                            titleMaxLines = 1,
                            onClick = ::onClick,
                            onLongClick = ::onLongClick,
                            artworkContent = {
                                AudioBrowserArtwork(
                                        item = item,
                                        defaultCover = defaultCoverCard,
                                        imageWidth = cardSize,
                                        card = true,
                                        selected = selected,
                                        isPresent = isPresent,
                                        isCurrent = isCurrent,
                                        playing = playing,
                                        onImageClick = ::onImageClick
                                )
                            },
                            badgeContent = {
                                AudioBrowserBadges(item = item, card = true, isPresent = isPresent)
                            },
                            primaryActionContent = if (isPresent && !inSelection) {
                                {
                                    Icon(
                                            painter = painterResource(R.drawable.ic_play),
                                            contentDescription = stringResource(R.string.play),
                                            tint = colors.primary
                                    )
                                }
                            } else null,
                            onPrimaryActionClick = ::onMainActionClick,
                            moreActionContent = if (isPresent && !inSelection) {
                                { AudioBrowserMoreIcon() }
                            } else null,
                            onMoreClick = ::onMoreClick
                    )
                } else {
                    VLCBrowserItemRow(
                            title = title,
                            subtitle = subtitle,
                            selected = selected,
                            contentDescription = audioBrowserContentDescription(composeView.context, item),
                            onClick = ::onClick,
                            onLongClick = ::onLongClick,
                            artworkContent = {
                                AudioBrowserArtwork(
                                        item = item,
                                        defaultCover = defaultCover,
                                        imageWidth = listImageWidth,
                                        card = false,
                                        selected = selected,
                                        isPresent = isPresent,
                                        isCurrent = isCurrent,
                                        playing = playing,
                                        onImageClick = ::onImageClick
                                )
                            },
                            badgeContent = {
                                if (canBeReordered && !inSelection) AudioBrowserMoveHandle(onTouch = ::onMoveTouch)
                                AudioBrowserBadges(item = item, card = false, isPresent = isPresent)
                            },
                            moreActionContent = if (isPresent && !inSelection) {
                                { AudioBrowserMoreIcon() }
                            } else null,
                            onMoreClick = ::onMoreClick
                    )
                }
            }
        }

        private fun onClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onClick(composeView, position, it) }
            }
        }

        private fun onLongClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onLongClick(composeView, position, it) }
            }
        }

        private fun onImageClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onImageClick(composeView, position, it) }
            }
        }

        private fun onMoreClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onCtxClick(composeView, position, it) }
            }
        }

        private fun onMainActionClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onMainActionClick(composeView, position, it) }
            }
        }

        private fun onMoveTouch(event: MotionEvent): Boolean {
            if (listEventsHandler == null) return false
            if (multiSelectHelper.getSelectionCount() != 0) return false
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                listEventsHandler.onStartDrag(this)
                return true
            }
            return false
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    inner class MediaItemViewHolder(binding: AudioBrowserItemBinding) : BindingMediaItemViewHolder<AudioBrowserItemBinding>(binding) {
        var onTouchListener: View.OnTouchListener

        override val titleView: TextView? = binding.title

        init {
            binding.holder = this
            defaultCover?.let { binding.cover = it }
            if (AndroidUtil.isMarshMallowOrLater)
                itemView.setOnContextClickListener { v ->
                    onMoreClick(v)
                    true
                }

            onTouchListener = object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    if (listEventsHandler == null) {
                        return false
                    }
                    if (multiSelectHelper.getSelectionCount() != 0) {
                        return false
                    }
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        listEventsHandler.onStartDrag(this@MediaItemViewHolder)
                        return true
                    }
                    return false
                }
            }
            binding.imageWidth = listImageWidth
        }

        override fun selectView(selected: Boolean) {
            binding.setVariable(BR.selected, selected)
            binding.itemMore.visibility = if (multiSelectHelper.inActionMode) View.INVISIBLE else View.VISIBLE
        }

        override fun setItem(item: MediaLibraryItem?) {
            binding.item = item
        }

        override fun recycle() {
            binding.cover = if (cardSize == SHOW_IN_LIST && defaultCover != null) defaultCover else null
            binding.mediaCover.resetFade()
            binding.title.isSelected = false
        }

        override fun getMiniVisu() = binding.playing

        override fun changePlayingVisibility(isCurrent: Boolean) {
            binding.mediaCover.visibility = if (isCurrent) View.INVISIBLE else View.VISIBLE
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    inner class MediaItemCardViewHolder(binding: AudioBrowserCardItemBinding) : BindingMediaItemViewHolder<AudioBrowserCardItemBinding>(binding) {

        override val titleView = binding.title

        init {
            binding.holder = this
            binding.scaleType = ImageView.ScaleType.CENTER_INSIDE
            defaultCoverCard?.let { binding.cover = it }
            if (AndroidUtil.isMarshMallowOrLater)
                itemView.setOnContextClickListener { v ->
                    onMoreClick(v)
                    true
                }
            binding.imageWidth = cardSize
            binding.container.layoutParams.width = cardSize

        }

        override fun selectView(selected: Boolean) {
            super.selectView(selected)
            binding.setVariable(BR.selected, selected)
            binding.itemMore.visibility = if (multiSelectHelper.inActionMode) View.INVISIBLE else View.VISIBLE
            binding.mainActionButton.visibility = if (multiSelectHelper.inActionMode) View.INVISIBLE else View.VISIBLE
        }

        override fun setItem(item: MediaLibraryItem?) {
            binding.item = item
        }

        override fun recycle() {
            defaultCoverCard?.let { binding.cover = it }
            binding.mediaCover.resetFade()
            binding.title.isSelected = false
        }

        override fun getMiniVisu() = binding.playing

        override fun changePlayingVisibility(isCurrent: Boolean) { }

    }

    abstract inner class BindingMediaItemViewHolder<T : ViewDataBinding>(val binding: T) : AbstractMediaItemViewHolder(binding.root) {

        override fun bindItem(
                item: MediaLibraryItem?,
                selected: Boolean,
                inSelection: Boolean,
                isCurrent: Boolean,
                playing: Boolean
        ) {
            setItem(item)
            selectView(selected)
            if (item is MediaWrapper) {
                binding.setVariable(BR.isNetwork, item.uri.scheme.isSchemeSMB())
                binding.setVariable(BR.isOTG, item.uri.isOTG())
                binding.setVariable(BR.isSD, item.uri.isSD())
                binding.setVariable(BR.isPresent, item.isPresent)
            } else {
                binding.setVariable(BR.isPresent, true)
            }
            val miniVisualizer = getMiniVisu()
            if (isCurrent) {
                if (playing) miniVisualizer?.start() else miniVisualizer?.stop()
                miniVisualizer?.visibility = View.VISIBLE
                changePlayingVisibility(true)
            } else {
                miniVisualizer?.stop()
                changePlayingVisibility(false)
                miniVisualizer?.visibility = View.INVISIBLE
            }
            item?.let { binding.setVariable(BR.isFavorite, it.isFavorite) }
            binding.setVariable(BR.inSelection, inSelection)
            binding.invalidateAll()
            binding.executePendingBindings()
        }

        abstract fun setItem(item: MediaLibraryItem?)
    }

    @TargetApi(Build.VERSION_CODES.M)
    abstract inner class AbstractMediaItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), MarqueeViewHolder {

        override val titleView: TextView? = null

        val canBeReordered: Boolean
            get() = reorderable && !stopReorder

        fun onClick(v: View) {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onClick(v, position, it) }
            }
        }

        fun onMoreClick(v: View) {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onCtxClick(v, position, it) }
            }
        }

        fun onLongClick(v: View): Boolean {
            return bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onLongClick(v, position, it) }
            } == true
        }

        fun onImageClick(v: View) {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onImageClick(v, position, it) }
            }
        }

        fun onMainActionClick(v: View) {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                getItem(position)?.let { eventsHandler.onMainActionClick(v, position, it) }
            }
        }

        open fun bindItem(
                item: MediaLibraryItem?,
                selected: Boolean,
                inSelection: Boolean,
                isCurrent: Boolean,
                playing: Boolean
        ) {
            selectView(selected)
        }

        open fun selectView(selected: Boolean) = Unit

        open fun getMiniVisu(): MiniVisualizer? = null

        open fun recycle() = Unit
        open fun changePlayingVisibility(isCurrent: Boolean) = Unit

    }

    companion object {

        private const val TAG = "VLC/AudioBrowserAdapter"
        private const val UPDATE_PAYLOAD = 1
        /**
         * Awful hack to workaround the [PagedListAdapter] not keeping track of notifyItemMoved operations
         */
        private var preventNextAnim: Boolean = false

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MediaLibraryItem>() {
            override fun areItemsTheSame(
                    oldMedia: MediaLibraryItem, newMedia: MediaLibraryItem): Boolean {
                return if (preventNextAnim) {
                    true
                } else if (oldMedia is MediaWrapper && newMedia is MediaWrapper && oldMedia.isPresent != newMedia.isPresent) {
                    false
                } else oldMedia === newMedia || oldMedia.title == newMedia.title && oldMedia.itemType == newMedia.itemType && oldMedia.tracksCount == newMedia.tracksCount && oldMedia.equals(newMedia)
            }

            override fun areContentsTheSame(
                    oldMedia: MediaLibraryItem, newMedia: MediaLibraryItem): Boolean {
                return false
            }

            override fun getChangePayload(oldItem: MediaLibraryItem, newItem: MediaLibraryItem): Any {
                preventNextAnim = false
                when {
                    oldItem.isFavorite != newItem.isFavorite  -> return UPDATE_FAVORITE_STATE
                }
                return UPDATE_PAYLOAD
            }
        }
    }
}

@Composable
private fun BoxScope.AudioBrowserArtwork(
        item: MediaLibraryItem?,
        defaultCover: BitmapDrawable?,
        imageWidth: Int,
        card: Boolean,
        selected: Boolean,
        isPresent: Boolean,
        isCurrent: Boolean,
        playing: Boolean,
        onImageClick: () -> Unit
) {
    val size = if (card) 48.dp else 40.dp
    val colors = VLCThemeDefaults.colors
    Box(
            modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.backgroundDefaultDarker)
                    .clickable(onClick = onImageClick)
                    .alpha(if (isPresent) 1f else 0.45f),
            contentAlignment = Alignment.Center
    ) {
        AndroidView(
                factory = { context ->
                    FadableImageView(context).apply {
                        scaleType = if (card) ImageView.ScaleType.CENTER_INSIDE else ImageView.ScaleType.CENTER_CROP
                        defaultCover?.let { setImageDrawable(it) }
                    }
                },
                modifier = Modifier.size(size),
                update = { imageView ->
                    imageView.resetFade()
                    defaultCover?.let { imageView.setImageDrawable(it) }
                    loadImage(imageView, item, imageWidth = imageWidth.takeIf { it > 0 } ?: 0, card = card)
                }
        )
        if (!isPresent) {
            Box(
                    modifier = Modifier
                            .size(size)
                            .background(Color.Black.copy(alpha = 0.32f))
            )
        }
        if (selected) {
            Icon(
                    painter = painterResource(R.drawable.ic_video_grid_check),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(18.dp)
            )
        }
        if (isCurrent) {
            Box(
                    modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.58f))
                            .padding(5.dp),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        painter = painterResource(if (playing) R.drawable.ic_pause_player else R.drawable.ic_play_player),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (card) 24.dp else 20.dp)
                )
            }
        }
    }
}

@Composable
internal fun AudioBrowserBadges(item: MediaLibraryItem?, card: Boolean, isPresent: Boolean) {
    if (item?.isFavorite == true) {
        AudioBrowserBadgeIcon(if (card) R.drawable.ic_emoji_favorite_white else R.drawable.ic_emoji_favorite, boxed = card)
    }
    val media = item as? MediaWrapper
    when {
        !isPresent -> AudioBrowserBadgeIcon(R.drawable.ic_emoji_absent, boxed = card)
        media?.uri?.scheme.isSchemeSMB() -> AudioBrowserBadgeIcon(R.drawable.ic_emoji_network, boxed = card)
        media?.uri?.isSD() == true -> AudioBrowserBadgeIcon(R.drawable.ic_emoji_sd, boxed = card)
        media?.uri?.isOTG() == true -> AudioBrowserBadgeIcon(R.drawable.ic_emoji_otg, boxed = card)
    }
}

@Composable
private fun AudioBrowserBadgeIcon(icon: Int, boxed: Boolean) {
    val modifier = Modifier
            .padding(horizontal = 1.dp)
            .then(
                    if (boxed) {
                        Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(3.dp)
                    } else {
                        Modifier.padding(3.dp)
                    }
            )
    Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = modifier.size(16.dp)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AudioBrowserMoveHandle(onTouch: (MotionEvent) -> Boolean) {
    Icon(
            painter = painterResource(R.drawable.ic_move_media),
            contentDescription = stringResource(R.string.more_actions),
            tint = VLCThemeDefaults.colors.listSubtitle,
            modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp)
                    .pointerInteropFilter { event -> onTouch(event) }
    )
}

@Composable
internal fun AudioBrowserMoreIcon() {
    Icon(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = stringResource(R.string.more),
            tint = VLCThemeDefaults.colors.listSubtitle
    )
}

private fun audioBrowserSubtitle(context: Context, item: MediaLibraryItem?): String? {
    return when (item) {
        null -> null
        is Playlist -> {
            if (item.duration != 0L) {
                val duration = Tools.millisToString(item.duration)
                TextUtils.separatedString(
                        context.getString(R.string.track_number, item.tracksCount),
                        if (item.nbDurationUnknown > 0) "$duration+" else duration
                )
            } else {
                context.getString(R.string.track_number, item.tracksCount)
            }
        }
        else -> item.description?.toString()
    }
}

private fun audioBrowserContentDescription(context: Context, item: MediaLibraryItem?): String? {
    return when (item) {
        null -> null
        is Album -> TalkbackUtil.getAlbum(context, item)
        is Artist -> TalkbackUtil.getArtist(context, item)
        is Genre -> TalkbackUtil.getGenre(context, item)
        is Playlist -> TalkbackUtil.getPlaylist(context, item)
        is MediaWrapper -> when (item.type) {
            MediaWrapper.TYPE_AUDIO -> TalkbackUtil.getAudioTrack(context, item)
            MediaWrapper.TYPE_VIDEO -> TalkbackUtil.getVideo(context, item)
            MediaWrapper.TYPE_STREAM -> TalkbackUtil.getStream(context, item)
            MediaWrapper.TYPE_DIR, MediaWrapper.TYPE_SUBTITLE, MediaWrapper.TYPE_PLAYLIST -> TalkbackUtil.getDir(context, item, false)
            MediaWrapper.TYPE_ALL -> TalkbackUtil.getAll(item)
            else -> item.title
        }
        else -> TalkbackUtil.getAll(item)
    }
}
