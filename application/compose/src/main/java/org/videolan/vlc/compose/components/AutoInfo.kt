package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                AutoInfoSection(
                    title = podcastModeTitle,
                    body = podcastModeText,
                    modifier = Modifier.padding(top = 24.dp)
                )

                AutoInfoSection(
                    title = voiceControlTitle,
                    body = voiceControlText,
                    modifier = Modifier.padding(top = 16.dp)
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
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = body,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Preview(name = "Auto Info", showBackground = true)
@Composable
private fun VLCAutoInfoDialogContentPreview() {
    VLCAutoInfoDialogContent(
        title = "Android Auto",
        podcastModeTitle = "Podcast mode",
        podcastModeText = "Podcast controls appear when playing a single track which meets one of the following criteria:\n\t- Length greater than 60 min\n\t- Length greater than 15 min with no album name\n\t- Genre is audiobook(s), podcast, speech, or vocal",
        voiceControlTitle = "Voice control",
        voiceControlText = "VLC can be controlled through Google Assistant by speaking the following commands:\n\t- Play, Pause, Stop, Previous, Next\n\t- Rewind, Fast Forward, Repeat <all|one|off>\n\t- Jump to <time>, Skip <duration>\n\t- What's this <artist|album|song>\n\t- Play [artist|album|song|genre|playlist] <name>"
    )
}

@Preview(name = "Auto Info Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCAutoInfoDialogContentDarkPreview() {
    VLCAutoInfoDialogContent(
        title = "Android Auto",
        podcastModeTitle = "Podcast mode",
        podcastModeText = "Podcast controls appear when playing a single track which meets one of the following criteria:\n\t- Length greater than 60 min\n\t- Length greater than 15 min with no album name\n\t- Genre is audiobook(s), podcast, speech, or vocal",
        voiceControlTitle = "Voice control",
        voiceControlText = "VLC can be controlled through Google Assistant by speaking the following commands:\n\t- Play, Pause, Stop, Previous, Next\n\t- Rewind, Fast Forward, Repeat <all|one|off>\n\t- Jump to <time>, Skip <duration>\n\t- What's this <artist|album|song>\n\t- Play [artist|album|song|genre|playlist] <name>"
    )
}
