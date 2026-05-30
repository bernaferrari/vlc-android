package org.videolan.vlc.compose

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCDropdownItem
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Preview utilities for the VLC Compose module.
 *
 * These @Preview functions make the basic theme + components immediately
 * visible and testable inside Android Studio (no device required).
 *
 * Expanded with light/dark demonstrations of the real mapped tokens from
 * the original VLC app (see VLCTheme.kt for full traceability to
 * colors.xml + attrs.xml + styles.xml + layout usage).
 */

@Preview(
    name = "VLC Theme - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 200
)
@Composable
fun VLCThemeLightPreview() {
    VLCTheme(darkTheme = false) {
        PreviewContent()
    }
}

@Preview(
    name = "VLC Theme - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 200
)
@Composable
fun VLCThemeDarkPreview() {
    VLCTheme(darkTheme = true) {
        PreviewContent()
    }
}

@Composable
private fun PreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "VLC Compose Bootstrap")
        Spacer(modifier = Modifier.height(8.dp))
        VLCDropdownItem(text = "Example dropdown item")
        Spacer(modifier = Modifier.height(4.dp))
        VLCDropdownItem(text = "Another item")
    }
}

// ============================================================
// Expanded token demonstration previews (light + dark variants)
// These exercise the key semantic tokens extracted for media lists,
// player chrome, about, dialogs, onboarding.
// ============================================================

@Preview(
    name = "Media List Items - Light (real tokens)",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 280
)
@Composable
fun MediaListLightPreview() {
    VLCTheme(darkTheme = false) {
        MediaListTokensDemo()
    }
}

@Preview(
    name = "Media List Items - Dark (real tokens)",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 280
)
@Composable
fun MediaListDarkPreview() {
    VLCTheme(darkTheme = true) {
        MediaListTokensDemo()
    }
}

@Composable
private fun MediaListTokensDemo() {
    val c = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .background(c.backgroundDefault)
            .padding(8.dp)
    ) {
        // Simulates browser_item.xml title + subtitle using ?attr/list_title / list_subtitle
        Text(
            text = "Folder / Album Name",
            color = c.listTitle,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Artist • 12 tracks • 48:12",
            color = c.listSubtitle,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
        Spacer(Modifier.height(12.dp))
        // Divider using default_divider token
        Divider(color = c.defaultDivider, thickness = 0.5.dp)
        Spacer(Modifier.height(12.dp))
        // Another row with font tokens
        Text("Song Title Here", color = c.fontDefault, style = MaterialTheme.typography.bodyLarge)
        Text("2:34", color = c.fontLight, style = MaterialTheme.typography.labelMedium)
    }
}

@Preview(
    name = "Audio Player Chrome Mock - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 220
)
@Composable
fun PlayerChromeLightPreview() {
    VLCTheme(darkTheme = false) {
        PlayerChromeTokensDemo()
    }
}

@Preview(
    name = "Audio Player Chrome Mock - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 220
)
@Composable
fun PlayerChromeDarkPreview() {
    VLCTheme(darkTheme = true) {
        PlayerChromeTokensDemo()
    }
}

@Composable
private fun PlayerChromeTokensDemo() {
    val c = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .background(c.backgroundDefault)
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        // Header area (audio_header_background + divider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.audioHeaderBackground)
                .padding(8.dp)
        ) {
            Text("Now Playing - Audio Title", color = c.fontDefault)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.audioHeaderDivider)
        )

        Spacer(Modifier.height(16.dp))

        // Chips row using audio_chips_* tokens (from audio_player.xml)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipMock("Speed 1.0x", c.audioChipsColor, c.audioChipsTextColor)
            ChipMock("Audio", c.audioChipsColor, c.audioChipsTextColor)
        }

        Spacer(Modifier.height(16.dp))

        // Player icons + controls using player_icon_color
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconMock("▶", c.playerIconColor)
            IconMock("⏸", c.playerIconColor)
            IconMock("⏭", c.playerIconColor)
            // Subtle selection example
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(c.subtleSelection, RoundedCornerShape(4.dp))
            )
        }

        Spacer(Modifier.height(12.dp))
        // Progress using audio seek etc (color approx)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(c.primary.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun ChipMock(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun IconMock(symbol: String, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = tint)
    }
}

@Preview(
    name = "About Screen Card - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun AboutCardLightPreview() {
    VLCTheme(darkTheme = false) {
        AboutCardTokensDemo()
    }
}

@Preview(
    name = "About Screen Card - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun AboutCardDarkPreview() {
    VLCTheme(darkTheme = true) {
        AboutCardTokensDemo()
    }
}

@Composable
private fun AboutCardTokensDemo() {
    val c = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(c.backgroundDefaultDarker, RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        // Uses about_text_primary (black light / orange dark) + font tokens
        Text(
            "VLC for Android 3.6.0",
            color = c.aboutTextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "VideoLAN & contributors • 2024",
            color = c.fontLight,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Divider(color = c.defaultDivider)
        Spacer(Modifier.height(8.dp))
        Text(
            "Libre and open source. Based on libvlc.",
            color = c.fontLight
        )
    }
}

@Preview(
    name = "Dialog / Empty State Tokens - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 160
)
@Composable
fun DialogEmptyLightPreview() {
    VLCTheme(darkTheme = false) {
        DialogEmptyDemo()
    }
}

@Preview(
    name = "Dialog / Empty State Tokens - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 160
)
@Composable
fun DialogEmptyDarkPreview() {
    VLCTheme(darkTheme = true) {
        DialogEmptyDemo()
    }
}

@Composable
private fun DialogEmptyDemo() {
    val c = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .background(c.backgroundDefault)
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // empty_* tokens + font
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(c.emptyBackground, RoundedCornerShape(24.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text("No media found", color = c.emptyTitle, style = MaterialTheme.typography.titleMedium)
        Text(
            "Your library is empty",
            color = c.fontLight,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        // primary focus example
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(36.dp)
                .background(c.primaryFocus, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("SCAN", color = c.primary)
        }
    }
}
