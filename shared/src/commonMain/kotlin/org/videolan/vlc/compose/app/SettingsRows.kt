@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import org.videolan.vlc.compose.components.VLCSlimSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.videolan.vlc.compose.components.VLCConfirmActionDialog
import org.videolan.vlc.compose.components.VLCModalHeader
import org.videolan.vlc.compose.components.VLCSettingsChoiceRow
import org.videolan.vlc.compose.components.VLCSettingsToggleRow
import org.videolan.vlc.compose.components.vlcSelectionWash
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.settings.SettingsAction
import org.videolan.vlc.settings.SettingsChoice
import org.videolan.vlc.settings.SettingsColor
import org.videolan.vlc.settings.SettingsConfirmation
import org.videolan.vlc.settings.SettingsItem
import org.videolan.vlc.settings.SettingsLink
import org.videolan.vlc.settings.SettingsMultiChoice
import org.videolan.vlc.settings.SettingsNote
import org.videolan.vlc.settings.SettingsNoteTone
import org.videolan.vlc.settings.SettingsOption
import org.videolan.vlc.settings.SettingsSlider
import org.videolan.vlc.settings.SettingsStepper
import org.videolan.vlc.settings.SettingsTextInput
import org.videolan.vlc.settings.SettingsToggle

/** Callbacks shared by every rendered settings row. */
internal class SettingsRowCallbacks(
    val onChange: (SettingsItem, Any) -> Unit,
    val onAction: (SettingsAction) -> Unit,
    val onOpenSection: (String) -> Unit,
)

private val SettingsCardShape = RoundedCornerShape(VLCLayout.GroupOuterCorner)

/** Section title above a grouped card. */
@Composable
internal fun SettingsGroupTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/** One tonal card of rows separated by hairline dividers. */
@Composable
internal fun SettingsCard(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            rows.forEachIndexed { index, row ->
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

@Composable
internal fun SettingsGroupBlock(
    title: String?,
    items: List<SettingsItem>,
    highlightedKey: String?,
    callbacks: SettingsRowCallbacks,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        title?.let { SettingsGroupTitle(it) }
        SettingsCard(items.map { item -> { SettingsItemRow(item, highlightedKey == item.key, callbacks) } })
    }
}

/** Renders any [SettingsItem] with the shared settings vocabulary. */
@Composable
internal fun SettingsItemRow(item: SettingsItem, highlighted: Boolean, callbacks: SettingsRowCallbacks) {
    val wash = Modifier.vlcSelectionWash(highlighted)
    when (item) {
        is SettingsToggle -> VLCSettingsToggleRow(
            title = item.title,
            checked = item.checked,
            onCheckedChange = { callbacks.onChange(item, it) },
            summary = item.summary,
            enabled = item.enabled,
            modifier = wash,
        )
        is SettingsChoice -> ChoiceRow(item, wash, callbacks)
        is SettingsMultiChoice -> MultiChoiceRow(item, wash, callbacks)
        is SettingsStepper -> StepperRow(item, wash, callbacks)
        is SettingsSlider -> SliderRow(item, wash, callbacks)
        is SettingsTextInput -> TextInputRow(item, wash, callbacks)
        is SettingsColor -> ColorRow(item, wash, callbacks)
        is SettingsAction -> SettingsRowFrame(
            title = item.title,
            summary = item.summary,
            enabled = item.enabled,
            modifier = wash,
            titleColor = if (item.destructive) MaterialTheme.colorScheme.error else Color.Unspecified,
            onClick = { callbacks.onAction(item) },
            leading = item.icon?.let { icon ->
                { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
            },
            trailing = if (item.external) {
                { Icon(MaterialSymbols.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            } else null,
        )
        is SettingsLink -> SettingsRowFrame(
            title = item.title,
            summary = item.summary,
            enabled = item.enabled,
            modifier = wash,
            onClick = { callbacks.onOpenSection(item.sectionId) },
            trailing = {
                Icon(MaterialSymbols.AutoMirrored.Filled.ChevronForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
        )
        is SettingsNote -> Text(
            item.text,
            style = MaterialTheme.typography.bodySmall,
            color = when (item.tone) {
                SettingsNoteTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                SettingsNoteTone.Accent -> VLCThemeDefaults.colors.primary
                SettingsNoteTone.Error -> MaterialTheme.colorScheme.error
            },
            modifier = wash.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

/** Common two-line row with an optional leading glyph and trailing control. */
@Composable
internal fun SettingsRowFrame(
    title: String,
    summary: String?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    titleColor: Color = Color.Unspecified,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val disabledAlpha = if (enabled) 1f else .38f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VLCLayout.RowHeight)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val resolvedTitleColor = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor
            Text(title, style = MaterialTheme.typography.bodyLarge, color = resolvedTitleColor.copy(alpha = disabledAlpha))
            summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = disabledAlpha),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            secondary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelLarge,
                    color = VLCThemeDefaults.colors.primary.copy(alpha = disabledAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun ChoiceRow(item: SettingsChoice, modifier: Modifier, callbacks: SettingsRowCallbacks) {
    var open by remember { mutableStateOf(false) }
    val selectedLabel = item.selectedLabel ?: item.selected.orEmpty()
    SettingsRowFrame(
        title = item.title,
        summary = item.summary,
        secondary = selectedLabel,
        enabled = item.enabled,
        modifier = modifier,
        onClick = { open = true },
    )
    if (open) {
        SettingsChoiceSheet(
            title = item.title,
            options = item.options,
            selected = item.selected,
            onSelect = { value ->
                open = false
                if (value != item.selected) callbacks.onChange(item, value)
            },
            onDismiss = { open = false },
        )
    }
}

@Composable
internal fun SettingsChoiceSheet(
    title: String,
    options: List<SettingsOption>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VLCThemeDefaults.colors.backgroundDefault,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = VLCLayout.SheetHorizontalPadding).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VLCModalHeader(title = title, onDismiss = onDismiss)
            val listState = rememberLazyListState()
            LaunchedEffect(Unit) {
                val index = options.indexOfFirst { it.value == selected }
                if (index > 2) listState.scrollToItem(index - 2)
            }
            Surface(shape = SettingsCardShape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                LazyColumn(state = listState, modifier = Modifier.heightIn(max = 520.dp)) {
                    itemsIndexed(options, key = { _, option -> option.value }) { index, option ->
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                        VLCSettingsChoiceRow(
                            title = option.label,
                            summary = option.summary,
                            selected = option.value == selected,
                            onClick = { onSelect(option.value) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(VLCLayout.SheetBottomPadding - 16.dp))
        }
    }
}

@Composable
private fun MultiChoiceRow(item: SettingsMultiChoice, modifier: Modifier, callbacks: SettingsRowCallbacks) {
    var open by remember { mutableStateOf(false) }
    val selectedLabels = item.options.filter { it.value in item.selected }.joinToString(", ") { it.label }
    SettingsRowFrame(
        title = item.title,
        summary = item.summary,
        secondary = selectedLabels.ifBlank { "—" },
        enabled = item.enabled,
        modifier = modifier,
        onClick = { open = true },
    )
    if (!open) return
    var pending by remember(item.selected) { mutableStateOf(item.selected) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { open = false },
        sheetState = sheetState,
        containerColor = VLCThemeDefaults.colors.backgroundDefault,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = VLCLayout.SheetHorizontalPadding).navigationBarsPadding()
                .padding(bottom = VLCLayout.SheetBottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VLCModalHeader(title = item.title, onDismiss = { open = false })
            SettingsCard(item.options.map { option ->
                {
                    val checked = option.value in pending
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .toggleable(value = checked, role = Role.Checkbox) {
                                pending = if (it) pending + option.value else pending - option.value
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            })
            Button(
                onClick = {
                    open = false
                    if (pending != item.selected) callbacks.onChange(item, pending)
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text(ShellStrings.done()) }
        }
    }
}

@Composable
private fun StepperRow(item: SettingsStepper, modifier: Modifier, callbacks: SettingsRowCallbacks) {
    val index = item.options.indexOfFirst { it.value == item.selected }.coerceAtLeast(0)
    SettingsValueStepper(
        title = item.title,
        summary = item.summary,
        value = item.options.getOrNull(index)?.label.orEmpty(),
        decreaseEnabled = item.enabled && index > 0,
        increaseEnabled = item.enabled && index < item.options.lastIndex,
        onDecrease = { callbacks.onChange(item, item.options[index - 1].value) },
        onIncrease = { callbacks.onChange(item, item.options[index + 1].value) },
        modifier = modifier,
    )
}

/** One connected −/value/+ control reads as a single value with two nudges, not three loose glyphs. */
@Composable
internal fun SettingsValueStepper(
    title: String,
    summary: String?,
    value: String,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val decreaseDescription = ShellStrings.decrease(title)
    val increaseDescription = ShellStrings.increase(title)
    Row(
        modifier.fillMaxWidth().heightIn(min = VLCLayout.RowHeight).padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            summary?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton("−", decreaseEnabled, decreaseDescription, onDecrease)
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
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

@Composable
private fun SliderRow(item: SettingsSlider, modifier: Modifier, callbacks: SettingsRowCallbacks) {
    var dragging by remember(item.key) { mutableStateOf(false) }
    var local by remember(item.key) { mutableFloatStateOf(item.value.toFloat()) }
    if (!dragging) local = item.value.coerceIn(item.range.first, item.range.last).toFloat()
    val span = item.range.last - item.range.first
    val steps = if (item.step > 1 && span / item.step <= 100) span / item.step - 1 else 0
    fun snapped(value: Float): Int {
        val raw = value.roundToInt()
        return if (item.step > 1) item.range.first + ((raw - item.range.first).toFloat() / item.step).roundToInt() * item.step else raw
    }
    Column(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (item.enabled) 1f else .38f))
                item.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Text(
                    item.valueLabel(snapped(local)),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        VLCSlimSlider(
            value = local,
            onValueChange = {
                dragging = true
                local = it
            },
            onValueChangeFinished = {
                dragging = false
                val committed = snapped(local).coerceIn(item.range.first, item.range.last)
                if (committed != item.value) callbacks.onChange(item, committed)
            },
            valueRange = item.range.first.toFloat()..item.range.last.toFloat(),
            steps = steps,
            enabled = item.enabled,
        )
    }
}

@Composable
private fun TextInputRow(item: SettingsTextInput, modifier: Modifier, callbacks: SettingsRowCallbacks) {
    var open by remember { mutableStateOf(false) }
    SettingsRowFrame(
        title = item.title,
        summary = item.summary,
        secondary = item.value.ifBlank { item.placeholder ?: "—" },
        enabled = item.enabled,
        modifier = modifier,
        onClick = { open = true },
        trailing = { Icon(MaterialSymbols.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
    )
    if (!open) return
    var draft by remember { mutableStateOf(item.value) }
    AlertDialog(
        onDismissRequest = { open = false },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = { VLCModalHeader(title = item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { value ->
                        val filtered = if (item.numeric) value.filter { it.isDigit() || it == '-' || it == '.' } else value
                        draft = item.maxLength?.let { filtered.take(it) } ?: filtered
                    },
                    placeholder = item.placeholder?.let { { Text(it) } },
                    singleLine = !item.multiline,
                    minLines = if (item.multiline) 3 else 1,
                    maxLines = if (item.multiline) 6 else 1,
                    keyboardOptions = KeyboardOptions(keyboardType = if (item.numeric) KeyboardType.Number else KeyboardType.Text),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                open = false
                if (draft != item.value) callbacks.onChange(item, draft)
            }) { Text(ShellStrings.ok()) }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text(ShellStrings.cancel()) } },
    )
}

internal fun Int.toHexColor(): String = (this and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

private fun parseHexColor(value: String): Int? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it.lowercaseChar() !in "0123456789abcdef" }) return null
    return (0xFF000000.toInt()) or hex.toInt(16)
}

@Composable
private fun ColorSwatch(color: Int, size: androidx.compose.ui.unit.Dp, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val ring = if (selected) VLCThemeDefaults.colors.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(Color(color or 0xFF000000.toInt()))
            .then(if (onClick != null) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier),
    )
}

@Composable
private fun ColorRow(item: SettingsColor, modifier: Modifier, callbacks: SettingsRowCallbacks) {
    var open by remember { mutableStateOf(false) }
    SettingsRowFrame(
        title = item.title,
        summary = item.summary,
        secondary = "#${item.color.toHexColor()}",
        enabled = item.enabled,
        modifier = modifier,
        onClick = { open = true },
        trailing = { ColorSwatch(item.color, 32.dp) },
    )
    if (!open) return
    var draft by remember { mutableStateOf(item.color.toHexColor()) }
    val parsed = parseHexColor(draft)
    AlertDialog(
        onDismissRequest = { open = false },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = { VLCModalHeader(title = item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item.palette.forEach { swatch ->
                        ColorSwatch(
                            color = swatch,
                            size = 44.dp,
                            selected = parsed != null && (parsed and 0xFFFFFF) == (swatch and 0xFFFFFF),
                            onClick = { draft = swatch.toHexColor() },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSwatch(parsed ?: item.color, 44.dp)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.removePrefix("#").take(6) },
                        label = { Text(ShellStrings.hexColor()) },
                        prefix = { Text("#") },
                        isError = parsed == null,
                        supportingText = if (parsed == null) ({ Text(ShellStrings.hexColorInvalid()) }) else null,
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = parsed != null, onClick = {
                open = false
                val value = parsed ?: return@Button
                if ((value and 0xFFFFFF) != (item.color and 0xFFFFFF)) callbacks.onChange(item, value)
            }) { Text(ShellStrings.ok()) }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text(ShellStrings.cancel()) } },
    )
}

/** Confirmation for a pending change or action. */
@Composable
internal fun SettingsConfirmationDialog(
    confirmation: SettingsConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (confirmation.destructive) {
        VLCConfirmActionDialog(
            title = confirmation.title,
            message = confirmation.message,
            target = null,
            confirmLabel = confirmation.confirmLabel,
            cancelLabel = ShellStrings.cancel(),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            title = { VLCModalHeader(title = confirmation.title) },
            text = { Text(confirmation.message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { Button(onClick = onConfirm) { Text(confirmation.confirmLabel) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(ShellStrings.cancel()) } },
        )
    }
}

/** Rounded search pill used at the top of the settings root. */
@Composable
internal fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(MaterialSymbols.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                if (query.isEmpty()) {
                    Text(ShellStrings.settingsSearchHint(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(VLCThemeDefaults.colors.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(MaterialSymbols.Filled.Close, contentDescription = ShellStrings.clear())
                }
            } else {
                Spacer(Modifier.size(12.dp))
            }
        }
    }
}

internal val SettingsListPadding = PaddingValues(top = 8.dp, bottom = VLCLayout.ScreenGutter + 16.dp)
