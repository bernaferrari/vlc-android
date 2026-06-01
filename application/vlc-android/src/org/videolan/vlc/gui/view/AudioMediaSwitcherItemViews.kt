/*****************************************************************************
 * AudioMediaSwitcherItemViews.kt
 *
 * Copyright © 2026 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults

class HeaderMediaSwitcherItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var state by mutableStateOf(HeaderSwitcherItemState())

    fun bind(title: String?, artist: String?, cover: Bitmap?) {
        state = HeaderSwitcherItemState(
            title = title.orEmpty(),
            artist = artist.orEmpty(),
            cover = cover
        )
    }

    @Composable
    override fun WidgetContent() {
        HeaderSwitcherItem(state)
    }
}

class CoverMediaSwitcherItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var state by mutableStateOf(CoverSwitcherItemState())

    fun bind(
        title: String?,
        subtitle: String?,
        trackInfo: String?,
        cover: Bitmap?,
        showChapterButtons: Boolean,
        showTrackInfo: Boolean,
        onTextClick: () -> Unit,
        onPreviousChapterClick: () -> Unit,
        onNextChapterClick: () -> Unit
    ) {
        state = CoverSwitcherItemState(
            title = title.orEmpty(),
            subtitle = subtitle.orEmpty(),
            trackInfo = trackInfo.orEmpty(),
            cover = cover,
            showChapterButtons = showChapterButtons,
            showTrackInfo = showTrackInfo,
            onTextClick = onTextClick,
            onPreviousChapterClick = onPreviousChapterClick,
            onNextChapterClick = onNextChapterClick
        )
    }

    @Composable
    override fun WidgetContent() {
        CoverSwitcherItem(state)
    }
}

private data class HeaderSwitcherItemState(
    val title: String = "",
    val artist: String = "",
    val cover: Bitmap? = null
)

private data class CoverSwitcherItemState(
    val title: String = "",
    val subtitle: String = "",
    val trackInfo: String = "",
    val cover: Bitmap? = null,
    val showChapterButtons: Boolean = false,
    val showTrackInfo: Boolean = false,
    val onTextClick: () -> Unit = {},
    val onPreviousChapterClick: () -> Unit = {},
    val onNextChapterClick: () -> Unit = {}
)

@Composable
private fun HeaderSwitcherItem(state: HeaderSwitcherItemState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.cover?.let { cover ->
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            MarqueeText(
                text = state.title,
                color = VLCThemeDefaults.colors.fontDefault,
                fontSize = 16,
                fontWeight = FontWeight.Medium
            )
            if (state.artist.isNotEmpty()) {
                MarqueeText(
                    text = state.artist,
                    color = VLCThemeDefaults.colors.fontAudioLight,
                    fontSize = 14
                )
            }
        }
    }
}

@Composable
private fun CoverSwitcherItem(state: CoverSwitcherItemState) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isLargeLandscape = isLandscape && configuration.screenWidthDp >= 600

    if (isLandscape) {
        CoverSwitcherLandscapeItem(state = state, large = isLargeLandscape)
    } else {
        CoverSwitcherPortraitItem(state)
    }
}

@Composable
private fun CoverSwitcherLandscapeItem(state: CoverSwitcherItemState, large: Boolean) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val horizontalPadding = if (large) 48.dp else 0.dp
        val verticalPadding = if (large) 40.dp else 0.dp
        val coverSize = minOf(
            (maxWidth - horizontalPadding).coerceAtLeast(64.dp),
            (maxHeight - verticalPadding).coerceAtLeast(64.dp)
        )

        CoverArt(
            cover = state.cover,
            contentScale = if (large) ContentScale.Crop else ContentScale.Fit,
            elevation = if (large) 12.dp else 16.dp,
            modifier = Modifier.size(coverSize)
        )
    }
}

@Composable
private fun CoverSwitcherPortraitItem(state: CoverSwitcherItemState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val textReserve = if (state.showTrackInfo && state.trackInfo.isNotEmpty()) 160.dp else 128.dp
        val coverSize = minOf(
            maxWidth,
            (maxHeight - textReserve).coerceAtLeast(96.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CoverArt(
                cover = state.cover,
                contentScale = ContentScale.Crop,
                elevation = 16.dp,
                modifier = Modifier.size(coverSize)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (state.showChapterButtons) {
                    ChapterButton(
                        icon = R.drawable.ic_previous_chapter,
                        onClick = state.onPreviousChapterClick
                    )
                }
                MarqueeText(
                    text = state.title,
                    color = VLCThemeDefaults.colors.fontDefault,
                    fontSize = 24,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = state.onTextClick)
                )
                if (state.showChapterButtons) {
                    ChapterButton(
                        icon = R.drawable.ic_next_chapter,
                        onClick = state.onNextChapterClick
                    )
                }
            }

            if (state.subtitle.isNotEmpty()) {
                MarqueeText(
                    text = state.subtitle,
                    color = VLCThemeDefaults.colors.fontAudioLight,
                    fontSize = 14,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable(onClick = state.onTextClick)
                )
            }

            if (state.showTrackInfo && state.trackInfo.isNotEmpty()) {
                MarqueeText(
                    text = state.trackInfo,
                    color = VLCThemeDefaults.colors.fontAudioLight,
                    fontSize = 12,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CoverArt(
    cover: Bitmap?,
    contentScale: ContentScale,
    elevation: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation, RectangleShape, clip = false)
            .clip(RectangleShape)
    ) {
        if (cover != null) {
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_no_thumbnail_song),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ChapterButton(@DrawableRes icon: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = VLCThemeDefaults.colors.fontDefault,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarqueeText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    fontSize: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null
) {
    val marquee = Settings.listTitleEllipsize == 4
    Text(
        text = text,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        maxLines = 1,
        overflow = if (marquee) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (marquee) modifier.basicMarquee(iterations = 1) else modifier
    )
}
