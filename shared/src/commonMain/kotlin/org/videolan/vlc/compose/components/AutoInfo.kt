package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * - application/vlc-android/res/layout/dialog_auto_info.xml
 *
 * The app module owns the BottomSheetDialog host and localized string formatting.
 * This content renders the Android Auto help sections without requiring a
 * DataBinding layout.
 *
 * Material 3 Expressive redesign: the former flat wall of text is grouped into
 * tonal cards, each headed by an accent indicator + colored section title, so the
 * podcast / voice-control sections read as scannable, distinct topics.
 */
@Composable
fun VLCAutoInfoDialogContent(
    title: String,
    podcastModeTitle: String,
    podcastModeText: String,
    voiceControlTitle: String,
    voiceControlText: String,
    modifier: Modifier = Modifier
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
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )

                AutoInfoSection(
                    title = podcastModeTitle,
                    body = podcastModeText,
                    modifier = Modifier.padding(top = 20.dp)
                )

                AutoInfoSection(
                    title = voiceControlTitle,
                    body = voiceControlText,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AutoInfoSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = colors.fontDefault
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    color = colors.primary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.defaultDivider)
            )

            Text(
                text = body,
                color = colors.fontDefault,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }
    }
}
