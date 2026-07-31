package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCListItemPosition
import org.videolan.vlc.compose.components.VLCSettingsToggleRow
import org.videolan.vlc.compose.components.segmentShape
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCThemeAccent
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import org.videolan.vlc.compose.theme.availableVLCThemeAccents
import org.videolan.vlc.viewmodel.SettingsViewModel

@Composable
internal fun SettingsOnlyPane(modifier: Modifier, vm: SettingsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    VLCUtilityPane(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VLCThemeDefaults.colors.backgroundDefault,
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = VLCLayout.ScreenGutter,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    AppearanceSettingsGroup(
                        appearance = state.themeAppearance,
                        accent = state.themeAccent,
                        onAppearanceChange = vm::setThemeAppearance,
                        onAccentChange = vm::setThemeAccent,
                    )
                }
                item {
                    SettingsGroup(title = ShellStrings.playback()) {
                        row { ToggleRow(ShellStrings.resumeAudio(), state.audioResume, vm::setAudioResume) }
                        row { ToggleRow(ShellStrings.resumeVideo(), state.videoResume, vm::setVideoResume) }
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
                        row { ToggleRow(ShellStrings.playbackHistory(), state.playbackHistory, vm::setPlaybackHistory) }
                        row { ToggleRow(ShellStrings.incognito(), state.incognito, vm::setIncognito) }
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
                        row { ToggleRow(ShellStrings.videoThumbnails(), state.showVideoThumbs, vm::setShowVideoThumbs) }
                        row { ToggleRow(ShellStrings.showHeaders(), state.showHeaders, vm::setShowHeaders) }
                        row { ToggleRow(ShellStrings.showTrackNumbers(), state.showTrackNumbers, vm::setShowTrackNumbers) }
                    }
                }
                item {
                    SettingsGroup(title = ShellStrings.browser()) {
                        row { ToggleRow(ShellStrings.showHiddenFiles(), state.showHiddenFiles, vm::setShowHiddenFiles) }
                        row { ToggleRow(ShellStrings.multimediaFilesOnly(), state.showOnlyMultimedia, vm::setShowOnlyMultimedia) }
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

/**
 * VLC's shared theme control mirrors QuietGuard's compact, connected appearance group. The
 * choices read as one decision, without the legacy outlined-control treatment fighting the page.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettingsGroup(
    appearance: VLCThemeAppearance,
    accent: VLCThemeAccent,
    onAppearanceChange: (VLCThemeAppearance) -> Unit,
    onAccentChange: (VLCThemeAccent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            ShellStrings.appearance(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = VLCThemeDefaults.colors.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        val modes = listOf(
            VLCThemeAppearance.Light to ShellStrings.lightTheme(),
            VLCThemeAppearance.Dark to ShellStrings.darkTheme(),
            VLCThemeAppearance.System to ShellStrings.systemTheme(),
        )
        // The choices are the group. An additional rounded container around them created a
        // card-inside-card effect that QuietGuard deliberately avoids.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            modes.forEachIndexed { index, (mode, label) ->
                AppearanceModeButton(
                    label = label,
                    selected = appearance == mode,
                    position = index,
                    lastPosition = modes.lastIndex,
                    onClick = { onAppearanceChange(mode) },
                )
            }
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            maxItemsInEachRow = 6,
        ) {
            availableVLCThemeAccents().forEach { option ->
                ThemeAccentSwatch(
                    accent = option,
                    selected = option == accent,
                    onClick = { onAccentChange(option) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.AppearanceModeButton(
    label: String,
    selected: Boolean,
    position: Int,
    lastPosition: Int,
    onClick: () -> Unit,
) {
    val cornerShape = when (position) {
        0 -> RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = 6.dp,
            bottomEnd = 6.dp,
        )
        lastPosition -> RoundedCornerShape(
            topStart = 6.dp,
            bottomStart = 6.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
        )
        else -> RoundedCornerShape(6.dp)
    }
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = cornerShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Icon(
                    icon = MaterialSymbols.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (selected) Modifier.padding(start = 6.dp) else Modifier,
            )
        }
    }
}

@Composable
private fun ThemeAccentSwatch(
    accent: VLCThemeAccent,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val motion = LocalVLCMotion.current
    val swatchColor = if (accent == VLCThemeAccent.Dynamic) MaterialTheme.colorScheme.primary else accent.swatchColor
    val isDynamic = accent == VLCThemeAccent.Dynamic
    val orbCornerFraction by animateFloatAsState(
        targetValue = if (selected) .5f else .26f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = .7f, stiffness = 520f),
        label = "themeOrbCorner_${accent.storageValue}",
    )
    val orbScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else .86f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = .56f, stiffness = 600f),
        label = "themeOrbScale_${accent.storageValue}",
    )
    val orbRotation by animateFloatAsState(
        targetValue = if (selected) 8f else 0f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = .66f, stiffness = 420f),
        label = "themeOrbRotation_${accent.storageValue}",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = if (motion.reducedMotion) 0 else 240, easing = FastOutSlowInEasing),
        label = "themeOrbGlow_${accent.storageValue}",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected || isDynamic) 1f else 0f,
        animationSpec = tween(durationMillis = if (motion.reducedMotion) 0 else 180, easing = FastOutSlowInEasing),
        label = "themeOrbIcon_${accent.storageValue}",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val label = ShellStrings.themeAccent(accent)
    val orbShape = RoundedCornerShape(percent = (orbCornerFraction * 100).toInt())
    val iconTint = if (accent == VLCThemeAccent.Amber || accent == VLCThemeAccent.Lime) Color.Black else Color.White
    Box(
        modifier = Modifier
            .size(50.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { alpha = glowAlpha }
                .drawBehind {
                    drawCircle(
                        color = swatchColor.copy(alpha = .44f),
                        radius = size.minDimension * .5f,
                    )
                },
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = orbScale
                    scaleY = orbScale
                    rotationZ = orbRotation
                }
                .clip(orbShape)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, radius = 18.dp, color = Color.White.copy(alpha = .32f)),
                )
                .background(
                    color = swatchColor,
                    shape = orbShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = .28f), Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(60f, 60f),
                        ),
                    ),
            )
            if (selected || isDynamic) {
                Icon(
                    if (selected) MaterialSymbols.Filled.CheckCircle else MaterialSymbols.Filled.Palette,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = iconAlpha),
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = VLCThemeDefaults.colors.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            scope.rows.forEachIndexed { index, row ->
                val position = when {
                    scope.rows.size == 1 -> VLCListItemPosition.Single
                    index == 0 -> VLCListItemPosition.First
                    index == scope.rows.lastIndex -> VLCListItemPosition.Last
                    else -> VLCListItemPosition.Middle
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = position.segmentShape(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    row()
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    VLCSettingsToggleRow(title = title, checked = checked, onCheckedChange = onChange)
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
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(
            onClick = onDecrease,
            enabled = decreaseEnabled,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        ) { Text("−") }
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 4.dp))
        TextButton(
            onClick = onIncrease,
            enabled = increaseEnabled,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        ) { Text("+") }
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
    title: String,
    subtitle: String,
    playing: Boolean,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    Surface(
        onClick = onExpand,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = colors.fontLight)
                }
            }
            IconButton(onClick = onToggle) {
                Crossfade(
                    targetState = playing,
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
