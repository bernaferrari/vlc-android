package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import org.videolan.vlc.compose.components.VLCStatePlaceholder
import org.videolan.vlc.compose.components.VLCStatePlaceholderTone
import org.videolan.vlc.compose.icons.MaterialSymbols

/** A recoverable inline failure using the same hierarchy as every other app state. */
@Composable
internal fun RetryMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VLCStatePlaceholder(
        title = error,
        modifier = modifier.fillMaxWidth(),
        symbol = MaterialSymbols.Filled.Warning,
        tone = VLCStatePlaceholderTone.Error,
        actionText = ShellStrings.retry(),
        onActionClick = onRetry,
        compact = true,
    )
}
