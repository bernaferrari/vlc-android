package org.videolan.vlc.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

enum class VLCAudioTrackInfoTextStyle {
    Title,
    Subtitle,
    Detail
}

/**
 * Compose equivalent of the landscape audio-player title, subtitle, and
 * technical-detail text formerly hosted as child TextViews inside the
 * landscape track-info island.
 *
 * The host now renders these rows inside the direct track_info_container
 * Compose island. This leaf owns VLC text colors, sizes, one-line marquee
 * behavior, and optional click handling for the title/subtitle text-click
 * shortcut.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCAudioTrackInfoText(
    text: String,
    style: VLCAudioTrackInfoTextStyle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = VLCThemeDefaults.colors
    VLCTheme {
        Text(
            text = text,
            color = when (style) {
                VLCAudioTrackInfoTextStyle.Title -> colors.fontDefault
                VLCAudioTrackInfoTextStyle.Subtitle,
                VLCAudioTrackInfoTextStyle.Detail -> colors.fontAudioLight
            },
            fontSize = when (style) {
                VLCAudioTrackInfoTextStyle.Title -> 24.sp
                VLCAudioTrackInfoTextStyle.Subtitle -> 14.sp
                VLCAudioTrackInfoTextStyle.Detail -> 12.sp
            },
            fontWeight = if (style == VLCAudioTrackInfoTextStyle.Title) FontWeight.Light else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = modifier
                .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
                .basicMarquee(iterations = 1)
                .clearAndSetSemantics { }
        )
    }
}

@Preview(
    name = "VLCAudioTrackInfoText - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 260,
    heightDp = 96
)
@Composable
fun VLCAudioTrackInfoTextLightPreview() {
    VLCTheme(darkTheme = false) {
        AudioTrackInfoTextPreviewContent()
    }
}

@Preview(
    name = "VLCAudioTrackInfoText - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 260,
    heightDp = 96
)
@Composable
fun VLCAudioTrackInfoTextDarkPreview() {
    VLCTheme(darkTheme = true) {
        AudioTrackInfoTextPreviewContent()
    }
}

@Composable
private fun AudioTrackInfoTextPreviewContent() {
    androidx.compose.foundation.layout.Column {
        VLCAudioTrackInfoText(
            text = "5th Symphony",
            style = VLCAudioTrackInfoTextStyle.Title
        )
        VLCAudioTrackInfoText(
            text = "Beethoven - Album",
            style = VLCAudioTrackInfoTextStyle.Subtitle
        )
        VLCAudioTrackInfoText(
            text = "Bitrate: 22.4 KB/s - Codec: Vorbis audio - Sample rate 8000 Hz",
            style = VLCAudioTrackInfoTextStyle.Detail
        )
    }
}
