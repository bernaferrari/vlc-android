package org.videolan.vlc.gui.audio

import android.content.Context
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioPlaylistItem
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.compose.VlcMediaImage
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.util.trackNumberText

@Composable
internal fun AudioPlaylistMediaItem(
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
    VlcMediaImage(
        item = media,
        width = if (media.type == MediaWrapper.TYPE_VIDEO) 77.dp else 48.dp,
        fallbackPainter = painterResource(media.defaultCoverResource),
        contentScale = ContentScale.Fit,
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

private val MediaWrapper.defaultCoverResource: Int
    get() = if (type == MediaWrapper.TYPE_VIDEO) R.drawable.ic_no_thumbnail_1610 else R.drawable.ic_song_background

private fun MediaWrapper.contentDescription(context: Context) = when (type) {
    MediaWrapper.TYPE_VIDEO -> TalkbackUtil.getVideo(context, this)
    MediaWrapper.TYPE_AUDIO -> TalkbackUtil.getAudioTrack(context, this)
    MediaWrapper.TYPE_STREAM -> TalkbackUtil.getStream(context, this)
    MediaWrapper.TYPE_DIR, MediaWrapper.TYPE_SUBTITLE, MediaWrapper.TYPE_PLAYLIST -> TalkbackUtil.getDir(context, this, false)
    MediaWrapper.TYPE_ALL -> TalkbackUtil.getAll(this)
    else -> title
}
