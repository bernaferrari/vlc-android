/*
 * ************************************************************************
 *  VideoResizeOverlayView.kt
 * *************************************************************************
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
 * **************************************************************************
 */

package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.libvlc.MediaPlayer
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * Compose replacement for the former video resize XML overlay. The delegate
 * owns settings and player mutations; this view owns the side panel UI.
 */
class VideoResizeOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var selectedScale by mutableStateOf(MediaPlayer.ScaleType.SURFACE_BEST_FIT)
    private var showFoldSection by mutableStateOf(false)
    private var foldChecked by mutableStateOf(true)
    private var showNotchSection by mutableStateOf(false)
    private var notchChecked by mutableStateOf(true)
    private var focusRequestToken by mutableIntStateOf(0)
    private var onDismissClick: () -> Unit = {}
    private var onFoldCheckedChange: (Boolean) -> Unit = {}
    private var onNotchCheckedChange: (Boolean) -> Unit = {}
    private var onScaleSelected: (MediaPlayer.ScaleType) -> Unit = {}

    init {
        isClickable = true
        isFocusable = false
    }

    fun bind(
        selectedScale: MediaPlayer.ScaleType,
        showFoldSection: Boolean,
        foldChecked: Boolean,
        showNotchSection: Boolean,
        notchChecked: Boolean,
        onDismiss: () -> Unit,
        onFoldCheckedChange: (Boolean) -> Unit,
        onNotchCheckedChange: (Boolean) -> Unit,
        onScaleSelected: (MediaPlayer.ScaleType) -> Unit
    ) {
        this.selectedScale = selectedScale
        this.showFoldSection = showFoldSection
        this.foldChecked = foldChecked
        this.showNotchSection = showNotchSection
        this.notchChecked = notchChecked
        onDismissClick = onDismiss
        this.onFoldCheckedChange = onFoldCheckedChange
        this.onNotchCheckedChange = onNotchCheckedChange
        this.onScaleSelected = onScaleSelected
    }

    fun requestInitialFocus() {
        focusRequestToken += 1
    }

    @Composable
    override fun WidgetContent() {
        val dismissInteraction = remember { MutableInteractionSource() }
        val panelInteraction = remember { MutableInteractionSource() }
        val firstScaleFocusRequester = remember { FocusRequester() }

        LaunchedEffect(focusRequestToken) {
            if (focusRequestToken > 0) {
                runCatching { firstScaleFocusRequester.requestFocus() }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismissClick
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF202020))
                    .clickable(
                        interactionSource = panelInteraction,
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_popup_close_w),
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White
                    )
                }
                if (showFoldSection) {
                    PanelTitle(text = stringResource(R.string.foldable))
                    ToggleRow(
                        checked = foldChecked,
                        text = stringResource(R.string.fold_optimize),
                        onCheckedChange = {
                            foldChecked = it
                            onFoldCheckedChange(it)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (showNotchSection) {
                    PanelTitle(text = stringResource(R.string.notch))
                    ToggleRow(
                        checked = notchChecked,
                        text = stringResource(R.string.player_under_notch),
                        onCheckedChange = {
                            notchChecked = it
                            onNotchCheckedChange(it)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                PanelTitle(text = stringResource(R.string.aspect_ratio))
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MediaPlayer.ScaleType.entries, key = { it.name }) { scale ->
                        val focusModifier = if (scale == MediaPlayer.ScaleType.entries.first()) {
                            Modifier.focusRequester(firstScaleFocusRequester)
                        } else {
                            Modifier
                        }
                        ScaleRow(
                            scale = scale,
                            selected = scale == selectedScale,
                            modifier = focusModifier,
                            onClick = {
                                selectedScale = scale
                                onScaleSelected(scale)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PanelTitle(text: String) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun ToggleRow(
        checked: Boolean,
        text: String,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp)
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    @Composable
    private fun ScaleRow(
        scale: MediaPlayer.ScaleType,
        selected: Boolean,
        modifier: Modifier,
        onClick: () -> Unit
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable(onClick = onClick)
                .focusable()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_delay_done),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
            Text(
                text = scaleLabel(scale),
                color = if (selected) Color.White else Color.White.copy(alpha = 0.5F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }

    @Composable
    private fun scaleLabel(scale: MediaPlayer.ScaleType) = when (scale) {
        MediaPlayer.ScaleType.SURFACE_BEST_FIT -> stringResource(R.string.surface_best_fit)
        MediaPlayer.ScaleType.SURFACE_FIT_SCREEN -> stringResource(R.string.surface_fit_screen)
        MediaPlayer.ScaleType.SURFACE_FILL -> stringResource(R.string.surface_fill)
        MediaPlayer.ScaleType.SURFACE_ORIGINAL -> stringResource(R.string.surface_original)
        MediaPlayer.ScaleType.SURFACE_16_9 -> "16:9"
        MediaPlayer.ScaleType.SURFACE_4_3 -> "4:3"
        MediaPlayer.ScaleType.SURFACE_16_10 -> "16:10"
        MediaPlayer.ScaleType.SURFACE_2_1 -> "2:1"
        MediaPlayer.ScaleType.SURFACE_221_1 -> "2.21:1"
        MediaPlayer.ScaleType.SURFACE_235_1 -> "2.35:1"
        MediaPlayer.ScaleType.SURFACE_239_1 -> "2.39:1"
        MediaPlayer.ScaleType.SURFACE_5_4 -> "5:4"
    }
}
