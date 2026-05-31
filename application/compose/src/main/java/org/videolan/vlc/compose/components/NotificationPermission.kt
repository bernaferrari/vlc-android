package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_norification_permission.xml
 *
 * The app module owns the BottomSheetDialog host, settings persistence, and
 * Android notification permission request. This content only renders the prompt
 * and exposes the positive action.
 */
@Composable
fun VLCNotificationPermissionDialogContent(
    title: String,
    explanation: String,
    okText: String,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit = { NotificationIconPlaceholder() }
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
                    .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            iconContent()
                        }
                    }
                    Text(
                        text = title,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, end = 16.dp)
                    )
                }

                Text(
                    text = explanation,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onOk) {
                        Text(okText)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationIconPlaceholder() {
    Spacer(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}

@Preview(name = "Notification Permission", showBackground = true)
@Composable
private fun VLCNotificationPermissionDialogContentPreview() {
    VLCNotificationPermissionDialogContent(
        title = "Notification permission",
        explanation = "VLC needs your permission to send notifications.\nThe app will only notify you when scanning your media or using the custom video Picture in Picture.\nRefusing it won't prevent VLC to work, however the notifications will be hidden.",
        okText = "OK",
        onOk = {}
    )
}

@Preview(name = "Notification Permission Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCNotificationPermissionDialogContentDarkPreview() {
    VLCNotificationPermissionDialogContent(
        title = "Notification permission",
        explanation = "VLC needs your permission to send notifications.\nThe app will only notify you when scanning your media or using the custom video Picture in Picture.\nRefusing it won't prevent VLC to work, however the notifications will be hidden.",
        okText = "OK",
        onOk = {}
    )
}
