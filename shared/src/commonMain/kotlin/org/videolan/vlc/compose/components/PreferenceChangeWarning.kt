package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_confirm_audio_playqueue.xml
 *
 * The app module owns result delivery back to the active preferences host. This content
 * keeps the warning icon, title, message, and OK/Cancel action surface.
 *
 * Material 3 Expressive redesign: a caution-toned hero icon disc leads the title and the
 * message is lifted into a soft tonal callout, replacing the former flat icon-beside-title
 * row with a fixed 72dp gap before the buttons.
 */
@Composable
fun VLCPreferenceChangeWarningDialogContent(
    title: String,
    message: String,
    okText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable (() -> Unit)? = null
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefault)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (iconContent != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    iconContent()
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                }

                VLCMessageCallout(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )

                PreferenceChangeWarningActions(
                    okText = okText,
                    cancelText = cancelText,
                    onConfirm = onConfirm,
                    onCancel = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceChangeWarningActions(
    okText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onCancel) {
            Text(cancelText)
        }
        Button(onClick = onConfirm) {
            Text(okText)
        }
    }
}

@Composable
private fun PreviewWarningIcon() {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(Color.Gray)
    )
}
