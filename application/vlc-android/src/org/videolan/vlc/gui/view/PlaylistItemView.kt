package org.videolan.vlc.gui.view

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioPlaylistItem
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.getBitmapFromDrawable
import org.videolan.vlc.gui.helpers.loadImage
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.trackNumberText
import kotlin.math.roundToInt

data class PlaylistItemState(
    val media: MediaWrapper? = null,
    val subtitle: String = "",
    val showTrackNumbers: Boolean = false,
    val showReorderButtons: Boolean = false,
    val showDeleteButton: Boolean = false,
    val stopAfterThis: Boolean = false,
    val current: Boolean = false,
    val playing: Boolean = false,
    val masked: Boolean = false
)

/**
 * Compose-backed replacement for playlist_item.xml. RecyclerView remains a
 * temporary host for the video overlay while the audio player queue has moved
 * to a full Compose LazyColumn.
 */
class PlaylistItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var state by mutableStateOf(PlaylistItemState())
    private val tipsOverlayColor = context.resolveComposeColor(R.attr.background_audio_tips)
    private var onRowClick: () -> Unit = {}
    private var onMoveUpClick: () -> Unit = {}
    private var onMoveDownClick: () -> Unit = {}
    private var onDeleteClick: () -> Unit = {}
    private var onMoreClick: () -> Unit = {}

    init {
        isClickable = true
        isFocusable = true
    }

    fun bind(
        media: MediaWrapper,
        subtitle: String = MediaUtils.getMediaSubtitle(media),
        showTrackNumbers: Boolean,
        showReorderButtons: Boolean,
        showDeleteButton: Boolean,
        stopAfterThis: Boolean,
        current: Boolean,
        playing: Boolean,
        masked: Boolean = false
    ) {
        state = PlaylistItemState(
            media = media,
            subtitle = subtitle,
            showTrackNumbers = showTrackNumbers,
            showReorderButtons = showReorderButtons,
            showDeleteButton = showDeleteButton,
            stopAfterThis = stopAfterThis,
            current = current,
            playing = playing,
            masked = masked
        )
    }

    fun setCallbacks(
        onRowClick: () -> Unit,
        onMoveUpClick: () -> Unit,
        onMoveDownClick: () -> Unit,
        onDeleteClick: () -> Unit,
        onMoreClick: () -> Unit
    ) {
        this.onRowClick = onRowClick
        this.onMoveUpClick = onMoveUpClick
        this.onMoveDownClick = onMoveDownClick
        this.onDeleteClick = onDeleteClick
        this.onMoreClick = onMoreClick
    }

    fun setMasked(masked: Boolean) {
        state = state.copy(masked = masked)
    }

    fun setCurrent(current: Boolean, playing: Boolean) {
        state = state.copy(current = current, playing = playing)
    }

    fun setPlaying(playing: Boolean) {
        state = state.copy(playing = playing)
    }

    fun deleteButtonCenterX(): Int {
        val action = actionButtonSizePx()
        return width - action - action / 2
    }

    fun moveUpButtonCenterX(): Int {
        val action = actionButtonSizePx()
        return width - (action * 3) - action / 2
    }

    @Composable
    override fun WidgetContent() {
        val item = state.media ?: return
        AudioPlaylistMediaItem(
            media = item,
            subtitle = state.subtitle,
            showTrackNumbers = state.showTrackNumbers,
            showReorderButtons = state.showReorderButtons,
            showDeleteButton = state.showDeleteButton,
            stopAfterThis = state.stopAfterThis,
            current = state.current,
            playing = state.playing,
            masked = state.masked,
            tipsOverlayColor = tipsOverlayColor,
            onClick = onRowClick,
            onMoveUpClick = onMoveUpClick,
            onMoveDownClick = onMoveDownClick,
            onDeleteClick = onDeleteClick,
            onMoreClick = onMoreClick
        )
    }

    private fun actionButtonSizePx() = (48 * resources.displayMetrics.density).roundToInt()
}

@Composable
fun AudioPlaylistMediaItem(
    media: MediaWrapper,
    subtitle: String,
    showTrackNumbers: Boolean,
    showReorderButtons: Boolean,
    showDeleteButton: Boolean,
    stopAfterThis: Boolean,
    current: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
    masked: Boolean = false,
    tipsOverlayColor: Color = Color.Transparent,
    onClick: () -> Unit = {},
    onMoveUpClick: () -> Unit = {},
    onMoveDownClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    val context = LocalContext.current
    VLCAudioPlaylistItem(
        title = media.title,
        subtitle = subtitle,
        contentDescription = media.contentDescription(context),
        trackNumberText = media.trackNumberText(),
        showTrackNumber = showTrackNumbers,
        showReorderButtons = showReorderButtons,
        showDeleteButton = showDeleteButton,
        stopAfterThis = stopAfterThis,
        current = current,
        video = media.type == MediaWrapper.TYPE_VIDEO,
        marqueeTitle = Settings.listTitleEllipsize == 4,
        masked = masked,
        tipsOverlayColor = tipsOverlayColor,
        onClick = onClick,
        onMoveUpClick = onMoveUpClick,
        onMoveDownClick = onMoveDownClick,
        onDeleteClick = onDeleteClick,
        onMoreClick = onMoreClick,
        modifier = modifier,
        coverContent = { AudioPlaylistMediaCover(media) },
        playingContent = { AudioPlaylistPlayingIndicator(playing) },
        stopAfterContent = {
            Icon(
                painter = painterResource(R.drawable.ic_stop_after_this),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor
            )
        },
        moveUpContent = {
            Icon(
                painter = painterResource(R.drawable.ic_playlist_moveup),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor
            )
        },
        moveDownContent = {
            Icon(
                painter = painterResource(R.drawable.ic_playlist_movedown),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor
            )
        },
        deleteContent = {
            Icon(
                painter = painterResource(R.drawable.ic_playlist_delete),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor
            )
        },
        moreContent = {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.playerIconColor
            )
        }
    )
}

@Composable
private fun AudioPlaylistMediaCover(media: MediaWrapper) {
    AndroidView(
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { image ->
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            image.setImageDrawable(media.defaultCover(image.context))
            loadImage(image, media, card = true)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun AudioPlaylistPlayingIndicator(playing: Boolean) {
    val heights = if (playing) listOf(22.dp, 30.dp, 18.dp) else listOf(4.dp, 4.dp, 4.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(32.dp)
            .height(32.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxHeight()
        ) {
            heights.forEach { height ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height)
                        .background(VLCThemeDefaults.colors.playerIconColor)
                )
            }
        }
    }
}

private fun MediaWrapper.defaultCover(context: Context) = if (type == MediaWrapper.TYPE_VIDEO) {
    UiTools.getDefaultVideoDrawable(context)
} else {
    BitmapDrawable(context.resources, getBitmapFromDrawable(context, R.drawable.ic_song_background))
}

private fun MediaWrapper.contentDescription(context: Context) = when (type) {
    MediaWrapper.TYPE_VIDEO -> TalkbackUtil.getVideo(context, this)
    MediaWrapper.TYPE_AUDIO -> TalkbackUtil.getAudioTrack(context, this)
    MediaWrapper.TYPE_STREAM -> TalkbackUtil.getStream(context, this)
    MediaWrapper.TYPE_DIR, MediaWrapper.TYPE_SUBTITLE, MediaWrapper.TYPE_PLAYLIST -> TalkbackUtil.getDir(context, this, false)
    MediaWrapper.TYPE_ALL -> TalkbackUtil.getAll(this)
    else -> title
}

private fun Context.resolveComposeColor(attr: Int): Color {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return Color(typedValue.data)
}
