package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RetryMessage(error: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(ShellStrings.retry()) }
    }
}
