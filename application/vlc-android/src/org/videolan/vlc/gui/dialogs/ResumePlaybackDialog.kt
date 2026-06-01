package org.videolan.vlc.gui.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

fun Context.createResumePlaybackDialogView(onApplyToPlayQueueChanged: (Boolean) -> Unit): ComposeView {
    return ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ResumePlaybackDialogContent(onApplyToPlayQueueChanged = onApplyToPlayQueueChanged)
        }
    }
}

@Composable
private fun ResumePlaybackDialogContent(onApplyToPlayQueueChanged: (Boolean) -> Unit) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        var applyToPlayQueue by rememberSaveable { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.confirm_resume),
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .toggleable(
                        value = applyToPlayQueue,
                        role = Role.Checkbox,
                        onValueChange = {
                            applyToPlayQueue = it
                            onApplyToPlayQueueChanged(it)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = applyToPlayQueue,
                    onCheckedChange = null
                )
                Text(
                    text = stringResource(R.string.apply_playqueue),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
