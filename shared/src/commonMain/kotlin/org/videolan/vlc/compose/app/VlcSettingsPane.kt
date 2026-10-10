package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCSettingsToggleRow
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.player.GenerativeAudioArtwork
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCThemeAccent
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import org.videolan.vlc.compose.theme.availableVLCThemeAccents
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.PlayerUiState

@Composable
internal fun SettingsOnlyPane(modifier: Modifier, vm: SettingsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    VLCUtilityPane(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VLCThemeDefaults.colors.backgroundDefault,
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = VLCLayout.ScreenGutter),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = VLCLayout.ScreenGutter,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    ThemePicker(
                        appearance = state.themeAppearance,
                        accent = state.themeAccent,
                        onAppearanceChange = vm::setThemeAppearance,
                        onAccentChange = vm::setThemeAccent,
                    )
                }
                item {
                    SettingsGroup(title = ShellStrings.playback()) {
                        row { ToggleRow(ShellStrings.resumeAudio(), state.audioResume, vm::setAudioResume, ShellStrings.audioResumeSummary()) }
                        row { ToggleRow(ShellStrings.resumeVideo(), state.videoResume, vm::setVideoResume, ShellStrings.videoResumeSummary()) }
                        row {
                            PlaybackSpeedStepperRow(
                                title = ShellStrings.defaultAudioPlaybackSpeed(),
                                rate = state.defaultAudioPlaybackSpeed,
                                onChange = vm::setDefaultAudioPlaybackSpeed,
                            )
                        }
                        row {
                            PlaybackSpeedStepperRow(
                                title = ShellStrings.defaultVideoPlaybackSpeed(),
                                rate = state.defaultVideoPlaybackSpeed,
                                onChange = vm::setDefaultVideoPlaybackSpeed,
                            )
                        }
                        row { ToggleRow(ShellStrings.playbackHistory(), state.playbackHistory, vm::setPlaybackHistory, ShellStrings.playbackHistorySummary()) }
                        row { ToggleRow(ShellStrings.incognito(), state.incognito, vm::setIncognito, ShellStrings.incognitoSummary()) }
                        row {
                            ValueStepperRow(
                                title = ShellStrings.videoHudTimeout(),
                                value = "${state.videoHudTimeoutSeconds}s",
                                decreaseEnabled = state.videoHudTimeoutSeconds > 1,
                                increaseEnabled = state.videoHudTimeoutSeconds < 10,
                                onDecrease = { vm.setVideoHudTimeout(state.videoHudTimeoutSeconds - 1) },
                                onIncrease = { vm.setVideoHudTimeout(state.videoHudTimeoutSeconds + 1) },
                            )
                        }
                    }
                }
                if (state.appLock.supported) {
                    item {
                        SettingsGroup(title = ShellStrings.privacy()) {
                            row {
                                ToggleRow(
                                    title = ShellStrings.appLock(),
                                    checked = state.appLock.enabled,
                                    onChange = { enabled ->
                                        if (enabled) vm.enableAppLock() else vm.disableAppLock()
                                    },
                                )
                            }
                            row {
                                Text(
                                    ShellStrings.appLockSummary(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VLCThemeDefaults.colors.fontLight,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                )
                            }
                            if (state.appLock.enabled && state.appLock.biometricsAvailable) {
                                row {
                                    ToggleRow(
                                        title = ShellStrings.useBiometrics(),
                                        checked = state.appLock.biometricsEnabled,
                                        onChange = vm::setAppLockBiometrics,
                                    )
                                }
                                row {
                                    Text(
                                        ShellStrings.useBiometricsSummary(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VLCThemeDefaults.colors.fontLight,
                                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    SettingsGroup(title = ShellStrings.library()) {
                        row { ToggleRow(ShellStrings.videoThumbnails(), state.showVideoThumbs, vm::setShowVideoThumbs, ShellStrings.videoThumbnailsSummary()) }
                        row { ToggleRow(ShellStrings.showHeaders(), state.showHeaders, vm::setShowHeaders, ShellStrings.showHeadersSummary()) }
                        row { ToggleRow(ShellStrings.showTrackNumbers(), state.showTrackNumbers, vm::setShowTrackNumbers) }
                    }
                }
                item {
                    SettingsGroup(title = ShellStrings.browser()) {
                        row { ToggleRow(ShellStrings.showHiddenFiles(), state.showHiddenFiles, vm::setShowHiddenFiles, ShellStrings.showHiddenFilesSummary()) }
                        row { ToggleRow(ShellStrings.multimediaFilesOnly(), state.showOnlyMultimedia, vm::setShowOnlyMultimedia, ShellStrings.multimediaFilesOnlySummary()) }
                    }
                }
                if (state.supportsNetworkBrowsing || state.supportsRemoteAccess) {
                    item {
                        val remoteAccessAddress = state.remoteAccessAddress
                        val remoteAccessError = state.remoteAccessError
                        SettingsGroup(title = ShellStrings.network()) {
                            if (state.supportsNetworkBrowsing) {
                                row { ToggleRow(ShellStrings.browseNetwork(), state.browseNetwork, vm::setBrowseNetwork) }
                            }
                            if (state.supportsRemoteAccess) {
                                row { ToggleRow(ShellStrings.remoteAccessServer(), state.remoteAccess, vm::setRemoteAccess) }
                                if (state.remoteAccessStarting || remoteAccessAddress != null || remoteAccessError != null) {
                                    row {
                                        when {
                                            state.remoteAccessStarting -> Text(
                                                ShellStrings.remoteAccessStarting(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                            )
                                            remoteAccessAddress != null -> Column(
                                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Text(
                                                    ShellStrings.remoteAccessUploadAddress(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                Text(
                                                    remoteAccessAddress,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = VLCThemeDefaults.colors.primary,
                                                )
                                            }
                                            remoteAccessError != null -> Text(
                                                remoteAccessError,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                            )
                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class SettingsGroupScope {
    val rows = mutableListOf<@Composable () -> Unit>()
    fun row(content: @Composable () -> Unit) { rows += content }
}

@Composable
private fun SettingsGroup(title: String, content: SettingsGroupScope.() -> Unit) {
    val scope = SettingsGroupScope().apply(content)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column {
                scope.rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f),
                        )
                    }
                    row()
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, summary: String? = null) {
    VLCSettingsToggleRow(title = title, checked = checked, onCheckedChange = onChange, summary = summary)
}

@Composable
private fun ValueStepperRow(
    title: String,
    value: String,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val decreaseDescription = ShellStrings.decrease(title)
    val increaseDescription = ShellStrings.increase(title)
    Row(
        Modifier.fillMaxWidth().heightIn(min = VLCLayout.RowHeight).padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 12.dp))
        // One connected control reads as a single value with two nudges, not three loose glyphs.
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton("−", decreaseEnabled, decreaseDescription, onDecrease)
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.widthIn(min = 44.dp),
                )
                StepperButton("+", increaseEnabled, increaseDescription, onIncrease)
            }
        }
    }
}

@Composable
private fun StepperButton(glyph: String, enabled: Boolean, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) VLCThemeDefaults.colors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
        )
    }
}

private val PlaybackSpeedChoices = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 6f, 8f)

@Composable
private fun PlaybackSpeedStepperRow(title: String, rate: Float, onChange: (Float) -> Unit) {
    val currentIndex = PlaybackSpeedChoices.indexOf(rate).takeIf { it >= 0 }
        ?: PlaybackSpeedChoices.indices.minBy { kotlin.math.abs(PlaybackSpeedChoices[it] - rate) }
    ValueStepperRow(
        title = title,
        value = "${PlaybackSpeedChoices[currentIndex]}×",
        decreaseEnabled = currentIndex > 0,
        increaseEnabled = currentIndex < PlaybackSpeedChoices.lastIndex,
        onDecrease = { onChange(PlaybackSpeedChoices[currentIndex - 1]) },
        onIncrease = { onChange(PlaybackSpeedChoices[currentIndex + 1]) },
    )
}

@Composable
internal fun MiniBar(
    state: PlayerUiState,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    val item = state.queue.getOrNull(state.currentQueueIndex)
    val targetProgress = if (state.progress.length > 0L) {
        (state.progress.time.toFloat() / state.progress.length.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (motion.reducedMotion) snap() else tween(220, easing = VLCMotion.Emphasized),
        label = "mini-player-progress",
    )
    Surface(
        onClick = onExpand,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val progressWidth = size.width * animatedProgress
                    drawRect(
                        color = colors.primary.copy(alpha = 0.12f),
                        topLeft = Offset(0f, size.height - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()),
                    )
                    drawRect(
                        color = colors.primary,
                        topLeft = Offset(0f, size.height - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(progressWidth, 2.dp.toPx()),
                    )
                }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(9.dp)),
                ) {
                    if (item != null) {
                        MediaArtwork(
                            item = item,
                            contentDescription = null,
                            size = 42.dp,
                            modifier = Modifier.fillMaxSize(),
                            fillMaxSizeArtwork = true,
                            fallback = { GenerativeAudioArtwork(item, state.progress) },
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {}
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.title.ifBlank { ShellStrings.notPlaying() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (state.subtitle.isNotBlank()) {
                        Text(
                            state.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (kotlin.math.abs(state.rate - 1f) > 0.001f) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primary.copy(alpha = 0.14f),
                        contentColor = colors.primary,
                    ) {
                        Text(
                            text = org.videolan.vlc.compose.player.playbackRateLabel(state.rate),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                IconButton(onClick = onToggle) {
                    Crossfade(
                        targetState = state.playing,
                        animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
                        label = "mini-player-icon",
                    ) { isPlaying ->
                        Icon(
                            icon = if (isPlaying) MaterialSymbols.Filled.Pause else MaterialSymbols.Filled.PlayArrow,
                            contentDescription = if (isPlaying) ShellStrings.pause() else ShellStrings.play(),
                        )
                    }
                }
            }
        }
    }
}
