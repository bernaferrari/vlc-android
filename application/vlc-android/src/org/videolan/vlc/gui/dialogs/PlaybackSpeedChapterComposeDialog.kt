package org.videolan.vlc.gui.dialogs

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.KEY_INCOGNITO_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE
import org.videolan.tools.KEY_INCOGNITO_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE
import org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL
import org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE
import org.videolan.tools.Settings
import org.videolan.tools.formatRateString
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.util.isSchemeStreaming
import kotlin.math.ln
import kotlin.math.pow

fun FragmentActivity.showPlaybackSpeedComposeDialog(onDismiss: (() -> Unit)? = null) {
    PlaybackSpeedComposeDialog(this, onDismiss).show()
}

fun FragmentActivity.showSelectChapterComposeDialog(onDismiss: (() -> Unit)? = null) {
    SelectChapterComposeDialog(this, onDismiss).show()
}

private class PlaybackSpeedComposeDialog(
    activity: FragmentActivity,
    onDismiss: (() -> Unit)? = null
) : PlaybackComposeBottomSheetDialog(activity = activity, onDismiss = onDismiss) {
    private val settings: SharedPreferences = Settings.getInstance(activity)
    private val forVideo: Boolean
        get() = PlaylistManager.showAudioPlayer.value == false
    private var rate by mutableFloatStateOf(PlaybackService.instance?.rate ?: 1F)

    override fun onServiceAvailable(service: PlaybackService) {
        rate = service.rate
    }

    override fun onMediaChanged(service: PlaybackService) {
        rate = service.rate
    }

    private fun changeSpeedTo(newValue: Float) {
        val currentService = service ?: return
        if (newValue > 8.0F || newValue < 0.25F) return
        if (isGlobalSpeedMode()) {
            if (settings.getBoolean(KEY_INCOGNITO, false)) {
                settings.edit {
                    putFloat(if (forVideo) KEY_INCOGNITO_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE else KEY_INCOGNITO_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, newValue)
                }
            } else {
                settings.edit {
                    putFloat(if (forVideo) KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE else KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, newValue)
                }
            }
        } else {
            getCurrentMedia()?.setStringMeta(MediaWrapper.META_SPEED, newValue.toString())
        }
        currentService.setRate(newValue, true)
        rate = newValue
    }

    private fun selectGlobalMode(global: Boolean) {
        settings.edit(commit = true) {
            putBoolean(if (forVideo) KEY_PLAYBACK_SPEED_VIDEO_GLOBAL else KEY_PLAYBACK_SPEED_AUDIO_GLOBAL, global)
        }
        val newValue = if (global) {
            when {
                settings.getBoolean(KEY_INCOGNITO, false) -> settings.getFloat(if (forVideo) KEY_INCOGNITO_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE else KEY_INCOGNITO_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, 1F)
                else -> settings.getFloat(if (forVideo) KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE else KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, 1F)
            }
        } else {
            getCurrentMedia()?.getMetaString(MediaWrapper.META_SPEED)?.toFloatOrNull() ?: 1F
        }
        changeSpeedTo(newValue)
    }

    private fun isGlobalSpeedMode(): Boolean {
        return if (forVideo) {
            settings.getBoolean(KEY_PLAYBACK_SPEED_VIDEO_GLOBAL, false)
        } else {
            settings.getBoolean(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL, false)
        }
    }

    private fun getCurrentMedia(): MediaWrapper? {
        PlaylistManager.currentPlayedMedia.value?.let {
            if (it.id > 0) return it
            return service?.medialibrary?.getMedia(it.uri)
        }
        return null
    }

    private fun explanationText(globalMode: Boolean): String {
        return when {
            globalMode && settings.getBoolean(KEY_INCOGNITO, false) -> buildString {
                append(if (forVideo) activity.getString(R.string.playback_speed_explanation_all_videos) else activity.getString(R.string.playback_speed_explanation_all_tracks))
                append("\n\n")
                append(activity.getString(R.string.playback_speed_explanation_all_incognito))
            }
            globalMode -> if (forVideo) activity.getString(R.string.playback_speed_explanation_all_videos) else activity.getString(R.string.playback_speed_explanation_all_tracks)
            else -> if (forVideo) activity.getString(R.string.playback_speed_explanation_one_video) else activity.getString(R.string.playback_speed_explanation_one_track)
        }
    }

    @Composable
    override fun Content() {
        var globalMode by rememberSaveable { mutableStateOf(isGlobalSpeedMode()) }
        val colors = VLCThemeDefaults.colors
        val sliderProgress = remember(rate) { rateToProgress(rate) }

        Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = activity.getString(R.string.playback_speed),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpeedModeButton(
                        text = activity.getString(if (forVideo) R.string.playback_speed_this_video else R.string.playback_speed_this_track),
                        selected = !globalMode,
                        onClick = {
                            globalMode = false
                            selectGlobalMode(false)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SpeedModeButton(
                        text = activity.getString(if (forVideo) R.string.playback_speed_all_videos else R.string.playback_speed_all_tracks),
                        selected = globalMode,
                        onClick = {
                            globalMode = true
                            selectGlobalMode(true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = explanationText(globalMode),
                    color = colors.fontDefault,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    IconButton(onClick = { changeSpeedTo(rate - 0.01F) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_left),
                            contentDescription = activity.getString(R.string.talkback_decrease_speed),
                            tint = colors.fontDefault
                        )
                    }
                    TextButton(onClick = { changeSpeedTo(1F) }) {
                        Text(
                            text = rate.formatRateString(),
                            color = if (rate != 1F) colors.primary else colors.fontDefault,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(96.dp)
                        )
                    }
                    IconButton(onClick = { changeSpeedTo(rate + 0.01F) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = activity.getString(R.string.talkback_increase_speed),
                            tint = colors.fontDefault
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("0.25", color = colors.fontLight, fontSize = 12.sp)
                    Slider(
                        value = sliderProgress,
                        onValueChange = { changeSpeedTo(progressToRate(it)) },
                        valueRange = 0F..200F,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text("8.00", color = colors.fontLight, fontSize = 12.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    listOf("0.8" to 0.8F, "1.0" to 1F, "1.25" to 1.25F, "1.5" to 1.5F, "2.0" to 2F).forEach { (label, value) ->
                        PresetSpeedButton(
                            label = label,
                            onClick = { changeSpeedTo(value) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
                if (isSchemeStreaming(service?.currentMediaLocation) && rate > 1F) {
                    Text(
                        text = activity.getString(R.string.warning_stream_speed),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(colors.primary, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

private class SelectChapterComposeDialog(
    activity: FragmentActivity,
    onDismiss: (() -> Unit)? = null
) : PlaybackComposeBottomSheetDialog(activity = activity, onDismiss = onDismiss) {
    private var chapters by mutableStateOf<List<ChapterItem>>(emptyList())
    private var selectedIndex by mutableStateOf(0)

    override fun onServiceAvailable(service: PlaybackService) {
        updateChapters(service)
    }

    override fun onMediaChanged(service: PlaybackService) {
        updateChapters(service)
    }

    private fun updateChapters(service: PlaybackService) {
        val serviceChapters = service.getChapters(-1)
        if (serviceChapters == null || serviceChapters.size <= 1) {
            dismiss()
            return
        }
        chapters = serviceChapters.mapIndexed { index, chapter ->
            ChapterItem(
                name = TextUtils.formatChapterTitle(activity, index + 1, chapter.name),
                time = Tools.millisToString(chapter.timeOffset)
            )
        }
        selectedIndex = service.chapterIdx.coerceIn(0, chapters.lastIndex)
    }

    @Composable
    override fun Content() {
        val colors = VLCThemeDefaults.colors
        val listState = rememberLazyListState()
        LaunchedEffect(chapters, selectedIndex) {
            if (chapters.isNotEmpty()) listState.scrollToItem(selectedIndex)
        }

        Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = activity.getString(R.string.go_to_chapter),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        ChapterRow(
                            chapter = chapter,
                            selected = index == selectedIndex,
                            onClick = {
                                service?.chapterIdx = index
                                dismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetSpeedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .border(1.dp, colors.defaultDivider, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .focusable()
    ) {
        Text(
            text = label,
            color = colors.fontDefault,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun SpeedModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (selected) colors.subtleSelection else Color.Transparent)
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = chapter.name,
            color = if (selected) colors.primary else colors.fontDefault,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = chapter.time,
            color = colors.fontLight,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private data class ChapterItem(
    val name: String,
    val time: String
)

private fun rateToProgress(rate: Float): Float {
    val coef = if (rate < 1F) 4.0 else 8.0
    return (100 * (1 + ln(rate.toDouble()) / ln(coef))).toFloat().coerceIn(0F, 200F)
}

private fun progressToRate(progress: Float): Float {
    val coef = if (progress < 100F) 4.0 else 8.0
    return coef.pow(progress / 100.0 - 1).toFloat().coerceIn(0.25F, 8F)
}
