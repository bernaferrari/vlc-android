package org.videolan.vlc.gui.dialogs

import android.content.Intent
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.MediaPlayer
import org.videolan.resources.AndroidDevices
import org.videolan.resources.VLCInstance
import org.videolan.tools.KEY_EQUALIZER_ENABLED
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCEqualizerBand
import org.videolan.vlc.compose.components.VLCEqualizerEditorDialogContent
import org.videolan.vlc.compose.components.VLCEqualizerEditorState
import org.videolan.vlc.compose.components.VLCEqualizerPreset
import org.videolan.vlc.compose.components.VLCEqualizerSettingsStrings
import org.videolan.vlc.gui.EqualizerSettingsActivity
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.mediadb.models.EqualizerBand
import org.videolan.vlc.mediadb.models.EqualizerWithBands
import org.videolan.vlc.repository.EqualizerRepository
import org.videolan.vlc.util.isTalkbackIsEnabled
import org.videolan.vlc.viewmodels.EqualizerViewModel
import org.videolan.vlc.viewmodels.EqualizerViewModelFactory
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Compose-hosted equalizer bottom sheet for player callers.
 *
 * This replaces EqualizerFragmentDialog without changing the player option
 * entry points or EqualizerViewModel behavior.
 */
fun FragmentActivity.showEqualizerComposeDialog(
    warnBeforeSettings: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    EqualizerComposeDialog(this, warnBeforeSettings, onDismiss).show()
}

private class EqualizerComposeDialog(
    private val activity: FragmentActivity,
    private val warnBeforeSettings: Boolean,
    private val onDismiss: (() -> Unit)?
) {
    private val model: EqualizerViewModel = ViewModelProvider(
        activity,
        EqualizerViewModelFactory(activity, EqualizerRepository.getInstance(activity.application))
    )[EqualizerViewModel::class.java]

    private val dialog = if (Settings.showTvUi) {
        BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
    } else {
        BottomSheetDialog(activity)
    }

    private var rootView: ComposeView? = null
    private var visibleEqualizers by mutableStateOf(emptyList<EqualizerWithBands>())
    private var currentEqualizerId by mutableStateOf(-1L)
    private var equalizerEnabled by mutableStateOf(false)
    private var snapBands by mutableStateOf(true)
    private var editorName by mutableStateOf("")
    private var editorNameError by mutableStateOf<String?>(null)
    private var editorNameFocused by mutableStateOf(false)
    private var previewPreamp by mutableStateOf<Float?>(null)
    private var previewBands by mutableStateOf<List<EqualizerBand>?>(null)
    private var bandDragStartValues: List<Float>? = null

    private val currentEqualizerObserver = Observer<Long> { id ->
        currentEqualizerId = id
        syncEditorName()
    }

    private val equalizersObserver = Observer<List<EqualizerWithBands>> { equalizers ->
        visibleEqualizers = equalizers.orEmpty()
        if (bandDragStartValues == null) resetEditorPreview()
        syncEditorName()
        if (equalizerEnabled && currentEqualizerForEditor() != null) model.updateEqualizer()
    }

    fun show() {
        if (dialog.isShowing) return
        equalizerEnabled = org.videolan.resources.VLCOptions.getEqualizerEnabledState(activity)
        setupContent()
        observeEqualizers()
        activity.lifecycleScope.launch {
            ensureBandCount()
            dialog.show()
            configureBottomSheet()
        }
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val editorState = buildEditorState()
                if (editorState != null) {
                    VLCEqualizerEditorDialogContent(
                        state = editorState,
                        strings = buildStrings(),
                        onDismiss = { dialog.dismiss() },
                        onOpenSettings = ::openEqualizerSettings,
                        onEqualizerEnabledChange = ::updateEqualizerEnabled,
                        onAddEqualizer = ::addEqualizer,
                        onSelectPreset = ::selectPreset,
                        onEditPreset = { model.createCustomEqualizer(activity) },
                        onUndo = ::undoEqualizerChange,
                        onDelete = ::deleteCurrentEqualizer,
                        onNameChange = ::renameCurrentEqualizer,
                        onNameFocusChange = { focused ->
                            editorNameFocused = focused
                            if (!focused) syncEditorName()
                        },
                        onPreampChange = ::updatePreamp,
                        onBandChange = ::previewBandChange,
                        onBandChangeFinished = ::commitBandChange,
                        onSnapBandsChange = { snapBands = it },
                        modifier = Modifier.fillMaxWidth(),
                        addIconContent = { EqualizerDialogIcon(R.drawable.ic_add) },
                        settingsIconContent = { EqualizerDialogIcon(R.drawable.ic_settings) },
                        editIconContent = { EqualizerDialogIcon(R.drawable.ic_edit) },
                        undoIconContent = { EqualizerDialogIcon(R.drawable.ic_undo) },
                        deleteIconContent = { EqualizerDialogIcon(R.drawable.ic_delete) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                    )
                }
            }
        }

        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            clearObservers()
            onDismiss?.invoke()
        }
    }

    private fun observeEqualizers() {
        EqualizerViewModel.currentEqualizerIdLive.observe(activity, currentEqualizerObserver)
        model.equalizerEntries.observe(activity, equalizersObserver)
    }

    private fun clearObservers() {
        EqualizerViewModel.currentEqualizerIdLive.removeObserver(currentEqualizerObserver)
        model.equalizerEntries.removeObserver(equalizersObserver)
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (AndroidDevices.isChromeBook) behavior.isDraggable = false
        }
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }

    private suspend fun ensureBandCount() {
        if (model.bandCount != -1) return
        model.bandCount = withContext(Dispatchers.IO) {
            VLCInstance.getInstance(activity)
            MediaPlayer.Equalizer.getBandCount()
        }
    }

    private fun openEqualizerSettings() {
        val openSettings = {
            dialog.dismiss()
            activity.startActivity(Intent(activity, EqualizerSettingsActivity::class.java))
        }
        if (warnBeforeSettings) {
            UiTools.snackerConfirm(
                activity = activity,
                message = activity.getString(R.string.equalizer_leave_warning),
                forcedView = rootView
            ) {
                openSettings()
            }
        } else {
            openSettings()
        }
    }

    private fun addEqualizer() {
        model.createCustomEqualizer(activity, true)
        if (!equalizerEnabled) updateEqualizerEnabled(true)
    }

    private fun undoEqualizerChange() {
        model.undoFromHistory(activity)
        resetEditorPreview()
    }

    private fun updateEqualizerEnabled(enabled: Boolean) {
        equalizerEnabled = enabled
        Settings.getInstance(activity).edit { putBoolean(KEY_EQUALIZER_ENABLED, enabled) }
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

    private fun deleteCurrentEqualizer() {
        val current = currentEqualizerForEditor() ?: return
        model.presetToDelete = current
        val message = if (current.equalizerEntry.presetIndex == -1) {
            activity.getString(R.string.confirm_delete_eq)
        } else {
            activity.getString(R.string.confirm_delete_vlc_eq)
        }
        UiTools.snackerConfirm(activity, message, forcedView = rootView) {
            model.deleteEqualizer(activity)
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
        val isAllowed = runCatching { model.isNameAllowed(name) }.getOrDefault(false)
        if (!isAllowed) {
            editorNameError = activity.getString(R.string.edit_eq_name_not_allowed)
            return
        }
        editorNameError = null
        model.updateEqualizerName(activity, name)
    }

    private fun updatePreamp(value: Float) {
        val current = currentEqualizerForEditor() ?: return
        if (!current.isEditable()) return
        val rounded = value.roundToInt().toFloat().coerceIn(EQUALIZER_VALUE_MIN, EQUALIZER_VALUE_MAX)
        previewPreamp = rounded
        model.saveInHistory(-1)
        model.updateCurrentPreamp(activity, rounded)
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
        model.updateEqualizerBands(activity, bands)
        bandDragStartValues = null
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

    private fun buildStrings() = VLCEqualizerSettingsStrings(
        title = activity.getString(R.string.equalizer),
        preferences = activity.getString(R.string.preferences),
        close = activity.getString(R.string.close),
        showEqualizer = activity.getString(R.string.equalizer),
        importEqualizer = activity.getString(R.string.import_equalizer),
        moreActions = activity.getString(R.string.more),
        showAll = activity.getString(R.string.show_all),
        hideAll = activity.getString(R.string.hide_all),
        exportAll = activity.getString(R.string.export_all),
        importAll = activity.getString(R.string.import_all),
        enablePreset = activity.getString(R.string.show_equalizer),
        disablePreset = activity.getString(R.string.hide_equalizer),
        delete = activity.getString(R.string.delete),
        exportEqualizer = activity.getString(R.string.export_equalizer),
        equalizerName = activity.getString(R.string.equalizer_name),
        cancel = activity.getString(R.string.cancel),
        enableEqualizer = activity.getString(R.string.enable_equalizer),
        addEqualizer = activity.getString(R.string.add),
        editPreset = activity.getString(R.string.edit_eq_preset),
        undo = activity.getString(R.string.undo),
        preamp = activity.getString(R.string.preamp),
        snapBands = activity.getString(R.string.eq_snap_bands),
        done = activity.getString(R.string.done)
    )

    private fun findEqualizer(id: Long) =
        visibleEqualizers.firstOrNull { it.equalizerEntry.id == id }

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
}

@Composable
private fun EqualizerDialogIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}

private const val EQUALIZER_VALUE_MIN = -20f
private const val EQUALIZER_VALUE_MAX = 20f
