package org.videolan.vlc.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose presentational leaf for dialog content.
 *
 * Focused subset of application/vlc-android/res/layout/dialog_confirm_delete.xml
 * (the warning icon + bold title + descriptive message).
 *
 * The original also contains action Buttons (delete/cancel) and a delete_animation
 * ImageView + ConstraintLayout + Barrier. Those are intentionally omitted here:
 * in Compose migration the dialog scaffolding (AlertDialog / ModalBottomSheet)
 * + actions will be provided by the caller using Material3 components.
 *
 * This enables reuse of the "are you sure you want to delete X?" messaging
 * pattern across multiple delete confirmation sites.
 *
 * @param title Localized confirm title (defaults to typical "Confirm delete")
 * @param message Explanation text (e.g. "This will permanently delete...")
 * @param iconContent Optional warning icon (caller supplies, e.g. Icon(Icons.Default.Warning))
 */
@Composable
fun VLCDialogConfirmDelete(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable (() -> Unit)? = null
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (iconContent != null) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        iconContent()
                    }
                }

                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                color = colors.fontLight,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
