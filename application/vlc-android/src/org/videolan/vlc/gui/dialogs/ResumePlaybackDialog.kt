package org.videolan.vlc.gui.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.appcompat.app.AppCompatDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

fun Context.showResumePlaybackComposeDialog(
    title: String,
    cancelable: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onApplyToPlayQueueChanged: (Boolean) -> Unit
): AppCompatDialog {
    val dialog = AppCompatDialog(this)
    dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(cancelable)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.setContentView(
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    ResumePlaybackDialog(
                        title = title,
                        onResume = {
                            onResume()
                            dialog.dismiss()
                        },
                        onRestart = {
                            onRestart()
                            dialog.dismiss()
                        },
                        onApplyToPlayQueueChanged = onApplyToPlayQueueChanged
                    )
                }
            }
        }
    )
    dialog.show()
    return dialog
}

@Composable
private fun ResumePlaybackDialog(
    title: String,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onApplyToPlayQueueChanged: (Boolean) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            Text(
                text = title,
                color = colors.fontDefault,
                style = MaterialTheme.typography.headlineSmall
            )
            ResumePlaybackDialogContent(onApplyToPlayQueueChanged = onApplyToPlayQueueChanged)
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                TextButton(onClick = onRestart) {
                    Text(text = stringResource(R.string.no))
                }
                Button(
                    onClick = onResume,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = stringResource(R.string.resume))
                }
            }
        }
    }
}

@Composable
private fun ResumePlaybackDialogContent(onApplyToPlayQueueChanged: (Boolean) -> Unit) {
    val colors = VLCThemeDefaults.colors
    var applyToPlayQueue by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
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
