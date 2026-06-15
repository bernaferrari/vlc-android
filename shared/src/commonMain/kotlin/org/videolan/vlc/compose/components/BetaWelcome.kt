package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/activity_beta_welcome.xml
 *
 * This is intentionally a full dialog Activity surface rather than an interop leaf:
 * BetaWelcomeActivity can drop DataBinding and the XML layout entirely while keeping
 * its existing dialog theme, localized text, VLC icon slot, and finish-on-OK behavior.
 *
 * Material 3 Expressive redesign: a branded hero icon disc + title lead into the
 * description; the crash warning is lifted into an emphasized tonal callout card so
 * the "expect bugs" message reads as a deliberate caution rather than another
 * paragraph. A full-width action anchors the sheet.
 */
@Composable
fun VLCBetaWelcomeScreen(
    title: String,
    description: String,
    bugsDescription: String,
    crashWarning: String,
    okText: String,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit = { DefaultBetaWelcomeIcon() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier
                .width(320.dp)
                .fillMaxHeight(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        iconContent()
                    }
                }

                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 20.dp)
                )

                Text(
                    text = description,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Text(
                    text = bugsDescription,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                WarningCallout(
                    text = crashWarning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = okText)
                }
            }
        }
    }
}

/**
 * Emphasized tonal callout used to highlight a cautionary message: a leading accent
 * bar in a tertiary-tinted container with bold-weighted text.
 */
@Composable
private fun WarningCallout(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun DefaultBetaWelcomeIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(VLCThemeDefaults.colors.primary)
    )
}
