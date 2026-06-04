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

import android.appwidget.AppWidgetManager
import android.content.SharedPreferences
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.palette.graphics.Palette
import org.videolan.resources.ACTION_REMOTE_BACKWARD
import org.videolan.resources.ACTION_REMOTE_FORWARD
import org.videolan.resources.ACTION_REMOTE_PLAYPAUSE
import org.videolan.resources.ACTION_REMOTE_SEEK_BACKWARD
import org.videolan.resources.ACTION_REMOTE_SEEK_FORWARD
import org.videolan.resources.EXTRA_SEEK_DELAY
import org.videolan.resources.buildPkgString
import org.videolan.tools.AppScope
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_ARTIST
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_THUMB
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_TITLE
import org.videolan.tools.Settings
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.gui.helpers.AudioUtil
import org.videolan.vlc.gui.helpers.BitmapUtil
import org.videolan.vlc.media.Progress
import org.videolan.vlc.mediadb.models.Widget
import org.videolan.vlc.repository.WidgetRepository
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.widget.utils.WidgetCache
import org.videolan.vlc.widget.utils.WidgetCacheEntry
import org.videolan.vlc.widget.utils.WidgetSizeUtil
import org.videolan.vlc.widget.utils.WidgetType
import org.videolan.vlc.widget.utils.WidgetUtils
import org.videolan.vlc.widget.utils.generateCircularProgressbar
import org.videolan.vlc.widget.utils.generatePillProgressbar
import org.videolan.vlc.widget.utils.getArtistColor
import org.videolan.vlc.widget.utils.getBackgroundColor
import org.videolan.vlc.widget.utils.getBackgroundSecondaryColor
import org.videolan.vlc.widget.utils.getForegroundColor
import org.videolan.vlc.widget.utils.getSeparatorColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production Glance implementation for the mini-player App Widget runtime.
 */
object MiniPlayerGlanceAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = (id as? AppWidgetId)?.appWidgetId ?: GlanceAppWidgetManager(context).getAppWidgetId(id)
        val state = buildMiniPlayerGlanceState(context, appWidgetId)
        provideContent {
            MiniPlayerGlanceContent(state)
        }
    }
}

class MiniPlayerAppWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniPlayerGlanceAppWidget

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == null || !action.startsWith(ACTION_WIDGET_PREFIX)) return
        updateWidgets(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgets(context, Intent(ACTION_WIDGET_INIT))
        context.sendBroadcast(Intent(ACTION_WIDGET_INIT).setPackage(context.packageName))
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(context, Intent(ACTION_WIDGET_INIT).putExtra("ID", appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        AppScope.launch {
            val widgetRepository = WidgetRepository.getInstance(context)
            appWidgetIds.forEach { widgetRepository.deleteWidget(it) }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.sendBroadcast(Intent(ACTION_WIDGET_ENABLED, null, context.applicationContext, PlaybackService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.sendBroadcast(Intent(ACTION_WIDGET_DISABLED, null, context.applicationContext, PlaybackService::class.java))
    }

    private fun updateWidgets(context: Context, intent: Intent) {
        AppScope.launch {
            val widgetRepository = WidgetRepository.getInstance(context)
            val manager = GlanceAppWidgetManager(context)
            val extraId = intent.getIntExtra("ID", -1)
            val widgetIds = if (extraId == -1) {
                widgetRepository.getAllWidgets().map { it.widgetId }.filter { it != 0 }
            } else {
                listOf(extraId)
            }
            widgetIds.forEach { appWidgetId ->
                updateWidgetSize(context, widgetRepository, appWidgetId)
                MiniPlayerGlanceAppWidget.update(context, manager.getGlanceIdBy(appWidgetId))
            }
        }
    }

    companion object {
        const val TAG = "VLC/MiniPlayerAppWidgetProvider"
        val ACTION_WIDGET_PREFIX = "widget.mini.".buildPkgString()
        val ACTION_WIDGET_INIT = ACTION_WIDGET_PREFIX + "INIT"
        val ACTION_WIDGET_UPDATE = ACTION_WIDGET_PREFIX + "UPDATE"
        val ACTION_WIDGET_UPDATE_POSITION = ACTION_WIDGET_PREFIX + "UPDATE_POSITION"
        val ACTION_WIDGET_ENABLED = ACTION_WIDGET_PREFIX + "ENABLED"
        val ACTION_WIDGET_DISABLED = ACTION_WIDGET_PREFIX + "DISABLED"
    }
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
            val foreground = ContextCompat.getColor(context, R.color.white)
            return MiniPlayerGlanceState(
                widgetType = WidgetType.MINI,
                title = context.getString(R.string.widget_default_text),
                artist = null,
                playing = false,
                foregroundColor = foreground,
                artistColor = foreground,
                backgroundColor = ContextCompat.getColor(context, R.color.black_transparent_80),
                secondaryBackgroundColor = ContextCompat.getColor(context, R.color.black_transparent_50),
                separatorColor = ContextCompat.getColor(context, R.color.white_transparent_10),
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
            palette: Palette? = null,
        ): MiniPlayerGlanceState {
            val widgetType = WidgetUtils.getWidgetType(widget)
            return MiniPlayerGlanceState(
                widgetType = widgetType,
                title = title ?: context.getString(R.string.widget_default_text),
                artist = artist,
                playing = playing,
                foregroundColor = widget.getForegroundColor(context, palette),
                artistColor = widget.getArtistColor(context, palette),
                backgroundColor = widget.getBackgroundColor(context, palette),
                secondaryBackgroundColor = widget.getBackgroundSecondaryColor(context, palette),
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

suspend fun buildMiniPlayerGlanceState(
    context: Context,
    appWidgetId: Int,
    forPreview: Boolean = false,
    previewBitmap: Bitmap? = null,
    previewPalette: Palette? = null,
    previewPlaying: Boolean = false,
): MiniPlayerGlanceState {
    val widgetRepository = WidgetRepository.getInstance(context)
    val persistedWidget = widgetRepository.getWidget(appWidgetId) ?: return MiniPlayerGlanceState.idle(context)
    val widgetCacheEntry = if (forPreview) {
        WidgetCacheEntry(persistedWidget)
    } else {
        WidgetCache.getEntry(persistedWidget) ?: WidgetCache.addEntry(persistedWidget)
    }
    val service = PlaybackService.serviceFlow.value
    val settings = Settings.getInstance(context)
    val playing = (service?.isPlaying == true && !forPreview && !service.isVideoPlaying) || previewPlaying

    if (!forPreview) widgetCacheEntry.currentMedia = service?.currentMediaWrapper
    val title = when {
        forPreview && !previewPlaying -> context.getString(R.string.widget_default_text)
        forPreview -> "Track name"
        playing -> service?.title ?: widgetCacheEntry.currentMedia?.title
        else -> settings.getString(KEY_CURRENT_AUDIO_RESUME_TITLE, context.getString(R.string.widget_default_text))
    }
    val artist = when {
        forPreview && previewPlaying -> "Artist name"
        forPreview -> null
        playing -> service?.artist ?: widgetCacheEntry.currentMedia?.artistName
        else -> settings.getString(KEY_CURRENT_AUDIO_RESUME_ARTIST, "")
    }

    val coverSource = when {
        forPreview -> previewBitmap
        else -> loadWidgetCover(context, service, settings, widgetCacheEntry)
    }
    val palette = previewPalette ?: coverSource?.let { Palette.from(it).generate() }
    widgetCacheEntry.palette = palette
    val widgetType = WidgetUtils.getWidgetType(widgetCacheEntry.widget)
    val cover = coverSource?.let { cutBitmapCover(context, widgetType, it, widgetCacheEntry) }
    val progress = (service?.playlistManager?.player?.progress?.value ?: if (forPreview && previewPlaying) Progress(3333L, 10000L) else null)
        ?.let { progress ->
            val pos = if (progress.length > 0) progress.time.toFloat() / progress.length else 0F
            when (widgetType) {
                WidgetType.PILL -> widgetCacheEntry.generatePillProgressbar(context, pos)
                WidgetType.MICRO -> widgetCacheEntry.generateCircularProgressbar(context, context.dpToPxFloat(128), pos)
                WidgetType.MINI, WidgetType.MACRO -> widgetCacheEntry.generateCircularProgressbar(context, context.dpToPxFloat(32), pos, context.dpToPxFloat(3))
            }
        }
    widgetCacheEntry.playing = playing
    return MiniPlayerGlanceState.fromWidget(context, persistedWidget, title, artist, playing, cover, progress, palette)
}

private suspend fun updateWidgetSize(context: Context, widgetRepository: WidgetRepository, appWidgetId: Int) {
    val widget = widgetRepository.getWidget(appWidgetId) ?: return
    val size = WidgetSizeUtil.getWidgetsSize(context, appWidgetId)
    if (size.first != 0 && size.second != 0 && (widget.width != size.first || widget.height != size.second)) {
        widget.width = size.first
        widget.height = size.second
        widgetRepository.updateWidget(widget, true)
    }
}

private suspend fun loadWidgetCover(
    context: Context,
    service: PlaybackService?,
    settings: SharedPreferences,
    widgetCacheEntry: WidgetCacheEntry,
): Bitmap? = withContext(Dispatchers.IO) {
    val coverMrl = if (service?.isPlaying == true && !service.isVideoPlaying) {
        widgetCacheEntry.currentMedia?.artworkMrl ?: settings.getString(KEY_CURRENT_AUDIO_RESUME_THUMB, null)
    } else {
        settings.getString(KEY_CURRENT_AUDIO_RESUME_THUMB, null)
    }
    if (coverMrl.isNullOrEmpty()) {
        widgetCacheEntry.currentCover = null
        null
    } else {
        widgetCacheEntry.currentCover = coverMrl
        AudioUtil.readCoverBitmap(Uri.decode(coverMrl), 320)
    }
}

private fun cutBitmapCover(context: Context, widgetType: WidgetType, cover: Bitmap, widgetCacheEntry: WidgetCacheEntry): Bitmap =
    when (widgetType) {
        WidgetType.MICRO -> BitmapUtil.roundBitmap(cover)
        WidgetType.PILL -> BitmapUtil.roundBitmap(cover)
        WidgetType.MINI -> BitmapUtil.roundedRectangleBitmap(cover, context.dpToPx(widgetCacheEntry.widget.height), bottomRight = false, topRight = false)
        WidgetType.MACRO -> BitmapUtil.roundedRectangleBitmap(cover, context.dpToPx(widgetCacheEntry.widget.width))
    }

private fun Context.dpToPx(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun Context.dpToPxFloat(value: Int): Float = value * resources.displayMetrics.density

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
