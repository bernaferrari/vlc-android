package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 *
 * Material 3 Expressive redesign: a centered hero — a large accent icon disc above
 * a centered title, with the explanation in a soft tonal callout and a full-width
 * filled action, replacing the former icon-jammed-beside-title flat layout.
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
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            iconContent()
                        }
                    }
                }

                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
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
                        textAlign = TextAlign.Center,
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
private fun NotificationIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}
