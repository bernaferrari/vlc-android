/*
 * ************************************************************************
 *  VideoDelayOverlayView.kt
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
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 */

package org.videolan.vlc.gui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

enum class VideoDelayOverlayAction {
    Decrease,
    Increase,
    MarkStart,
    MarkStop,
    Reset,
    ApplyAll,
    ApplyBluetooth,
    Close
}

private enum class VideoDelayFocusTarget {
    Plus,
    FirstButton,
    SecondButton
}

private data class VideoDelayOverlayState(
    val title: String = "",
    val value: String = "0 ms",
    val firstButtonText: String = "",
    val secondButtonText: String = "",
    val firstMarked: Boolean = false,
    val secondMarked: Boolean = false,
    val showApplyAll: Boolean = false,
    val showApplyBluetooth: Boolean = false,
    val flashInfoToken: Int = 0
)

/**
 * Compose replacement for player_overlay_settings.xml. VideoDelayDelegate owns
 * playback-service mutations; this view owns only the delay controls surface
 * and exposes a small action/state contract.
 */
class VideoDelayOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    var onAction: ((VideoDelayOverlayAction) -> Unit)? = null

    private var state by mutableStateOf(VideoDelayOverlayState())
    private var focusRequest by mutableStateOf<Pair<VideoDelayFocusTarget, Int>?>(null)
    private var focusToken by mutableIntStateOf(0)
    private val panelColor = Color(ContextCompat.getColor(context, R.color.playerbackground))
    private val flashColor = Color(ContextCompat.getColor(context, R.color.orange500focus))
    private val orangeColor = Color(ContextCompat.getColor(context, R.color.orange500))
    private val inactiveIconColor = Color(ContextCompat.getColor(context, R.color.grey400transparent))

    init {
        id = R.id.delay_container
        visibility = View.INVISIBLE
        isFocusable = false
        isClickable = false
    }

    fun show(
        title: String,
        value: String,
        firstButtonText: String,
        secondButtonText: String,
        showApplyAll: Boolean,
        showApplyBluetooth: Boolean
    ) {
        state = state.copy(
            title = title,
            value = value,
            firstButtonText = firstButtonText,
            secondButtonText = secondButtonText,
            firstMarked = false,
            secondMarked = false,
            showApplyAll = showApplyAll,
            showApplyBluetooth = showApplyBluetooth
        )
        visibility = View.VISIBLE
    }

    fun updateDelayInfo(title: String, value: String) {
        state = state.copy(title = title, value = value)
    }

    fun updateDelayMarkers(
        firstMarked: Boolean,
        secondMarked: Boolean,
        flashInfo: Boolean
    ) {
        state = state.copy(
            firstMarked = firstMarked,
            secondMarked = secondMarked,
            flashInfoToken = if (flashInfo) state.flashInfoToken + 1 else state.flashInfoToken
        )
    }

    fun hideOverlay() {
        visibility = View.INVISIBLE
    }

    fun requestPlusFocus() = requestFocus(VideoDelayFocusTarget.Plus)

    fun requestFirstButtonFocus() = requestFocus(VideoDelayFocusTarget.FirstButton)

    fun requestSecondButtonFocus() = requestFocus(VideoDelayFocusTarget.SecondButton)

    private fun requestFocus(target: VideoDelayFocusTarget) {
        focusRequest = target to ++focusToken
    }

    @Composable
    override fun WidgetContent() {
        val current = state
        val plusRequester = remember { FocusRequester() }
        val firstRequester = remember { FocusRequester() }
        val secondRequester = remember { FocusRequester() }
        var highlightInfo by remember { mutableStateOf(false) }
        val infoColor by animateColorAsState(
            targetValue = if (highlightInfo) flashColor else panelColor,
            label = "delayInfoColor"
        )

        LaunchedEffect(current.flashInfoToken) {
            if (current.flashInfoToken == 0) return@LaunchedEffect
            highlightInfo = true
            delay(500)
            highlightInfo = false
        }

        LaunchedEffect(focusRequest) {
            when (focusRequest?.first) {
                VideoDelayFocusTarget.Plus -> plusRequester.requestFocus()
                VideoDelayFocusTarget.FirstButton -> firstRequester.requestFocus()
                VideoDelayFocusTarget.SecondButton -> secondRequester.requestFocus()
                null -> Unit
            }
        }

        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 420.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(panelColor, RoundedCornerShape(8.dp))
                    .padding(start = 42.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = current.title,
                    color = orangeColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.Close) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_small),
                        contentDescription = stringResource(R.string.close),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(infoColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                DelayIconButton(
                    icon = R.drawable.ic_down_on_circle_player,
                    contentDescription = stringResource(R.string.talkback_decrease_delay),
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.Decrease) }
                )
                Text(
                    text = current.value,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 10.dp)
                )
                DelayIconButton(
                    icon = R.drawable.ic_up_on_circle_player,
                    contentDescription = stringResource(R.string.talkback_increase_delay),
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.Increase) },
                    modifier = Modifier.focusRequester(plusRequester)
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DelayActionButton(
                    text = current.firstButtonText,
                    marked = current.firstMarked,
                    modifier = Modifier
                        .weight(1F)
                        .focusRequester(firstRequester),
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.MarkStart) }
                )
                DelayActionButton(
                    text = current.secondButtonText,
                    marked = current.secondMarked,
                    modifier = Modifier
                        .weight(1F)
                        .focusRequester(secondRequester),
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.MarkStop) }
                )
                DelayActionButton(
                    text = stringResource(R.string.reset),
                    marked = false,
                    modifier = Modifier.weight(1F),
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.Reset) }
                )
            }

            if (current.showApplyAll) {
                DelayFullWidthButton(
                    text = stringResource(R.string.apply_to_all),
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.ApplyAll) }
                )
            }

            if (current.showApplyBluetooth) {
                DelayFullWidthButton(
                    text = stringResource(R.string.apply_to_bt),
                    onClick = { onAction?.invoke(VideoDelayOverlayAction.ApplyBluetooth) }
                )
            }
        }
    }

    @Composable
    private fun DelayIconButton(
        @DrawableRes icon: Int,
        contentDescription: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .size(44.dp)
                .focusable()
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    @Composable
    private fun DelayActionButton(
        text: String,
        marked: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = panelColor,
                contentColor = Color.White
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delay_done),
                contentDescription = null,
                tint = if (marked) orangeColor else inactiveIconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun DelayFullWidthButton(
        text: String,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = panelColor,
                contentColor = Color.White
            )
        ) {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
