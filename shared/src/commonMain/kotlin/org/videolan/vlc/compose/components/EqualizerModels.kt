package org.videolan.vlc.compose.components

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
