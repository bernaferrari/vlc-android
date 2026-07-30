package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols

/**
 * Shared contextual selection header for library, browser, and playlist lists.
 *
 * A selection is a temporary mode, not another filter row. This replaces the
 * normal screen identity while active, matching Markor's focused contextual
 * toolbar pattern and keeping every action within one compact surface.
 */
@Composable
fun VLCSelectionContextBar(
    title: String,
    clearContentDescription: String,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = VLCListItemPosition.Single.segmentShape(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(MaterialSymbols.Filled.Close, contentDescription = clearContentDescription)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
