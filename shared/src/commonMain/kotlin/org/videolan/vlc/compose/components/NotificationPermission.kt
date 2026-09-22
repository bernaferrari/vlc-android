package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_norification_permission.xml
 *
 * The app module owns the BottomSheetDialog host, settings persistence, and
 * Android notification permission request. This content only renders the prompt
 * and exposes the positive action.
 *
 * The shared modal header and tonal explanation match the other permission and warning
 * surfaces. The full-width action remains reachable through compact-window scrolling.
 */
@Composable
fun VLCNotificationPermissionDialogContent(
    title: String,
    explanation: String,
    okText: String,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit = { DefaultNotificationIcon() }
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
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VLCModalHeader(
                    title = title,
                    icon = {
                        VLCIconChip {
                            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { iconContent() }
                        }
                    },
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = colors.fontDefault
                ) {
                    Text(
                        text = explanation,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text(okText)
                }
            }
        }
    }
}

@Composable
private fun DefaultNotificationIcon() = Icon(
    icon = MaterialSymbols.Filled.Notifications,
    contentDescription = null,
    modifier = Modifier.size(36.dp),
)
