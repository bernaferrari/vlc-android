package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import kotlin.math.roundToInt

data class VLCEqualizerBand(
    val index: Int,
    val label: String,
    val value: Float
)

data class VLCEqualizerPreset(
    val id: Long,
    val name: String,
    val presetIndex: Int,
    val isDisabled: Boolean,
    val isCurrent: Boolean,
    val preamp: Float,
    val bands: List<VLCEqualizerBand>
) {
    val isCustom: Boolean get() = presetIndex == -1
}

data class VLCEqualizerOverwriteState(
    val name: String,
    val warning: String,
    val label: String,
    val error: String?,
    val confirmText: String,
    val confirmEnabled: Boolean
)

data class VLCEqualizerEditorState(
    val presets: List<VLCEqualizerPreset>,
    val current: VLCEqualizerPreset,
    val equalizerEnabled: Boolean,
    val snapBands: Boolean,
    val canEditCurrent: Boolean,
    val canUndo: Boolean,
    val editedName: String,
    val nameError: String?
)

data class VLCEqualizerSettingsStrings(
    val title: String,
    val preferences: String,
    val close: String,
    val showEqualizer: String,
    val importEqualizer: String,
    val moreActions: String,
    val showAll: String,
    val hideAll: String,
    val exportAll: String,
    val importAll: String,
    val enablePreset: String,
    val disablePreset: String,
    val delete: String,
    val exportEqualizer: String,
    val equalizerName: String,
    val cancel: String,
    val enableEqualizer: String,
    val addEqualizer: String,
    val editPreset: String,
    val undo: String,
    val preamp: String,
    val snapBands: String,
    val done: String
)

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/equalizer_settings_activity.xml
 * - application/vlc-android/res/layout/equalizer_setting_item.xml
 *
 * EqualizerSettingsActivity keeps ownership of Room-backed presets,
 * import/export file flows, snackbars, and playback updates. This screen owns
 * the toolbar, preset list, overwrite form, and Activity-hosted equalizer editor
 * so the settings screen opens its main controls directly from Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLCEqualizerSettingsScreen(
    presets: List<VLCEqualizerPreset>,
    editorState: VLCEqualizerEditorState?,
    overwriteState: VLCEqualizerOverwriteState?,
    strings: VLCEqualizerSettingsStrings,
    scrollToTopSignal: Int,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    onImport: () -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onExportAll: () -> Unit,
    onImportAll: () -> Unit,
    onSelectPreset: (Long) -> Unit,
    onEnablePreset: (Long) -> Unit,
    onDisablePreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onExportPreset: (Long) -> Unit,
    onOverwriteNameChange: (String) -> Unit,
    onCancelOverwrite: () -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismissEditor: () -> Unit,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onAddEqualizer: () -> Unit,
    onSelectEditorPreset: (Long) -> Unit,
    onEditCurrentPreset: () -> Unit,
    onUndo: () -> Unit,
    onDeleteCurrent: () -> Unit,
    onEditorNameChange: (String) -> Unit,
    onEditorNameFocusChange: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onBandChangeFinished: () -> Unit,
    onSnapBandsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    closeIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    equalizerIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    importIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    overflowIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    enableIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    disableIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    deleteIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    exportIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    editIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    undoIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    addIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    currentIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val listState = rememberLazyListState()

        LaunchedEffect(scrollToTopSignal) {
            if (scrollToTopSignal > 0) listState.scrollToItem(0)
        }

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                EqualizerTopBar(
                    strings = strings,
                    onBack = onBack,
                    onOpenEditor = onOpenEditor,
                    onImport = onImport,
                    onShowAll = onShowAll,
                    onHideAll = onHideAll,
                    onExportAll = onExportAll,
                    onImportAll = onImportAll,
                    closeIconContent = closeIconContent,
                    equalizerIconContent = equalizerIconContent,
                    importIconContent = importIconContent,
                    overflowIconContent = overflowIconContent
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.backgroundDefault),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp)
                ) {
                    if (overwriteState != null) {
                        item(key = "overwrite") {
                            EqualizerOverwriteCard(
                                state = overwriteState,
                                strings = strings,
                                onNameChange = onOverwriteNameChange,
                                onCancel = onCancelOverwrite,
                                onConfirm = onConfirmOverwrite
                            )
                        }
                    }

                    items(
                        items = presets,
                        key = { it.id }
                    ) { preset ->
                        EqualizerPresetRow(
                            preset = preset,
                            strings = strings,
                            onSelect = { onSelectPreset(preset.id) },
                            onEnable = { onEnablePreset(preset.id) },
                            onDisable = { onDisablePreset(preset.id) },
                            onDelete = { onDeletePreset(preset.id) },
                            onExport = { onExportPreset(preset.id) },
                            enableIconContent = enableIconContent,
                            disableIconContent = disableIconContent,
                            deleteIconContent = deleteIconContent,
                            exportIconContent = exportIconContent,
                            currentIconContent = currentIconContent
                        )
                    }
                }
            }
        }

        if (editorState != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissEditor,
                sheetState = sheetState,
                containerColor = colors.backgroundDefault,
                contentColor = colors.fontDefault
            ) {
                EqualizerEditorSheet(
                    state = editorState,
                    strings = strings,
                    onDismiss = onDismissEditor,
                    onEqualizerEnabledChange = onEqualizerEnabledChange,
                    onAddEqualizer = onAddEqualizer,
                    onSelectPreset = onSelectEditorPreset,
                    onEditPreset = onEditCurrentPreset,
                    onUndo = onUndo,
                    onDelete = onDeleteCurrent,
                    onNameChange = onEditorNameChange,
                    onNameFocusChange = onEditorNameFocusChange,
                    onPreampChange = onPreampChange,
                    onBandChange = onBandChange,
                    onBandChangeFinished = onBandChangeFinished,
                    onSnapBandsChange = onSnapBandsChange,
                    addIconContent = addIconContent,
                    editIconContent = editIconContent,
                    undoIconContent = undoIconContent,
                    deleteIconContent = deleteIconContent
                )
            }
        }
    }
}

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_equalizer.xml
 * - application/vlc-android/res/layout/equalizer_bar.xml
 *
 * The app module owns the BottomSheetDialog host and EqualizerViewModel wiring;
 * this content owns the player-facing equalizer controls without requiring a
 * legacy XML binding.
 */
@Composable
fun VLCEqualizerEditorDialogContent(
    state: VLCEqualizerEditorState,
    strings: VLCEqualizerSettingsStrings,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onAddEqualizer: () -> Unit,
    onSelectPreset: (Long) -> Unit,
    onEditPreset: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameFocusChange: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onBandChangeFinished: () -> Unit,
    onSnapBandsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    addIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    settingsIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    editIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    undoIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() },
    deleteIconContent: @Composable () -> Unit = { EqualizerIconPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            EqualizerEditorSheet(
                state = state,
                strings = strings,
                onDismiss = onDismiss,
                onOpenSettings = onOpenSettings,
                onEqualizerEnabledChange = onEqualizerEnabledChange,
                onAddEqualizer = onAddEqualizer,
                onSelectPreset = onSelectPreset,
                onEditPreset = onEditPreset,
                onUndo = onUndo,
                onDelete = onDelete,
                onNameChange = onNameChange,
                onNameFocusChange = onNameFocusChange,
                onPreampChange = onPreampChange,
                onBandChange = onBandChange,
                onBandChangeFinished = onBandChangeFinished,
                onSnapBandsChange = onSnapBandsChange,
                addIconContent = addIconContent,
                settingsIconContent = settingsIconContent,
                editIconContent = editIconContent,
                undoIconContent = undoIconContent,
                deleteIconContent = deleteIconContent
            )
        }
    }
}

@Composable
private fun EqualizerTopBar(
    strings: VLCEqualizerSettingsStrings,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    onImport: () -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onExportAll: () -> Unit,
    onImportAll: () -> Unit,
    closeIconContent: @Composable () -> Unit,
    equalizerIconContent: @Composable () -> Unit,
    importIconContent: @Composable () -> Unit,
    overflowIconContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.backgroundDefault)
            .padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EqualizerIconButton(
            contentDescription = strings.close,
            onClick = onBack,
            iconContent = closeIconContent
        )

        Text(
            text = strings.title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        EqualizerIconButton(
            contentDescription = strings.showEqualizer,
            onClick = onOpenEditor,
            iconContent = equalizerIconContent
        )
        EqualizerIconButton(
            contentDescription = strings.importEqualizer,
            onClick = onImport,
            iconContent = importIconContent
        )

        Box {
            EqualizerIconButton(
                contentDescription = strings.moreActions,
                onClick = { menuExpanded = true },
                iconContent = overflowIconContent
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(strings.showAll) },
                    onClick = {
                        menuExpanded = false
                        onShowAll()
                    }
                )
                DropdownMenuItem(
                    text = { Text(strings.hideAll) },
                    onClick = {
                        menuExpanded = false
                        onHideAll()
                    }
                )
                DropdownMenuItem(
                    text = { Text(strings.exportAll) },
                    onClick = {
                        menuExpanded = false
                        onExportAll()
                    }
                )
                DropdownMenuItem(
                    text = { Text(strings.importAll) },
                    onClick = {
                        menuExpanded = false
                        onImportAll()
                    }
                )
            }
        }
    }
}

@Composable
private fun EqualizerPresetRow(
    preset: VLCEqualizerPreset,
    strings: VLCEqualizerSettingsStrings,
    onSelect: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    enableIconContent: @Composable () -> Unit,
    disableIconContent: @Composable () -> Unit,
    deleteIconContent: @Composable () -> Unit,
    exportIconContent: @Composable () -> Unit,
    currentIconContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val enabledForSelection = !preset.isDisabled && !preset.isCurrent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (preset.isCurrent) colors.subtleSelection else Color.Transparent)
            .clickable(
                enabled = enabledForSelection,
                role = Role.Button,
                onClick = onSelect
            )
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            EqualizerPreviewIcon(
                bands = preset.bands,
                isCustom = preset.isCustom,
                modifier = Modifier.fillMaxSize()
            )
            if (preset.isCurrent) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.62f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        currentIconContent()
                    }
                }
            }
        }

        Text(
            text = preset.name,
            color = if (preset.isDisabled) colors.fontDisabled else colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        if (preset.isCustom) {
            EqualizerIconButton(
                contentDescription = strings.exportEqualizer,
                onClick = onExport,
                iconContent = exportIconContent
            )
            if (!preset.isCurrent) {
                EqualizerIconButton(
                    contentDescription = strings.delete,
                    onClick = onDelete,
                    iconContent = deleteIconContent
                )
            }
        } else if (!preset.isCurrent) {
            if (preset.isDisabled) {
                EqualizerIconButton(
                    contentDescription = strings.enablePreset,
                    onClick = onEnable,
                    iconContent = enableIconContent
                )
            } else {
                EqualizerIconButton(
                    contentDescription = strings.disablePreset,
                    onClick = onDisable,
                    iconContent = disableIconContent
                )
            }
        }
    }
}

@Composable
private fun EqualizerOverwriteCard(
    state: VLCEqualizerOverwriteState,
    strings: VLCEqualizerSettingsStrings,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = VLCThemeDefaults.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        shape = RoundedCornerShape(4.dp),
        color = colors.cardBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = state.warning,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(state.label) },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { error -> { Text(error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text(strings.cancel)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onConfirm,
                    enabled = state.confirmEnabled
                ) {
                    Text(state.confirmText)
                }
            }
        }
    }
}

@Composable
private fun EqualizerEditorSheet(
    state: VLCEqualizerEditorState,
    strings: VLCEqualizerSettingsStrings,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onAddEqualizer: () -> Unit,
    onSelectPreset: (Long) -> Unit,
    onEditPreset: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameFocusChange: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onBandChangeFinished: () -> Unit,
    onSnapBandsChange: (Boolean) -> Unit,
    addIconContent: @Composable () -> Unit,
    settingsIconContent: (@Composable () -> Unit)? = null,
    editIconContent: @Composable () -> Unit,
    undoIconContent: @Composable () -> Unit,
    deleteIconContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
    ) {
        item(key = "title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                EqualizerIconButton(
                    contentDescription = strings.addEqualizer,
                    onClick = onAddEqualizer,
                    iconContent = addIconContent
                )
                if (onOpenSettings != null && settingsIconContent != null) {
                    EqualizerIconButton(
                        contentDescription = strings.preferences,
                        onClick = onOpenSettings,
                        iconContent = settingsIconContent
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.done)
                }
            }
        }

        item(key = "enabled") {
            EqualizerSwitchRow(
                text = strings.enableEqualizer,
                checked = state.equalizerEnabled,
                onCheckedChange = onEqualizerEnabledChange,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item(key = "presets") {
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 8.dp)
            ) {
                items(
                    items = state.presets,
                    key = { it.id }
                ) { preset ->
                    FilterChip(
                        selected = preset.id == state.current.id,
                        enabled = state.equalizerEnabled && !preset.isDisabled,
                        onClick = { onSelectPreset(preset.id) },
                        label = {
                            Text(
                                text = preset.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        item(key = "card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(4.dp),
                color = colors.cardBackground,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.current.isCustom) {
                            OutlinedTextField(
                                value = state.editedName,
                                onValueChange = onNameChange,
                                enabled = state.canEditCurrent,
                                singleLine = true,
                                isError = state.nameError != null,
                                supportingText = state.nameError?.let { error -> { Text(error) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { onNameFocusChange(it.isFocused) }
                            )
                        } else {
                            Text(
                                text = state.current.name,
                                color = colors.fontDefault,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (state.current.isCustom) {
                            EqualizerIconButton(
                                contentDescription = strings.undo,
                                enabled = state.canUndo,
                                onClick = onUndo,
                                iconContent = undoIconContent
                            )
                        } else {
                            EqualizerIconButton(
                                contentDescription = strings.editPreset,
                                enabled = state.equalizerEnabled,
                                onClick = onEditPreset,
                                iconContent = editIconContent
                            )
                        }
                        EqualizerIconButton(
                            contentDescription = strings.delete,
                            enabled = state.equalizerEnabled,
                            onClick = onDelete,
                            iconContent = deleteIconContent
                        )
                    }

                    EqualizerSliderRow(
                        label = strings.preamp,
                        value = state.current.preamp,
                        enabled = state.canEditCurrent,
                        onValueChange = onPreampChange,
                        onValueChangeFinished = {},
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    state.current.bands.sortedBy { it.index }.forEach { band ->
                        EqualizerSliderRow(
                            label = band.label,
                            value = band.value,
                            enabled = state.canEditCurrent,
                            onValueChange = { onBandChange(band.index, it) },
                            onValueChangeFinished = onBandChangeFinished,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    EqualizerSwitchRow(
                        text = strings.snapBands,
                        checked = state.snapBands,
                        enabled = state.canEditCurrent,
                        onCheckedChange = onSnapBandsChange,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerSliderRow(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    val rounded = value.roundToInt()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) colors.fontDefault else colors.fontDisabled,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = Modifier.width(64.dp)
        )
        Slider(
            value = value.coerceIn(EqualizerValueMin, EqualizerValueMax),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = EqualizerValueMin..EqualizerValueMax,
            steps = EqualizerSliderSteps,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (rounded > 0) "+${rounded}dB" else "${rounded}dB",
            color = if (enabled) colors.fontLight else colors.fontDisabled,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun EqualizerSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = VLCThemeDefaults.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (enabled) colors.fontDefault else colors.fontDisabled,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun EqualizerIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        }
    ) {
        iconContent()
    }
}

@Composable
private fun EqualizerPreviewIcon(
    bands: List<VLCEqualizerBand>,
    isCustom: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    val barColor = if (isCustom) colors.primary else colors.defaultDivider
    val backgroundColor = colors.backgroundDefaultDarker
    val strokeColor = colors.defaultDivider

    Canvas(modifier = modifier) {
        val radius = 4.dp.toPx()
        val stroke = 1.dp.toPx()
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = strokeColor,
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(stroke)
        )

        val sortedBands = bands.sortedBy { it.index }.ifEmpty { MockEqualizerBands }
        val barWidth = size.width / sortedBands.size
        sortedBands.forEachIndexed { index, band ->
            val normalized = ((band.value + 20f) / 40f).coerceIn(0f, 1f)
            val barHeight = (size.height - stroke * 2f) * normalized
            drawRect(
                color = barColor,
                topLeft = Offset(index * barWidth + stroke, size.height - barHeight - stroke),
                size = Size((barWidth - stroke * 2f).coerceAtLeast(1f), barHeight.coerceAtLeast(1f))
            )
        }
    }
}

@Composable
private fun EqualizerIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}

private const val EqualizerValueMin = -20f
private const val EqualizerValueMax = 20f
private const val EqualizerSliderSteps = 39

private val MockEqualizerBands = listOf(
    VLCEqualizerBand(0, "60Hz", -4f),
    VLCEqualizerBand(1, "170Hz", 3f),
    VLCEqualizerBand(2, "310Hz", 6f),
    VLCEqualizerBand(3, "600Hz", 1f),
    VLCEqualizerBand(4, "1kHz", -2f),
    VLCEqualizerBand(5, "3kHz", 7f),
    VLCEqualizerBand(6, "6kHz", 4f),
    VLCEqualizerBand(7, "12kHz", 0f)
)

private val MockEqualizerPresets = listOf(
    VLCEqualizerPreset(
        id = 1L,
        name = "Flat",
        presetIndex = 0,
        isDisabled = false,
        isCurrent = true,
        preamp = 0f,
        bands = MockEqualizerBands.map { it.copy(value = 0f) }
    ),
    VLCEqualizerPreset(
        id = 2L,
        name = "Classical",
        presetIndex = 1,
        isDisabled = false,
        isCurrent = false,
        preamp = 1f,
        bands = MockEqualizerBands
    ),
    VLCEqualizerPreset(
        id = 3L,
        name = "Late night headphones",
        presetIndex = -1,
        isDisabled = false,
        isCurrent = false,
        preamp = -2f,
        bands = MockEqualizerBands.reversed().mapIndexed { index, band -> band.copy(index = index) }
    ),
    VLCEqualizerPreset(
        id = 4L,
        name = "Dance",
        presetIndex = 2,
        isDisabled = true,
        isCurrent = false,
        preamp = 0f,
        bands = MockEqualizerBands
    )
)

private val MockEqualizerStrings = VLCEqualizerSettingsStrings(
    title = "Equalizer",
    preferences = "Settings",
    close = "Close",
    showEqualizer = "Show equalizer",
    importEqualizer = "Import equalizer",
    moreActions = "More",
    showAll = "Show all",
    hideAll = "Hide all",
    exportAll = "Export all",
    importAll = "Import all",
    enablePreset = "Show equalizer",
    disablePreset = "Hide equalizer",
    delete = "Delete",
    exportEqualizer = "Export equalizer",
    equalizerName = "Equalizer name",
    cancel = "Cancel",
    enableEqualizer = "Enable",
    addEqualizer = "Add",
    editPreset = "Edit equalizer preset",
    undo = "Undo",
    preamp = "Preamp",
    snapBands = "Snap bands",
    done = "Done"
)

@Preview(name = "Equalizer Settings", showBackground = true)
@Composable
private fun VLCEqualizerSettingsScreenPreview() {
    VLCEqualizerSettingsScreen(
        presets = MockEqualizerPresets,
        editorState = null,
        overwriteState = VLCEqualizerOverwriteState(
            name = "Late night headphones",
            warning = "This equalizer already exists",
            label = "Equalizer name",
            error = null,
            confirmText = "Overwrite",
            confirmEnabled = true
        ),
        strings = MockEqualizerStrings,
        scrollToTopSignal = 0,
        onBack = {},
        onOpenEditor = {},
        onImport = {},
        onShowAll = {},
        onHideAll = {},
        onExportAll = {},
        onImportAll = {},
        onSelectPreset = {},
        onEnablePreset = {},
        onDisablePreset = {},
        onDeletePreset = {},
        onExportPreset = {},
        onOverwriteNameChange = {},
        onCancelOverwrite = {},
        onConfirmOverwrite = {},
        onDismissEditor = {},
        onEqualizerEnabledChange = {},
        onAddEqualizer = {},
        onSelectEditorPreset = {},
        onEditCurrentPreset = {},
        onUndo = {},
        onDeleteCurrent = {},
        onEditorNameChange = {},
        onEditorNameFocusChange = {},
        onPreampChange = {},
        onBandChange = { _, _ -> },
        onBandChangeFinished = {},
        onSnapBandsChange = {}
    )
}

@Preview(name = "Equalizer Editor Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCEqualizerSettingsEditorPreview() {
    VLCEqualizerSettingsScreen(
        presets = MockEqualizerPresets,
        editorState = VLCEqualizerEditorState(
            presets = MockEqualizerPresets.filterNot { it.isDisabled },
            current = MockEqualizerPresets[2].copy(isCurrent = true),
            equalizerEnabled = true,
            snapBands = true,
            canEditCurrent = true,
            canUndo = true,
            editedName = "Late night headphones",
            nameError = null
        ),
        overwriteState = null,
        strings = MockEqualizerStrings,
        scrollToTopSignal = 0,
        onBack = {},
        onOpenEditor = {},
        onImport = {},
        onShowAll = {},
        onHideAll = {},
        onExportAll = {},
        onImportAll = {},
        onSelectPreset = {},
        onEnablePreset = {},
        onDisablePreset = {},
        onDeletePreset = {},
        onExportPreset = {},
        onOverwriteNameChange = {},
        onCancelOverwrite = {},
        onConfirmOverwrite = {},
        onDismissEditor = {},
        onEqualizerEnabledChange = {},
        onAddEqualizer = {},
        onSelectEditorPreset = {},
        onEditCurrentPreset = {},
        onUndo = {},
        onDeleteCurrent = {},
        onEditorNameChange = {},
        onEditorNameFocusChange = {},
        onPreampChange = {},
        onBandChange = { _, _ -> },
        onBandChangeFinished = {},
        onSnapBandsChange = {}
    )
}
