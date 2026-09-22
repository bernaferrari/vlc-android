package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon

/**
 * One action in a compact, connected control group. Library screens use this
 * instead of scattering unrelated icon buttons across a header.
 */
data class VLCConnectedIconAction(
    val icon: MaterialIcon,
    val contentDescription: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

/** A text filter in a horizontally scrollable, connected section selector. */
data class VLCSectionOption(
    val label: String,
    val contentDescription: String = label,
)

/**
 * Quiet utility actions keep header chrome secondary to the page title. This deliberately
 * keeps every target at the Material-recommended 48dp touch size.
 */
@Composable
fun VLCConnectedIconActionBar(
    actions: List<VLCConnectedIconAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        actions.forEach { action ->
            VLCConnectedControlSurface(
                selected = action.selected,
                shape = RoundedCornerShape(24.dp),
                onClick = action.onClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    icon = action.icon,
                    contentDescription = action.contentDescription,
                )
            }
        }
    }
}

/**
 * Replaces the legacy underline tab row with a compact selector that can safely overflow on a
 * narrow phone. A selector remains a selector on every target—there is no Web-only tab design.
 */
@Composable
fun VLCSectionSelector(
    options: List<VLCSectionOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        itemsIndexed(options, key = { _, option -> option.label }) { index, option ->
            val selected = index == selectedIndex
            VLCConnectedControlSurface(
                selected = selected,
                shape = RoundedCornerShape(12.dp),
                onClick = { onSelect(index) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                        this.contentDescription = option.contentDescription
                    },
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    // Selection is already visible through the filled chip; keep the label
                    // weight stable so switching tabs never nudges neighboring text.
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun VLCConnectedControlSurface(
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
