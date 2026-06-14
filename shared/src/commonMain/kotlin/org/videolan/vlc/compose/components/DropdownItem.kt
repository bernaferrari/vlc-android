package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose equivalent of res/layout/dropdown_item.xml
 * Used by ArrayAdapter in dialogs like NetworkServerDialog.
 *
 * Can be used directly in Compose or via interop.
 */
@Composable
fun VLCDropdownItem(
    text: String,
    modifier: Modifier = Modifier
) {
    VLCTheme {
        Box(
            modifier = modifier
                .widthIn(min = 72.dp)
                .heightIn(min = 48.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text)
        }
    }
}
