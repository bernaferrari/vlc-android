package org.videolan.vlc.gui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.MediaPlayer
import org.videolan.resources.AndroidDevices
import org.videolan.resources.VLCInstance
import org.videolan.resources.VLCOptions
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.tools.KEY_EQUALIZER_ENABLED
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCEqualizerBand
import org.videolan.vlc.compose.components.VLCEqualizerEditorState
import org.videolan.vlc.compose.components.VLCEqualizerOverwriteState
import org.videolan.vlc.compose.components.VLCEqualizerPreset
import org.videolan.vlc.compose.components.VLCEqualizerSettingsScreen
import org.videolan.vlc.compose.components.VLCEqualizerSettingsStrings
import org.videolan.vlc.gui.browser.EXTRA_MRL
import org.videolan.vlc.gui.browser.FilePickerActivity
import org.videolan.vlc.gui.browser.KEY_PICKER_TYPE
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.mediadb.models.EqualizerBand
import org.videolan.vlc.mediadb.models.EqualizerWithBands
import org.videolan.vlc.providers.PickerType
import org.videolan.vlc.repository.EqualizerRepository
import org.videolan.vlc.util.EqualizerUtil
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.JsonUtil
import org.videolan.vlc.viewmodels.EqualizerViewModel
import org.videolan.vlc.viewmodels.EqualizerViewModelFactory
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val FILE_PICKER_RESULT_CODE = 10000
private const val FILE_PICKER_ALL_RESULT_CODE = 10001

/**
 * Equalizer settings activity allowing to enable/disable/delete/export/import
 * presets. The screen itself is now hosted by Compose; the legacy
 * EqualizerFragmentDialog remains available to player callers but is no longer
 * used by this Activity.
 */
class EqualizerSettingsActivity : BaseActivity() {

    private var scrollTopNext = false
    private var rootView: ComposeView? = null
    private var unfilteredEqualizers by mutableStateOf(emptyList<EqualizerWithBands>())
    private var visibleEqualizers by mutableStateOf(emptyList<EqualizerWithBands>())
    private var currentEqualizerId by mutableStateOf(-1L)
    private var scrollToTopSignal by mutableStateOf(0)
    private var overwriteTarget by mutableStateOf<EqualizerWithBands?>(null)
    private var overwriteName by mutableStateOf("")
    private var editorVisible by mutableStateOf(false)
    private var equalizerEnabled by mutableStateOf(false)
    private var snapBands by mutableStateOf(true)
    private var editorName by mutableStateOf("")
    private var editorNameError by mutableStateOf<String?>(null)
    private var editorNameFocused by mutableStateOf(false)
    private var previewPreamp by mutableStateOf<Float?>(null)
    private var previewBands by mutableStateOf<List<EqualizerBand>?>(null)
    private var bandDragStartValues: List<Float>? = null

    private val model: EqualizerViewModel by viewModels {
        EqualizerViewModelFactory(this, EqualizerRepository.getInstance(application))
    }

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = rootView ?: window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        equalizerEnabled = VLCOptions.getEqualizerEnabledState(this)
        lifecycleScope.launch { ensureBandCount() }

        rootView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                VLCEqualizerSettingsScreen(
                    presets = unfilteredEqualizers.map { it.toPresetUi() },
                    editorState = buildEditorState(),
                    overwriteState = buildOverwriteState(),
                    strings = buildStrings(),
                    scrollToTopSignal = scrollToTopSignal,
                    onBack = ::finish,
                    onOpenEditor = ::showEqualizerEditor,
                    onImport = { startFilePicker(FILE_PICKER_RESULT_CODE) },
                    onShowAll = { model.showAll(this@EqualizerSettingsActivity) },
                    onHideAll = { model.hideAll(this@EqualizerSettingsActivity) },
                    onExportAll = { model.exportAll(this@EqualizerSettingsActivity) },
                    onImportAll = { startFilePicker(FILE_PICKER_ALL_RESULT_CODE) },
                    onSelectPreset = ::selectPreset,
                    onEnablePreset = { findEqualizer(it)?.let { equalizer -> model.enable(this@EqualizerSettingsActivity, equalizer) } },
                    onDisablePreset = { findEqualizer(it)?.let { equalizer -> model.disable(this@EqualizerSettingsActivity, equalizer) } },
                    onDeletePreset = ::deletePreset,
                    onExportPreset = { findEqualizer(it)?.let { equalizer -> model.export(this@EqualizerSettingsActivity, equalizer) } },
                    onOverwriteNameChange = { overwriteName = it },
                    onCancelOverwrite = ::clearOverwrite,
                    onConfirmOverwrite = ::confirmOverwrite,
                    onDismissEditor = ::dismissEqualizerEditor,
                    onEqualizerEnabledChange = ::updateEqualizerEnabled,
                    onAddEqualizer = ::addEqualizer,
                    onSelectEditorPreset = ::selectPreset,
                    onEditCurrentPreset = { model.createCustomEqualizer(this@EqualizerSettingsActivity) },
                    onUndo = ::undoEqualizerChange,
                    onDeleteCurrent = ::deleteCurrentEqualizer,
                    onEditorNameChange = ::renameCurrentEqualizer,
                    onEditorNameFocusChange = { focused ->
                        editorNameFocused = focused
                        if (!focused) syncEditorName()
                    },
                    onPreampChange = ::updatePreamp,
                    onBandChange = ::previewBandChange,
                    onBandChangeFinished = ::commitBandChange,
                    onSnapBandsChange = { snapBands = it },
                    closeIconContent = { EqualizerIcon(R.drawable.ic_close_up) },
                    equalizerIconContent = { EqualizerIcon(R.drawable.ic_options_equalizer) },
                    importIconContent = { EqualizerIcon(R.drawable.ic_import) },
                    overflowIconContent = { EqualizerIcon(R.drawable.ic_more) },
                    enableIconContent = { EqualizerIcon(R.drawable.ic_invisible) },
                    disableIconContent = { EqualizerIcon(R.drawable.ic_visible) },
                    deleteIconContent = { EqualizerIcon(R.drawable.ic_delete) },
                    exportIconContent = { EqualizerIcon(R.drawable.ic_export) },
                    editIconContent = { EqualizerIcon(R.drawable.ic_edit) },
                    undoIconContent = { EqualizerIcon(R.drawable.ic_undo) },
                    addIconContent = { EqualizerIcon(R.drawable.ic_add) },
                    currentIconContent = { EqualizerIcon(R.drawable.ic_check_large_white) }
                )
            }
        }
        setContentView(rootView)

        EqualizerViewModel.currentEqualizerIdLive.observe(this) { id ->
            currentEqualizerId = id
            if (scrollTopNext) {
                scrollToTopSignal += 1
                scrollTopNext = false
            }
            if (editorVisible) syncEditorName()
        }

        model.equalizerUnfilteredEntries.observe(this) { equalizers ->
            unfilteredEqualizers = equalizers.orEmpty()
            if (editorVisible && bandDragStartValues == null) {
                previewBands = null
                previewPreamp = null
            }
            if (editorVisible) {
                syncEditorName()
                if (equalizerEnabled && currentEqualizerForEditor() != null) model.updateEqualizer()
            }
        }

        model.equalizerEntries.observe(this) { equalizers ->
            visibleEqualizers = equalizers.orEmpty()
            if (editorVisible) syncEditorName()
        }

        if (AndroidDevices.isTv) applyOverscanMargin(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        if (requestCode == FILE_PICKER_RESULT_CODE) {
            if (data.hasExtra(EXTRA_MRL)) lifecycleScope.launch {
                data.getStringExtra(EXTRA_MRL)?.toUri()?.path?.let {
                    val equalizerString = FileUtils.getStringFromFile(it)
                    try {
                        val equalizer = JsonUtil.getEqualizerFromJson(equalizerString)
                        equalizer?.let {
                            if (it.bands.isEmpty()) {
                                UiTools.snacker(this@EqualizerSettingsActivity, getString(R.string.invalid_equalizer_file))
                            } else {
                                if (isNameAllowed(it.equalizerEntry.name)) {
                                    model.insert(this@EqualizerSettingsActivity, it)
                                } else {
                                    showOverwriteDialog(it)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EqualizerSettings", "onActivityResult: ${e.message}", e)
                        UiTools.snacker(this@EqualizerSettingsActivity, getString(R.string.invalid_equalizer_file))
                    }
                }
            }
        } else if (requestCode == FILE_PICKER_ALL_RESULT_CODE) {
            if (data.hasExtra(EXTRA_MRL)) lifecycleScope.launch {
                data.getStringExtra(EXTRA_MRL)?.toUri()?.path?.let {
                    val equalizerString = FileUtils.getStringFromFile(it)
                    try {
                        scrollTopNext = true
                        EqualizerUtil.importAll(this@EqualizerSettingsActivity, equalizerString) { newId ->
                            model.currentEqualizerId = newId
                            model.updateEqualizer()
                        }
                    } catch (e: Exception) {
                        Log.e("EqualizerSettings", "onActivityResult: ${e.message}", e)
                        UiTools.snacker(this@EqualizerSettingsActivity, getString(R.string.invalid_equalizer_file))
                    }
                }
            }
        }
    }

    fun showOverwriteDialog(equalizer: EqualizerWithBands) {
        overwriteTarget = equalizer
        overwriteName = equalizer.equalizerEntry.name
        scrollToTopSignal += 1
    }

    private fun startFilePicker(requestCode: Int) {
        val filePickerIntent = Intent(this, FilePickerActivity::class.java)
        filePickerIntent.putExtra(KEY_PICKER_TYPE, PickerType.EQUALIZER.ordinal)
        startActivityForResult(filePickerIntent, requestCode)
    }

    private suspend fun ensureBandCount() {
        if (model.bandCount != -1) return
        model.bandCount = withContext(Dispatchers.IO) {
            VLCInstance.getInstance(this@EqualizerSettingsActivity)
            MediaPlayer.Equalizer.getBandCount()
        }
    }

    private fun showEqualizerEditor() {
        lifecycleScope.launch {
            ensureBandCount()
            equalizerEnabled = VLCOptions.getEqualizerEnabledState(this@EqualizerSettingsActivity)
            resetEditorPreview()
            editorVisible = true
            syncEditorName(force = true)
        }
    }

    private fun dismissEqualizerEditor() {
        editorVisible = false
        editorNameFocused = false
        editorNameError = null
        resetEditorPreview()
    }

    private fun addEqualizer() {
        model.createCustomEqualizer(this, true)
        if (!equalizerEnabled) updateEqualizerEnabled(true)
    }

    private fun undoEqualizerChange() {
        model.undoFromHistory(this)
        resetEditorPreview()
    }

    private fun updateEqualizerEnabled(enabled: Boolean) {
        equalizerEnabled = enabled
        Settings.getInstance(this).edit { putBoolean(KEY_EQUALIZER_ENABLED, enabled) }
        if (currentEqualizerForEditor() != null) model.updateEqualizer()
    }

    private fun selectPreset(id: Long) {
        val equalizer = findEqualizer(id) ?: return
        if (equalizer.equalizerEntry.isDisabled || equalizer.equalizerEntry.id == currentEqualizerId) return
        model.clearHistory()
        model.currentEqualizerId = id
        resetEditorPreview()
        editorName = equalizer.equalizerEntry.name
        editorNameError = null
        if (equalizerEnabled) model.updateEqualizer()
    }

    private fun deletePreset(id: Long) {
        val equalizer = findEqualizer(id) ?: return
        model.delete(equalizer)
        UiTools.snackerConfirm(this, getString(R.string.equalizer_deleted), confirmMessage = R.string.undo) {
            model.restore(this)
        }
    }

    private fun deleteCurrentEqualizer() {
        val current = currentEqualizerForEditor() ?: return
        model.presetToDelete = current
        val message = if (current.equalizerEntry.presetIndex == -1) {
            getString(R.string.confirm_delete_eq)
        } else {
            getString(R.string.confirm_delete_vlc_eq)
        }
        UiTools.snackerConfirm(this, message) {
            model.deleteEqualizer(this)
            resetEditorPreview()
        }
    }

    private fun renameCurrentEqualizer(name: String) {
        editorName = name
        val current = currentEqualizerForEditor() ?: return
        if (name == current.equalizerEntry.name) {
            editorNameError = null
            return
        }
        if (!isNameAllowed(name)) {
            editorNameError = getString(R.string.edit_eq_name_not_allowed)
            return
        }
        editorNameError = null
        model.updateEqualizerName(this, name)
    }

    private fun updatePreamp(value: Float) {
        val current = currentEqualizerForEditor() ?: return
        if (!current.isEditable()) return
        val rounded = value.roundToInt().toFloat().coerceIn(EQUALIZER_VALUE_MIN, EQUALIZER_VALUE_MAX)
        previewPreamp = rounded
        model.saveInHistory(-1)
        model.updateCurrentPreamp(this, rounded)
    }

    private fun previewBandChange(index: Int, value: Float) {
        val current = currentEqualizerForEditor() ?: return
        if (!current.isEditable()) return

        val sortedBands = (previewBands ?: current.bands).sortedBy { it.index }
        if (bandDragStartValues == null) bandDragStartValues = sortedBands.map { it.bandValue }
        val startValues = bandDragStartValues ?: return
        val rounded = value.roundToInt().toFloat().coerceIn(EQUALIZER_VALUE_MIN, EQUALIZER_VALUE_MAX)

        model.saveInHistory(index)
        previewBands = sortedBands.map { band ->
            val newValue = when {
                band.index == index -> rounded
                snapBands -> {
                    val oldValue = startValues.getOrNull(band.index) ?: band.bandValue
                    val startValue = startValues.getOrNull(index) ?: rounded
                    val distance = (band.index - index).absoluteValue
                    (oldValue + (rounded - startValue) / (distance * distance * distance + 1)).coerceIn(EQUALIZER_VALUE_MIN, EQUALIZER_VALUE_MAX)
                }
                else -> band.bandValue
            }
            band.copy(bandValue = newValue)
        }
    }

    private fun commitBandChange() {
        val bands = previewBands ?: run {
            bandDragStartValues = null
            return
        }
        model.updateEqualizerBands(this, bands)
        bandDragStartValues = null
    }

    private fun confirmOverwrite() {
        val target = overwriteTarget ?: return
        if (isNameForbidden(overwriteName)) return
        model.insert(this, target.copy(equalizerEntry = target.equalizerEntry.copy(name = overwriteName)))
        UiTools.setKeyboardVisibility(window.decorView, false)
        clearOverwrite()
    }

    private fun clearOverwrite() {
        overwriteTarget = null
        overwriteName = ""
    }

    private fun resetEditorPreview() {
        previewPreamp = null
        previewBands = null
        bandDragStartValues = null
    }

    private fun syncEditorName(force: Boolean = false) {
        val current = currentEqualizerForEditor() ?: return
        if (force || !editorNameFocused) editorName = current.equalizerEntry.name
    }

    private fun buildEditorState(): VLCEqualizerEditorState? {
        if (!editorVisible) return null
        val current = currentEqualizerForEditor() ?: return null
        val currentUi = current.toPresetUi().copy(
            name = if (current.equalizerEntry.presetIndex == -1) editorName else current.equalizerEntry.name,
            preamp = previewPreamp ?: current.equalizerEntry.preamp,
            bands = (previewBands ?: current.bands).sortedBy { it.index }.map { it.toBandUi() }
        )
        val canEditCurrent = equalizerEnabled && current.equalizerEntry.presetIndex == -1
        return VLCEqualizerEditorState(
            presets = visibleEqualizers.map { it.toPresetUi() },
            current = currentUi,
            equalizerEnabled = equalizerEnabled,
            snapBands = snapBands,
            canEditCurrent = canEditCurrent,
            canUndo = canEditCurrent && model.history.isNotEmpty(),
            editedName = editorName,
            nameError = editorNameError
        )
    }

    private fun buildOverwriteState(): VLCEqualizerOverwriteState? {
        overwriteTarget ?: return null
        val forbidden = isNameForbidden(overwriteName)
        val allowed = isNameAllowed(overwriteName)
        return VLCEqualizerOverwriteState(
            name = overwriteName,
            warning = getString(R.string.equalizer_overwrite_warning),
            label = getString(R.string.equalizer_name),
            error = if (forbidden) getString(R.string.eq_cannot_overwrite) else null,
            confirmText = getString(if (allowed) R.string.rename else R.string.overwrite),
            confirmEnabled = !forbidden
        )
    }

    private fun buildStrings() = VLCEqualizerSettingsStrings(
        title = getString(R.string.equalizer),
        close = getString(R.string.close),
        showEqualizer = getString(R.string.equalizer),
        importEqualizer = getString(R.string.import_equalizer),
        moreActions = getString(R.string.more),
        showAll = getString(R.string.show_all),
        hideAll = getString(R.string.hide_all),
        exportAll = getString(R.string.export_all),
        importAll = getString(R.string.import_all),
        enablePreset = getString(R.string.show_equalizer),
        disablePreset = getString(R.string.hide_equalizer),
        delete = getString(R.string.delete),
        exportEqualizer = getString(R.string.export_equalizer),
        equalizerName = getString(R.string.equalizer_name),
        cancel = getString(R.string.cancel),
        enableEqualizer = getString(R.string.enable_equalizer),
        addEqualizer = getString(R.string.add),
        editPreset = getString(R.string.edit_eq_preset),
        undo = getString(R.string.undo),
        preamp = getString(R.string.preamp),
        snapBands = getString(R.string.eq_snap_bands),
        done = getString(R.string.done)
    )

    private fun findEqualizer(id: Long) =
        unfilteredEqualizers.firstOrNull { it.equalizerEntry.id == id }
            ?: visibleEqualizers.firstOrNull { it.equalizerEntry.id == id }

    private fun currentEqualizerForEditor() =
        visibleEqualizers.firstOrNull { it.equalizerEntry.id == currentEqualizerId }
            ?: visibleEqualizers.firstOrNull()

    private fun EqualizerWithBands.isEditable() =
        equalizerEnabled && equalizerEntry.presetIndex == -1

    private fun EqualizerWithBands.toPresetUi() = VLCEqualizerPreset(
        id = equalizerEntry.id,
        name = equalizerEntry.name,
        presetIndex = equalizerEntry.presetIndex,
        isDisabled = equalizerEntry.isDisabled,
        isCurrent = equalizerEntry.id == currentEqualizerId,
        preamp = equalizerEntry.preamp,
        bands = bands.sortedBy { it.index }.map { it.toBandUi() }
    )

    private fun EqualizerBand.toBandUi() = VLCEqualizerBand(
        index = index,
        label = formatBandFrequency(index),
        value = bandValue
    )

    private fun formatBandFrequency(index: Int): String {
        val bandFrequency = runCatching { MediaPlayer.Equalizer.getBandFrequency(index) }.getOrNull()
            ?: return "${index + 1}"
        return if (bandFrequency < 999.5f) {
            "${(bandFrequency + 0.5f).toInt()}Hz"
        } else {
            "${(bandFrequency / 1000.0f + 0.5f).toInt()}kHz"
        }
    }

    private fun isNameAllowed(name: String): Boolean {
        return name.isNotBlank() && unfilteredEqualizers.none { it.equalizerEntry.name == name }
    }

    private fun isNameForbidden(name: String): Boolean {
        return unfilteredEqualizers.any { it.equalizerEntry.name == name && it.equalizerEntry.presetIndex != -1 }
    }

    companion object {

        const val TAG = "VLC/EqualizerSettingsActivity"
    }
}

@Composable
private fun EqualizerIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}

private const val EQUALIZER_VALUE_MIN = -20f
private const val EQUALIZER_VALUE_MAX = 20f
