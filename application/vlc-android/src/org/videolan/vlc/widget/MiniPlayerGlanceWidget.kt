/*
 * ************************************************************************
 *  MiniPlayerGlanceWidget.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * **************************************************************************
 */

package org.videolan.vlc.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.videolan.resources.ACTION_REMOTE_BACKWARD
import org.videolan.resources.ACTION_REMOTE_FORWARD
import org.videolan.resources.ACTION_REMOTE_PLAYPAUSE
import org.videolan.resources.ACTION_REMOTE_SEEK_BACKWARD
import org.videolan.resources.ACTION_REMOTE_SEEK_FORWARD
import org.videolan.resources.EXTRA_SEEK_DELAY
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.mediadb.models.Widget
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.widget.utils.WidgetType
import org.videolan.vlc.widget.utils.WidgetUtils
import org.videolan.vlc.widget.utils.getArtistColor
import org.videolan.vlc.widget.utils.getBackgroundColor
import org.videolan.vlc.widget.utils.getBackgroundSecondaryColor
import org.videolan.vlc.widget.utils.getForegroundColor
import org.videolan.vlc.widget.utils.getSeparatorColor

/**
 * Glance prototype for the mini-player App Widget runtime.
 *
 * Glance still renders through the platform App Widget host, but this moves the
 * widget UI declaration from layout XML into Compose-style Kotlin. The current
 * manifest keeps [MiniPlayerAppWidgetProvider] active until state persistence,
 * preview rendering, and legacy migration are wired to this implementation.
 */
class MiniPlayerGlanceAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MiniPlayerGlanceContent(MiniPlayerGlanceState.idle(context))
        }
    }
}

class MiniPlayerGlanceAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniPlayerGlanceAppWidget()
}

data class MiniPlayerGlanceState(
    val widgetType: WidgetType,
    val title: String,
    val artist: String?,
    val playing: Boolean,
    val foregroundColor: Int,
    val artistColor: Int,
    val backgroundColor: Int,
    val secondaryBackgroundColor: Int,
    val separatorColor: Int,
    val cover: Bitmap?,
    val progress: Bitmap?,
    val showSeek: Boolean,
    val showConfigure: Boolean,
    val forwardDelay: Int,
    val rewindDelay: Int,
) {
    companion object {
        fun idle(context: Context): MiniPlayerGlanceState {
            val foreground = context.getColor(R.color.white)
            return MiniPlayerGlanceState(
                widgetType = WidgetType.MINI,
                title = context.getString(R.string.widget_default_text),
                artist = null,
                playing = false,
                foregroundColor = foreground,
                artistColor = foreground,
                backgroundColor = context.getColor(R.color.black_transparent_80),
                secondaryBackgroundColor = context.getColor(R.color.black_transparent_50),
                separatorColor = context.getColor(R.color.white_transparent_10),
                cover = null,
                progress = null,
                showSeek = false,
                showConfigure = false,
                forwardDelay = 10,
                rewindDelay = 10,
            )
        }

        fun fromWidget(
            context: Context,
            widget: Widget,
            title: String?,
            artist: String?,
            playing: Boolean,
            cover: Bitmap?,
            progress: Bitmap?,
        ): MiniPlayerGlanceState {
            val widgetType = WidgetUtils.getWidgetType(widget)
            return MiniPlayerGlanceState(
                widgetType = widgetType,
                title = title ?: context.getString(R.string.widget_default_text),
                artist = artist,
                playing = playing,
                foregroundColor = widget.getForegroundColor(context, null),
                artistColor = widget.getArtistColor(context, null),
                backgroundColor = widget.getBackgroundColor(context, null),
                secondaryBackgroundColor = widget.getBackgroundSecondaryColor(context, null),
                separatorColor = widget.getSeparatorColor(context),
                cover = cover,
                progress = progress,
                showSeek = WidgetUtils.shouldShowSeek(widget, widgetType),
                showConfigure = widget.showConfigure,
                forwardDelay = widget.forwardDelay,
                rewindDelay = widget.rewindDelay,
            )
        }
    }
}

@Composable
fun MiniPlayerGlanceContent(state: MiniPlayerGlanceState) {
    val context = LocalContext.current
    val openVlcAction = actionStartActivity(Intent(context, StartActivity::class.java))
    when (state.widgetType) {
        WidgetType.PILL -> MiniPlayerPillGlanceContent(context, state, openVlcAction)
        WidgetType.MINI -> MiniPlayerMiniGlanceContent(context, state, openVlcAction)
        WidgetType.MICRO -> MiniPlayerMicroGlanceContent(context, state, openVlcAction)
        WidgetType.MACRO -> MiniPlayerMacroGlanceContent(context, state, openVlcAction)
    }
}

@Composable
private fun MiniPlayerPillGlanceContent(context: Context, state: MiniPlayerGlanceState, openVlcAction: Action) {
    Row(
        modifier = playerModifier(state, openVlcAction).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverOrIcon(state, sizeDp = 36)
        Spacer(GlanceModifier.width(8.dp))
        TrackText(state, modifier = GlanceModifier.defaultWeight())
        WidgetActionButton(
            icon = playPauseIcon(state.playing),
            contentDescription = playPauseDescription(state.playing),
            tint = state.foregroundColor,
            action = playbackAction(context, ACTION_REMOTE_PLAYPAUSE),
        )
    }
}

@Composable
private fun MiniPlayerMiniGlanceContent(context: Context, state: MiniPlayerGlanceState, openVlcAction: Action) {
    Row(
        modifier = playerModifier(state, openVlcAction).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverOrIcon(state, sizeDp = 52)
        Spacer(GlanceModifier.width(10.dp))
        TrackText(state, modifier = GlanceModifier.defaultWeight())
        TransportControls(context, state)
    }
}

@Composable
private fun MiniPlayerMicroGlanceContent(context: Context, state: MiniPlayerGlanceState, openVlcAction: Action) {
    Column(
        modifier = playerModifier(state, openVlcAction).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverOrIcon(state, sizeDp = 56)
        Spacer(GlanceModifier.height(8.dp))
        TrackText(state, centered = true, modifier = GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(6.dp))
        WidgetActionButton(
            icon = playPauseIcon(state.playing),
            contentDescription = playPauseDescription(state.playing),
            tint = state.foregroundColor,
            action = playbackAction(context, ACTION_REMOTE_PLAYPAUSE),
        )
    }
}

@Composable
private fun MiniPlayerMacroGlanceContent(context: Context, state: MiniPlayerGlanceState, openVlcAction: Action) {
    Column(
        modifier = playerModifier(state, openVlcAction).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverOrIcon(state, sizeDp = 96)
        Spacer(GlanceModifier.height(8.dp))
        TrackText(state, centered = true, modifier = GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(8.dp))
        TransportControls(context, state)
    }
}

@Composable
private fun TransportControls(context: Context, state: MiniPlayerGlanceState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.showSeek) {
            WidgetActionButton(
                icon = R.drawable.ic_widget_rewind_10,
                contentDescription = state.rewindDelay.toString(),
                tint = state.foregroundColor,
                action = playbackAction(context, ACTION_REMOTE_SEEK_BACKWARD, state.rewindDelay),
            )
        } else {
            WidgetActionButton(
                icon = R.drawable.ic_widget_previous_normal,
                contentDescription = null,
                tint = state.foregroundColor,
                action = playbackAction(context, ACTION_REMOTE_BACKWARD),
            )
        }
        WidgetActionButton(
            icon = playPauseIcon(state.playing),
            contentDescription = playPauseDescription(state.playing),
            tint = state.foregroundColor,
            action = playbackAction(context, ACTION_REMOTE_PLAYPAUSE),
        )
        if (state.showSeek) {
            WidgetActionButton(
                icon = R.drawable.ic_widget_forward_10,
                contentDescription = state.forwardDelay.toString(),
                tint = state.foregroundColor,
                action = playbackAction(context, ACTION_REMOTE_SEEK_FORWARD, state.forwardDelay),
            )
        } else {
            WidgetActionButton(
                icon = R.drawable.ic_widget_next_normal,
                contentDescription = null,
                tint = state.foregroundColor,
                action = playbackAction(context, ACTION_REMOTE_FORWARD),
            )
        }
    }
}

@Composable
private fun TrackText(
    state: MiniPlayerGlanceState,
    modifier: GlanceModifier = GlanceModifier,
    centered: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = state.title,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(state.foregroundColor),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        if (!state.artist.isNullOrBlank()) {
            Text(
                text = "${if (state.widgetType == WidgetType.MACRO || centered) "" else " ${TextUtils.SEPARATOR} "}${state.artist}",
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(state.artistColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
    }
}

@Composable
private fun CoverOrIcon(state: MiniPlayerGlanceState, sizeDp: Int) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .background(ColorProvider(state.secondaryBackgroundColor)),
        contentAlignment = Alignment.Center,
    ) {
        val cover = state.cover
        if (cover != null && state.playing) {
            Image(
                provider = ImageProvider(cover),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_icon),
                contentDescription = null,
                modifier = GlanceModifier.size((sizeDp / 2).dp),
                colorFilter = ColorFilter.tint(ColorProvider(state.foregroundColor)),
            )
        }
        state.progress?.let {
            Image(
                provider = ImageProvider(it),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun WidgetActionButton(
    icon: Int,
    contentDescription: String?,
    tint: Int,
    action: Action,
) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(24.dp),
            colorFilter = ColorFilter.tint(ColorProvider(tint)),
        )
    }
}

private fun playerModifier(state: MiniPlayerGlanceState, openVlcAction: Action): GlanceModifier =
    GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(state.backgroundColor))
        .clickable(openVlcAction)

private fun playbackAction(context: Context, action: String, seekDelay: Int? = null): Action =
    actionStartService(
        Intent(context, PlaybackService::class.java).apply {
            this.action = action
            seekDelay?.let { putExtra(EXTRA_SEEK_DELAY, it.toLong()) }
        },
        false,
    )

private fun playPauseIcon(playing: Boolean): Int =
    if (playing) R.drawable.ic_widget_pause_inner else R.drawable.ic_widget_play

private fun playPauseDescription(playing: Boolean): String? =
    if (playing) "Pause" else "Play"
