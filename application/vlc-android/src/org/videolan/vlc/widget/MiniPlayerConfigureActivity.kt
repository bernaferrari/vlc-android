/*
 * ************************************************************************
 *  MiniPlayerConfigureActivity.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 *
 *
 */

package org.videolan.vlc.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.tools.PREF_WIDGETS_TIPS_SHOWN
import org.videolan.tools.Settings
import org.videolan.tools.WIDGETS_PREVIEW_PLAYING
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.dialogs.showWidgetExplanationComposeDialog
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.bitmapFromView
import org.videolan.vlc.mediadb.models.Widget
import org.videolan.vlc.repository.WidgetRepository
import org.videolan.vlc.widget.utils.WidgetCache
import org.videolan.vlc.widget.utils.WidgetSizeUtil
import org.videolan.vlc.widget.utils.WidgetType
import org.videolan.vlc.widget.utils.WidgetUtils
import org.videolan.vlc.widget.utils.WidgetUtils.getWidgetType
import org.videolan.vlc.widget.utils.WidgetUtils.hasEnoughSpaceForSeek

class MiniPlayerConfigureActivity : BaseActivity() {

    internal lateinit var model: WidgetViewModel
    override val displayTitle = true
    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    override var isEdgeToEdge = false

    private var rootView: ComposeView? = null
    private var widgetState by mutableStateOf<Widget?>(null)
    private var previewBitmap by mutableStateOf<Bitmap?>(null)
    private var previewPlaying by mutableStateOf(true)
    private val widgetRepository by lazy { WidgetRepository.getInstance(this) }

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = rootView ?: window.decorView

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        model = ViewModelProvider(this, WidgetViewModel.Factory(this, appWidgetId))[WidgetViewModel::class.java]
        previewPlaying = Settings.getInstance(this).getBoolean(WIDGETS_PREVIEW_PLAYING, true)

        rootView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCTheme {
                    MiniPlayerConfigureScreen(
                        widget = widgetState,
                        previewBitmap = previewBitmap,
                        previewPlaying = previewPlaying,
                        dynamicColorAvailable = DynamicColors.isDynamicColorAvailable(),
                        colorChoices = widgetColorChoices(),
                        onInfo = { showWidgetExplanationComposeDialog() },
                        onDone = { finish() },
                        onPreviewPlayingChanged = ::updatePreviewPlaying,
                        onThemeChanged = { updateWidget { theme = it } },
                        onTypeChanged = ::setWidgetType,
                        onLightThemeChanged = { updateWidget { lightTheme = it } },
                        onOpacityChanged = { updateWidget { opacity = it } },
                        onBackgroundColorChanged = { updateWidget { backgroundColor = it } },
                        onForegroundColorChanged = { updateWidget { foregroundColor = it } },
                        onShowCoverChanged = { updateWidget { showCover = it } },
                        onShowSeekChanged = { updateWidget { showSeek = it } },
                        onForwardDelayChanged = { updateWidget { forwardDelay = it } },
                        onRewindDelayChanged = { updateWidget { rewindDelay = it } },
                        onShowConfigureChanged = { updateWidget { showConfigure = it } }
                    )
                }
            }
        }
        setContentView(rootView)

        lifecycleScope.launch {
            if (widgetRepository.getWidget(appWidgetId) == null) {
                model.create(this@MiniPlayerConfigureActivity, appWidgetId)
            }
        }

        model.widget.observe(this) { widget ->
            if (widget == null) return@observe
            val nextWidget = widget.copy()
            if (!DynamicColors.isDynamicColorAvailable() && nextWidget.theme == 0) {
                nextWidget.theme = 1
                updateWidgetEntity(nextWidget)
            }
            widgetState = nextWidget
            updatePreview(nextWidget)
        }

        if (!settings.getBoolean(PREF_WIDGETS_TIPS_SHOWN, false)) {
            showWidgetExplanationComposeDialog()
            settings.putSingle(PREF_WIDGETS_TIPS_SHOWN, true)
        }
    }

    private fun updatePreviewPlaying(playing: Boolean) {
        previewPlaying = playing
        Settings.getInstance(this).putSingle(WIDGETS_PREVIEW_PLAYING, playing)
        updatePreview(widgetState)
    }

    private fun setWidgetType(type: Int) {
        updateWidget {
            this.type = type
            val size = WidgetSizeUtil.getWidgetsSize(this@MiniPlayerConfigureActivity, widgetId)
            val minimalSize = WidgetUtils.getMinimalWidgetSize(getWidgetType(this))
            if (size.first < minimalSize.first || size.second < minimalSize.second) {
                UiTools.snackerConfirm(this@MiniPlayerConfigureActivity, getString(R.string.widget_type_error)) { }
            }
        }
    }

    private fun updateWidget(update: Widget.() -> Unit) {
        val current = widgetState ?: return
        val updated = current.copy().apply(update)
        widgetState = updated
        updateWidgetEntity(updated)
        updatePreview(updated)
    }

    private fun updateWidgetEntity(widget: Widget) {
        lifecycleScope.launch {
            widgetRepository.updateWidget(widget)
        }
    }

    private fun updatePreview(widget: Widget?) {
        if (widget == null) return
        lifecycleScope.launch {
            val coverBitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(resources, R.drawable.vlc_fake_cover)
            }
            val palette = withContext(Dispatchers.Default) {
                Palette.from(coverBitmap).generate()
            }
            val preview = buildPreviewBitmap(widget, coverBitmap, palette)
            previewBitmap = preview
        }
    }

    private suspend fun buildPreviewBitmap(widget: Widget, coverBitmap: Bitmap, palette: Palette): Bitmap? {
        val width = if (widget.width <= 0 || widget.height <= 0) 276 else widget.width
        val height = if (widget.width <= 0 || widget.height <= 0) 94 else widget.height
        val state = buildMiniPlayerGlanceState(
            this@MiniPlayerConfigureActivity,
            widget.widgetId,
            true,
            coverBitmap,
            palette,
            previewPlaying = previewPlaying,
        )
        val preview = ComposeView(this@MiniPlayerConfigureActivity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    MiniPlayerWidgetPreviewContent(state)
                }
            }
            measure(
                View.MeasureSpec.makeMeasureSpec(width.toPx(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height.toPx(), View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, width.toPx(), height.toPx())
        }
        return bitmapFromView(preview, width.toPx(), height.toPx())
    }

    private fun Int.toPx() = (this * resources.displayMetrics.density).toInt()

    private fun widgetColorChoices() = listOf(
        ContextCompat.getColor(this, R.color.black),
        ContextCompat.getColor(this, R.color.white),
        ContextCompat.getColor(this, R.color.orange500),
        ContextCompat.getColor(this, R.color.grey300),
        ContextCompat.getColor(this, R.color.grey800)
    )

    override fun finish() {
        onWidgetContainerClicked()
        super.finish()
    }

    private fun onWidgetContainerClicked() {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || !::model.isInitialized) return
        (widgetState ?: model.widget.value)?.let { WidgetCache.clear(it) }

        sendBroadcast(Intent(MiniPlayerAppWidgetProvider.ACTION_WIDGET_INIT).apply {
            component = ComponentName(applicationContext, MiniPlayerAppWidgetProvider::class.java)
        })

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniPlayerConfigureScreen(
    widget: Widget?,
    previewBitmap: Bitmap?,
    previewPlaying: Boolean,
    dynamicColorAvailable: Boolean,
    colorChoices: List<Int>,
    onInfo: () -> Unit,
    onDone: () -> Unit,
    onPreviewPlayingChanged: (Boolean) -> Unit,
    onThemeChanged: (Int) -> Unit,
    onTypeChanged: (Int) -> Unit,
    onLightThemeChanged: (Boolean) -> Unit,
    onOpacityChanged: (Int) -> Unit,
    onBackgroundColorChanged: (Int) -> Unit,
    onForegroundColorChanged: (Int) -> Unit,
    onShowCoverChanged: (Boolean) -> Unit,
    onShowSeekChanged: (Boolean) -> Unit,
    onForwardDelayChanged: (Int) -> Unit,
    onRewindDelayChanged: (Int) -> Unit,
    onShowConfigureChanged: (Boolean) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = colors.backgroundDefault,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.configure_widget),
                        color = colors.fontDefault,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = onInfo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_information),
                            contentDescription = stringResource(R.string.info),
                            tint = colors.fontDefault
                        )
                    }
                    IconButton(onClick = onDone) {
                        Icon(
                            painter = painterResource(R.drawable.ic_done),
                            contentDescription = stringResource(R.string.done),
                            tint = colors.fontDefault
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.backgroundDefault)
            )
        }
    ) { padding ->
        if (widget == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            WidgetConfigureContent(
                widget = widget,
                previewBitmap = previewBitmap,
                previewPlaying = previewPlaying,
                dynamicColorAvailable = dynamicColorAvailable,
                colorChoices = colorChoices,
                padding = padding,
                onPreviewPlayingChanged = onPreviewPlayingChanged,
                onThemeChanged = onThemeChanged,
                onTypeChanged = onTypeChanged,
                onLightThemeChanged = onLightThemeChanged,
                onOpacityChanged = onOpacityChanged,
                onBackgroundColorChanged = onBackgroundColorChanged,
                onForegroundColorChanged = onForegroundColorChanged,
                onShowCoverChanged = onShowCoverChanged,
                onShowSeekChanged = onShowSeekChanged,
                onForwardDelayChanged = onForwardDelayChanged,
                onRewindDelayChanged = onRewindDelayChanged,
                onShowConfigureChanged = onShowConfigureChanged
            )
        }
    }
}

@Composable
private fun WidgetConfigureContent(
    widget: Widget,
    previewBitmap: Bitmap?,
    previewPlaying: Boolean,
    dynamicColorAvailable: Boolean,
    colorChoices: List<Int>,
    padding: PaddingValues,
    onPreviewPlayingChanged: (Boolean) -> Unit,
    onThemeChanged: (Int) -> Unit,
    onTypeChanged: (Int) -> Unit,
    onLightThemeChanged: (Boolean) -> Unit,
    onOpacityChanged: (Int) -> Unit,
    onBackgroundColorChanged: (Int) -> Unit,
    onForegroundColorChanged: (Int) -> Unit,
    onShowCoverChanged: (Boolean) -> Unit,
    onShowSeekChanged: (Boolean) -> Unit,
    onForwardDelayChanged: (Int) -> Unit,
    onRewindDelayChanged: (Int) -> Unit,
    onShowConfigureChanged: (Boolean) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val themeOptions = widgetOptions(
        labels = stringArrayResource(R.array.widget_themes_entries).toList(),
        values = stringArrayResource(R.array.widget_themes_values).map { it.toInt() }
    ).filter { dynamicColorAvailable || it.value != 0 }
    val typeOptions = widgetOptions(
        labels = stringArrayResource(R.array.widget_type_entries).toList(),
        values = stringArrayResource(R.array.widget_type_values).map { it.toInt() }
    )
    val widgetType = getWidgetType(widget)
    val showSeekPrefs = (widgetType == WidgetType.MINI || widgetType == WidgetType.MACRO) && hasEnoughSpaceForSeek(widget, widgetType)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            if (previewBitmap == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.widget_preview)
                )
            }
        }

        OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            SwitchRow(
                title = stringResource(R.string.widget_preview),
                subtitle = stringResource(R.string.playing),
                checked = previewPlaying,
                onCheckedChange = onPreviewPlayingChanged
            )
        }

        Text(
            text = stringResource(R.string.widget_preferences),
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            CycleRow(
                title = stringResource(R.string.widget_theme),
                value = themeOptions.labelFor(widget.theme),
                onNext = { onThemeChanged(themeOptions.nextValue(widget.theme)) }
            )
            CycleRow(
                title = stringResource(R.string.widget_type),
                value = typeOptions.labelFor(widget.type),
                onNext = { onTypeChanged(typeOptions.nextValue(widget.type)) }
            )
            if (widget.theme != 2) {
                SwitchRow(
                    title = stringResource(R.string.light_theme),
                    checked = widget.lightTheme,
                    onCheckedChange = onLightThemeChanged
                )
            }
            SliderRow(
                title = stringResource(R.string.opacity),
                value = widget.opacity,
                range = 0..100,
                onValueChange = onOpacityChanged
            )
            if (widget.theme == 2) {
                ColorRow(
                    title = stringResource(R.string.widget_background),
                    selected = widget.backgroundColor,
                    choices = colorChoices.withSelected(widget.backgroundColor),
                    onSelected = onBackgroundColorChanged
                )
                ColorRow(
                    title = stringResource(R.string.widget_foreground),
                    selected = widget.foregroundColor,
                    choices = colorChoices.withSelected(widget.foregroundColor),
                    onSelected = onForegroundColorChanged
                )
            }
            if (widgetType == WidgetType.MINI) {
                SwitchRow(
                    title = stringResource(R.string.widget_show_cover),
                    checked = widget.showCover,
                    onCheckedChange = onShowCoverChanged
                )
            }
            if (showSeekPrefs) {
                SwitchRow(
                    title = stringResource(R.string.widget_show_seek),
                    checked = widget.showSeek,
                    onCheckedChange = onShowSeekChanged
                )
                if (widget.showSeek) {
                    StepperRow(
                        title = stringResource(R.string.widget_forward_delay),
                        value = widget.forwardDelay,
                        onValueChange = onForwardDelayChanged
                    )
                    StepperRow(
                        title = stringResource(R.string.widget_rewind_delay),
                        value = widget.rewindDelay,
                        onValueChange = onRewindDelayChanged
                    )
                }
            }
            SwitchRow(
                title = stringResource(R.string.widget_show_configure),
                subtitle = stringResource(R.string.widget_show_configure_summary),
                checked = widget.showConfigure,
                onCheckedChange = onShowConfigureChanged
            )
        }
    }
}

@Composable
private fun MiniPlayerWidgetPreviewContent(state: MiniPlayerGlanceState) {
    val cover = state.cover
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(state.backgroundColor))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (state.widgetType == WidgetType.MACRO) 88.dp else 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(state.secondaryBackgroundColor)),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null && state.playing) {
                Image(
                    bitmap = cover.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_widget_icon),
                    contentDescription = null,
                    tint = Color(state.foregroundColor),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                color = Color(state.foregroundColor),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!state.artist.isNullOrBlank()) {
                Text(
                    text = state.artist,
                    color = Color(state.artistColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            painter = painterResource(if (state.playing) R.drawable.ic_widget_pause_inner else R.drawable.ic_widget_play),
            contentDescription = null,
            tint = Color(state.foregroundColor),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun CycleRow(title: String, value: String, onNext: () -> Unit) {
    PreferenceRow(title = title, trailing = {
        TextButton(onClick = onNext) {
            Text(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    })
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    PreferenceRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        onClick = { onCheckedChange(!checked) }
    )
}

@Composable
private fun SliderRow(title: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    val colors = VLCThemeDefaults.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$value%",
                color = colors.fontLight,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun StepperRow(title: String, value: Int, onValueChange: (Int) -> Unit) {
    PreferenceRow(title = title, trailing = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onValueChange((value - 5).coerceAtLeast(5)) }) {
                Text("-")
            }
            Text(
                text = value.toString(),
                color = VLCThemeDefaults.colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(onClick = { onValueChange((value + 5).coerceAtMost(60)) }) {
                Text("+")
            }
        }
    })
}

@Composable
private fun ColorRow(title: String, selected: Int, choices: List<Int>, onSelected: (Int) -> Unit) {
    val colors = VLCThemeDefaults.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            choices.forEach { color ->
                val selectedColor = color == selected
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (selectedColor) 3.dp else 1.dp,
                            color = if (selectedColor) colors.primary else colors.defaultDivider,
                            shape = CircleShape
                        )
                        .clickable { onSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        trailing()
    }
}

private data class WidgetOption(val label: String, val value: Int)

private fun widgetOptions(labels: List<String>, values: List<Int>) = values.mapIndexed { index, value ->
    WidgetOption(labels.getOrElse(index) { value.toString() }, value)
}

private fun List<WidgetOption>.labelFor(value: Int) = firstOrNull { it.value == value }?.label
    ?: firstOrNull()?.label.orEmpty()

private fun List<WidgetOption>.nextValue(current: Int): Int {
    if (isEmpty()) return current
    val index = indexOfFirst { it.value == current }.takeIf { it >= 0 } ?: 0
    return this[(index + 1) % size].value
}

private fun List<Int>.withSelected(selected: Int) = if (contains(selected)) this else listOf(selected) + this
