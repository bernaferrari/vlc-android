/*
 * ************************************************************************
 *  VideoOrientationOverlayHost.kt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.video.OrientationMode

/**
 * Compose replacement for the former video orientation XML overlay. The
 * delegate owns settings and orientation mutations; this host owns the side
 * panel UI and user events.
 */
internal fun VLCComposeView.installVideoOrientationOverlayHost() {
    val host = VideoOrientationOverlayHost()
    setTag(R.id.player_orientation_stub, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoOrientationOverlayHost(): VideoOrientationOverlayHost =
    getTag(R.id.player_orientation_stub) as? VideoOrientationOverlayHost ?: error("Missing video orientation overlay host")

internal class VideoOrientationOverlayHost {
    private var selectedOrientation by mutableStateOf(OrientationMode.SENSOR)
    private var showOrientationButton by mutableStateOf(true)
    private var onDismissClick: () -> Unit = {}
    private var onShowButtonChanged: (Boolean) -> Unit = {}
    private var onOrientationSelected: (OrientationMode) -> Unit = {}

    fun bind(
        selected: OrientationMode,
        showButton: Boolean,
        onDismiss: () -> Unit,
        onShowButtonChange: (Boolean) -> Unit,
        onOrientationSelected: (OrientationMode) -> Unit
    ) {
        selectedOrientation = selected
        showOrientationButton = showButton
        onDismissClick = onDismiss
        onShowButtonChanged = onShowButtonChange
        this.onOrientationSelected = onOrientationSelected
    }

    fun updateShowOrientationButton(checked: Boolean) {
        showOrientationButton = checked
    }

    @Composable
    fun Content() {
        val dismissInteraction = remember { MutableInteractionSource() }
        val panelInteraction = remember { MutableInteractionSource() }

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
                Text(
                    text = stringResource(R.string.lock_orientation),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp)
                        .clickable {
                            val checked = !showOrientationButton
                            showOrientationButton = checked
                            onShowButtonChanged(checked)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showOrientationButton,
                        onCheckedChange = {
                            showOrientationButton = it
                            onShowButtonChanged(it)
                        }
                    )
                    Text(
                        text = stringResource(R.string.video_show_orientation_button),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(OrientationMode.entries, key = { it.name }) { mode ->
                        OrientationRow(
                            mode = mode,
                            selected = mode == selectedOrientation,
                            onClick = {
                                selectedOrientation = mode
                                onOrientationSelected(mode)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun OrientationRow(
        mode: OrientationMode,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
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
                text = stringResource(mode.title),
                color = if (selected) Color.White else Color.White.copy(alpha = 0.5F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
