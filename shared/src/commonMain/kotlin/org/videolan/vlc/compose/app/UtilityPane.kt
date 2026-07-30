package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.videolan.vlc.compose.theme.VLCLayout

/**
 * Keeps utility screens composed on a rail/tablet layout without constraining media grids or the
 * adaptive library list-detail scene. This is intentionally top-aligned: settings retain their
 * familiar document flow while empty media states own their vertical centering.
 */
@Composable
internal fun VLCUtilityPane(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = VLCLayout.ListMaxWidth)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}
