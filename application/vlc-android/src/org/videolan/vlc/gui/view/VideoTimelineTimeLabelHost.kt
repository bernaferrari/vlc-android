/*
 * ************************************************************************
 *  VideoTimelineTimeLabelHost.kt
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

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose-backed replacement for the video HUD elapsed/length time labels.
 * The legacy delegate keeps using the same view IDs for focus, margins, lock
 * state, and bookmark height anchoring while this host owns text rendering.
 */
internal fun VLCComposeView.installVideoTimelineTimeLabelHost(alignEnd: Boolean) {
    val host = VideoTimelineTimeLabelHost(this, alignEnd)
    setTag(id, host)
    isFocusable = true
    isClickable = true
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setOnFocusChangeListener { _, hasFocus -> host.updateFocused(hasFocus) }
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoTimelineTimeLabelHost(): VideoTimelineTimeLabelHost =
    getTag(id) as? VideoTimelineTimeLabelHost ?: error("Missing video timeline time label host")

internal class VideoTimelineTimeLabelHost(
    private val view: VLCComposeView,
    private val alignEnd: Boolean
) {

    private var label by mutableStateOf("")
    private var focused by mutableStateOf(false)
    private var enabledState by mutableStateOf(view.isEnabled)

    fun setTimelineText(text: CharSequence?) {
        label = text?.toString().orEmpty()
    }

    fun setEnabled(enabled: Boolean) {
        view.isEnabled = enabled
        enabledState = enabled
    }

    fun updateFocused(focused: Boolean) {
        this.focused = focused
    }

    @Composable
    fun Content() {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val highlighted = focused || pressed
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7F),
                        offset = Offset(0F, 2F),
                        blurRadius = 6F
                    )
                ),
                modifier = Modifier
                    .then(
                        if (highlighted) {
                            Modifier.background(
                                colorResource(R.color.orange500focus),
                                RoundedCornerShape(4.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        enabled = enabledState,
                        interactionSource = interactionSource,
                        indication = null
                    ) { view.performClick() }
                    .clearAndSetSemantics { }
                    .padding(start = 4.dp, end = 4.dp)
            )
        }
    }
}
