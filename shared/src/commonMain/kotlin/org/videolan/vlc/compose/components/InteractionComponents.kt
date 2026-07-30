package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A simple in-place disclosure for utility forms. Forms are often opened and closed repeatedly;
 * changing layout height there made the surrounding grouped rows jump and compete with the
 * deliberate Nav3 detail transition. Keeping the state change immediate is calmer and avoids
 * layout-property animation on a scrolling screen.
 */
@Composable
fun VLCExpandableContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (visible) {
        Box(modifier = modifier) { content() }
    }
}
