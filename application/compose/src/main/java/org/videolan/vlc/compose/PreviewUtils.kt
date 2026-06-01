package org.videolan.vlc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import org.videolan.vlc.compose.components.VLCDebugLogLine
import org.videolan.vlc.compose.components.VLCDialogConfirmDelete
import org.videolan.vlc.compose.components.VLCInfoItem
import org.videolan.vlc.compose.components.VLCAudioHeaderActionButton
import org.videolan.vlc.compose.components.VLCAudioHeaderPlayPauseButton
import org.videolan.vlc.compose.components.VLCAudioHeaderTimeLabel
import org.videolan.vlc.compose.components.VLCAudioHeaderTransportButton
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPill
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPillState
import org.videolan.vlc.compose.components.VLCAudioPlayerChips
import org.videolan.vlc.compose.components.VLCAudioPlayerChipsState
import org.videolan.vlc.compose.components.VLCOnboardingWelcome
import org.videolan.vlc.compose.components.VLCSectionHeader
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
        Spacer(modifier = Modifier.height(12.dp))
        VLCSectionHeader(text = "Section Example")
        Spacer(modifier = Modifier.height(8.dp))
        VLCDebugLogLine(text = "12:34:56.789 [info] compose leaf ready")
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Now Playing - Audio Title", color = c.fontDefault)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VLCAudioHeaderTimeLabel(text = "10:42")
                    VLCAudioHeaderActionButton(contentDescription = "Search") {
                        IconMock("S", c.audioMenuIcon)
                    }
                    VLCAudioHeaderActionButton(contentDescription = "Show playlist") {
                        IconMock("P", c.primary)
                    }
                    VLCAudioHeaderActionButton(contentDescription = "Advanced") {
                        IconMock("⋮", c.audioMenuIcon)
                    }
                    VLCAudioHeaderPlayPauseButton(contentDescription = "Pause") {
                        IconMock("Ⅱ", c.playerIconColor)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.audioHeaderDivider)
        )

        Spacer(Modifier.height(16.dp))

        // Real Wave 2 chips leaf using audio_chips_* tokens (from audio_player.xml)
        VLCAudioPlayerChips(
            state = VLCAudioPlayerChipsState(
                speedText = "1.25x",
                sleepText = "12:55 AM",
                speedUsesGlobalRate = true
            ),
            speedIconContent = { Text("S", color = c.audioChipsTextColor) },
            sleepIconContent = { Text("Z", color = c.audioChipsTextColor) }
        )

        Spacer(Modifier.height(16.dp))

        // Player icons + controls using player_icon_color
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            VLCAudioHeaderTransportButton(contentDescription = "Shuffle") {
                IconMock("S", c.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Previous") {
                IconMock("<", c.playerIconColor)
            }
            VLCAudioHeaderTransportButton(
                contentDescription = "Pause",
                size = 56.dp
            ) {
                IconMock("P", c.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Next") {
                IconMock(">", c.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Repeat") {
                IconMock("R", c.playerIconColor)
            }
            // Subtle selection example
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(c.subtleSelection, RoundedCornerShape(4.dp))
            )
        }

        Spacer(Modifier.height(12.dp))
        VLCAudioQueueProgressPill(
            state = VLCAudioQueueProgressPillState(
                text = "Track 3 / 12  •  10:42 / 48:12",
                contentDescription = "Track 3 of 12. 10 minutes 42 seconds out of 48 minutes 12 seconds."
            )
        )

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

// ============================================================
// Wave 1 Leaf Composables Previews (high-leverage list/dialog/onboarding leaves)
// All demonstrate light + dark using real VLC tokens + KDoc traceability
// ============================================================

@Preview(
    name = "VLCSectionHeader - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 120
)
@Composable
fun VLCSectionHeaderLightPreview() {
    VLCTheme(darkTheme = false) {
        Column(Modifier.padding(8.dp)) {
            VLCSectionHeader(text = "Recently Played")
            Spacer(Modifier.height(8.dp))
            VLCSectionHeader(text = "Audio Books & Long Titles That Wrap Or Ellipsize Here")
        }
    }
}

@Preview(
    name = "VLCSectionHeader - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 120
)
@Composable
fun VLCSectionHeaderDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column(Modifier.padding(8.dp)) {
            VLCSectionHeader(text = "Songs")
            Spacer(Modifier.height(8.dp))
            VLCSectionHeader(text = "Playlists")
        }
    }
}

@Preview(
    name = "VLCInfoItem - Light (Media Tracks)",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 160
)
@Composable
fun VLCInfoItemLightPreview() {
    VLCTheme(darkTheme = false) {
        Column(Modifier.padding(8.dp)) {
            VLCInfoItem(
                title = "Audio",
                subtitle = "Bitrate: 320 kb/s • Codec: mp3 • Channels: 2 • Language: eng",
                leadingContent = { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCInfoItem(
                title = "Video",
                subtitle = "1920×1080 • 23.97 fps • Codec: h264",
                leadingContent = { Text("📺", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCInfoItem(
                title = "Text",
                subtitle = "Language: eng • Codec: subrip"
            )
        }
    }
}

@Preview(
    name = "VLCInfoItem - Dark (Media Tracks)",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 160
)
@Composable
fun VLCInfoItemDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column(Modifier.padding(8.dp)) {
            VLCInfoItem(
                title = "Audio",
                subtitle = "Bitrate: 320 kb/s • Codec: aac • Channels: 2",
                leadingContent = { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
        }
    }
}

// ============================================================
// WAVE 1.3 ENHANCEMENT (compose-2l4.1.3 / bd compose-l94)
// Realistic "Media Info Track List" mocks simulating exactly what
// MediaInfoAdapter renders for a video file's track list inside
// InfoActivity (the primary host surface for this leaf).
// These exercise the leadingContent slot mapping (♪ 📺 📝) +
// realistic subtitle strings that come from appendCommon/append* helpers.
// Directly referenced from the new comments in MediaInfoAdapter.kt
// and the combined mock already present in ComposeInteropLabActivity.
// ============================================================

@Preview(
    name = "Media Info Track List Mock (MediaInfoAdapter simulation) - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 260
)
@Composable
fun MediaInfoTrackListLightPreview() {
    VLCTheme(darkTheme = false) {
        Column(Modifier.padding(4.dp).background(VLCThemeDefaults.colors.backgroundDefault)) {
            // Simulates the exact rows MediaInfoAdapter produces for a typical movie
            VLCInfoItem(
                title = "Video",
                subtitle = "1920×1080 • 23.98 fps • Codec: h264 • 8.2 Mbps",
                leadingContent = { Text("📺", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCInfoItem(
                title = "Audio",
                subtitle = "Bitrate: 320 kb/s • Codec: aac • Channels: 2 • Language: eng",
                leadingContent = { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCInfoItem(
                title = "Audio",
                subtitle = "Bitrate: 192 kb/s • Codec: ac3 • Channels: 6 • Language: eng",
                leadingContent = { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCInfoItem(
                title = "Text",
                subtitle = "Language: eng • Codec: subrip • Forced: no • Default: yes"
            )
            VLCInfoItem(
                title = "Text",
                subtitle = "Language: spa • Codec: subrip • Forced: no"
            )
        }
    }
}

@Preview(
    name = "Media Info Track List Mock (MediaInfoAdapter simulation) - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 260
)
@Composable
fun MediaInfoTrackListDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column(Modifier.padding(4.dp).background(VLCThemeDefaults.colors.backgroundDefault)) {
            VLCInfoItem(
                title = "Video",
                subtitle = "1920×1080 • 23.98 fps • Codec: h264 • 8.2 Mbps",
                leadingContent = { Text("📺", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCInfoItem(
                title = "Audio",
                subtitle = "Bitrate: 320 kb/s • Codec: aac • Channels: 2 • Language: eng",
                leadingContent = { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCInfoItem(
                title = "Text",
                subtitle = "Language: eng • Codec: subrip • Forced: no • Default: yes"
            )
        }
    }
}

@Preview(
    name = "VLCDebugLogLine - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 140
)
@Composable
fun VLCDebugLogLineLightPreview() {
    VLCTheme(darkTheme = false) {
        Column(Modifier.padding(4.dp)) {
            VLCDebugLogLine(text = "VLC media player 3.6.0 Vetinari")
            VLCDebugLogLine(text = "[8f3a2b] main libvlc: Running vlc with the default interface")
            VLCDebugLogLine(text = "[warn] direct3d11 vout: Failed to create texture (hr=0x80070057)")
        }
    }
}

@Preview(
    name = "VLCDebugLogLine - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 140
)
@Composable
fun VLCDebugLogLineDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column(Modifier.padding(4.dp)) {
            VLCDebugLogLine(text = "I/DEBUG: signal 11 (SIGSEGV), code 1 (SEGV_MAPERR)")
            VLCDebugLogLine(text = "D/VLC: [h264 @ 0x7f8b2c] Profile: High@5.1")
        }
    }
}

@Preview(
    name = "VLCDialogConfirmDelete - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 200
)
@Composable
fun VLCDialogConfirmDeleteLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCDialogConfirmDelete(
            title = "Confirm delete",
            message = "This will permanently delete the selected media from your device. This action cannot be undone.",
            iconContent = { Text("⚠", style = MaterialTheme.typography.headlineMedium) }
        )
    }
}

@Preview(
    name = "VLCDialogConfirmDelete - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 200
)
@Composable
fun VLCDialogConfirmDeleteDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCDialogConfirmDelete(
            title = "Delete forever",
            message = "The file will be removed from storage.",
            iconContent = { Text("🗑", style = MaterialTheme.typography.headlineMedium) }
        )
    }
}

// ============================================================
// WAVE 1.6 ONBOARDING PREVIEW ENHANCEMENT (compose-2l4.1.6 / bd compose-mdj)
// The dark onboarding-background previews are now excellent:
//   - Explicit Box using VLCThemeDefaults.colors.onboardingBackground (the real token
//     #011422 from DarkVLCColors / LightVLCColors, matching @color/onboarding_grey +
//     Theme.VLC.Onboarding.* styles used by the original XMLs).
//   - Larger realistic height (640dp) simulating a phone screen for the welcome step.
//   - Light variant also wrapped for visual parity (even though onboarding forces dark).
//   - These previews (plus the Lab's real-logo variant) now directly exercise the
//     title/subtitle usage that the first-run Compose onboarding flow and the
//     Interop Lab employ.
//   - No drawable refs here (Wave-1-safe: previews stay self-contained in :compose;
//     real painterResource mapping lives only in vlc-android hosts: fragment + Lab).
// This advances the "preview + gate enforcement" + "richer usage mocks" criteria.
// ============================================================

@Preview(
    name = "VLCOnboardingWelcome - Light (static parts)",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun VLCOnboardingWelcomeLightPreview() {
    VLCTheme(darkTheme = false) {
        // Excellent preview wrapper: exercises the token even in light (for completeness).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VLCThemeDefaults.colors.onboardingBackground)
        ) {
            VLCOnboardingWelcome(
                title = "Welcome to VLC for Android",
                subtitle = "The best open source video and audio player, now on your mobile device."
            )
        }
    }
}

@Preview(
    name = "VLCOnboardingWelcome - Dark (static parts) — excellent onboardingBackground token",
    showBackground = true,
    backgroundColor = 0xFF011422,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun VLCOnboardingWelcomeDarkPreview() {
    VLCTheme(darkTheme = true) {
        // Excellent dark preview: full token fidelity for the first-run welcome screen.
        // Matches the deep blue-grey used by the onboarding activity's forced dark bars.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VLCThemeDefaults.colors.onboardingBackground)
        ) {
            VLCOnboardingWelcome(
                title = "Welcome to VLC for Android",
                subtitle = "Play everything. Free. No ads. No tracking."
            )
        }
    }
}

// ============================================================
// WAVE 1.8 RICHER USAGE MOCKS (derived from Compose Interop Lab host)
// compose-2l4.1.8 cross-cutting: these are the "richer usage mocks" requirement.
// WAVE 1.6 addition (compose-2l4.1.6 / bd compose-mdj): OnboardingWelcome*Previews
// strengthened with excellent dark onboardingBackground token wrappers + real host
// context (full Compose first-run flow + Lab real-logo variant).
// WAVE 1.3 addition (compose-2l4.1.3): MediaInfoTrackList*Previews were added here
// (and exercised by the Lab's combined mock) as part of the MediaInfoAdapter host migration.
// They are extracted / inspired directly from the live interactive examples
// in ComposeInteropLabActivity.kt (the crown jewel dev-only Lab launched
// from DebugLogActivity). They provide:
//   - Combined realistic compositions (not just isolated leaves)
//   - Documentation value for future migrators
//   - Regression coverage in Android Studio previews (no device needed)
//   - Direct feed into the gate enforcement story (hosts that exercise
//     these previews must still compile green)
// ============================================================

/**
 * Rich mock: A realistic sectioned list using VLCSectionHeader + VLCInfoItem.
 * This is the exact pattern that will appear in migrated MediaInfo screens,
 * audio browser sections, etc. Copied/adapted from the "COMBINED MOCK" section
 * of the Interop Lab (ComposeInteropLabContent).
 */
@Preview(
    name = "Sectioned List Mock (SectionHeader + InfoItem) - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 320
)
@Composable
fun SectionedListLightPreview() {
    VLCTheme(darkTheme = false) {
        Column(Modifier.padding(8.dp)) {
            VLCSectionHeader(text = "Video Tracks")
            VLCInfoItem(title = "H.264", subtitle = "1920×1080 @ 23.97fps • 8.2 Mbps")
            VLCInfoItem(title = "MPEG-4", subtitle = "1280×720 • 4.1 Mbps")
            VLCSectionHeader(text = "Audio Tracks")
            VLCInfoItem(
                title = "English (AAC)",
                subtitle = "48 kHz • Stereo • 256 kb/s",
                leadingContent = { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
            VLCSectionHeader(text = "Subtitles")
            VLCInfoItem(title = "English SRT", subtitle = "Forced: no • Default: yes")
        }
    }
}

@Preview(
    name = "Sectioned List Mock (SectionHeader + InfoItem) - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 320
)
@Composable
fun SectionedListDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column(Modifier.padding(8.dp)) {
            VLCSectionHeader(text = "Video Tracks")
            VLCInfoItem(title = "H.264", subtitle = "1920×1080 @ 23.97fps • 8.2 Mbps")
            VLCInfoItem(title = "MPEG-4", subtitle = "1280×720 • 4.1 Mbps")
            VLCSectionHeader(text = "Audio Tracks")
            VLCInfoItem(
                title = "English (AAC)",
                subtitle = "48 kHz • Stereo • 256 kb/s",
                leadingContent = { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
            )
        }
    }
}

/**
 * WAVE 1.4 ENHANCEMENT (compose-2l4.1.4 / bd compose-95d)
 * Richer audio-browser-realistic sectioned list mock.
 * Exercises multiple VLCSectionHeader (phone 36dp variant) + realistic
 * browser items (using listTitle/listSubtitle tokens for fidelity).
 * Directly feeds the "update PreviewUtils with realistic sectioned list mock"
 * requirement + demonstrates what BaseAudioBrowser / PlaylistFragment lists
 * will render once decorations host the Composable.
 * Also visible in Interop Lab combined mocks.
 */
@Preview(
    name = "Audio Browser Sectioned List Mock (SectionHeader + items) - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 380
)
@Composable
fun AudioBrowserSectionedListLightPreview() {
    VLCTheme(darkTheme = false) {
        Column(Modifier.padding(4.dp).background(VLCThemeDefaults.colors.backgroundDefault)) {
            VLCSectionHeader(text = "Recently Played")
            // Simulated browser rows (title + subtitle using real tokens)
            Text("Some Awesome Album", color = VLCThemeDefaults.colors.listTitle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start=16.dp, top=4.dp))
            Text("Artist Name • 12 tracks", color = VLCThemeDefaults.colors.listSubtitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start=16.dp))
            Spacer(Modifier.height(8.dp))
            VLCSectionHeader(text = "All Artists")
            Text("A Great Artist", color = VLCThemeDefaults.colors.listTitle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start=16.dp, top=4.dp))
            Text("42 albums • 512 tracks", color = VLCThemeDefaults.colors.listSubtitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start=16.dp))
            Spacer(Modifier.height(4.dp))
            Text("Another Artist With A Very Long Name That Should Ellipsize", color = VLCThemeDefaults.colors.listTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start=16.dp, top=4.dp))
        }
    }
}

@Preview(
    name = "Audio Browser Sectioned List Mock (SectionHeader + items) - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 380
)
@Composable
fun AudioBrowserSectionedListDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column(Modifier.padding(4.dp).background(VLCThemeDefaults.colors.backgroundDefault)) {
            VLCSectionHeader(text = "Recently Played")
            Text("Some Awesome Album", color = VLCThemeDefaults.colors.listTitle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start=16.dp, top=4.dp))
            Text("Artist Name • 12 tracks", color = VLCThemeDefaults.colors.listSubtitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start=16.dp))
            Spacer(Modifier.height(8.dp))
            VLCSectionHeader(text = "All Artists")
            Text("A Great Artist", color = VLCThemeDefaults.colors.listTitle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start=16.dp, top=4.dp))
            Text("42 albums • 512 tracks", color = VLCThemeDefaults.colors.listSubtitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start=16.dp))
        }
    }
}

/**
 * Rich mock: Dialog content using VLCDialogConfirmDelete inside a simulated
 * dialog surface. Mirrors the interactive "Show Confirm Delete Dialog" demo
 * in the Interop Lab (which wraps the leaf inside a real Material3 AlertDialog).
 */
@Preview(
    name = "Dialog Confirm Delete Mock - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 220
)
@Composable
fun DialogConfirmDeleteMockLightPreview() {
    VLCTheme(darkTheme = false) {
        // Simulated dialog card (the real usage wraps in AlertDialog)
        androidx.compose.material3.Surface(
            modifier = Modifier.padding(16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            tonalElevation = 6.dp
        ) {
            VLCDialogConfirmDelete(
                title = "Delete this media?",
                message = "This will permanently remove the selected file from your device. This action cannot be undone.",
                iconContent = { Text("⚠", style = MaterialTheme.typography.headlineMedium) }
            )
        }
    }
}

@Preview(
    name = "Dialog Confirm Delete Mock - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 220
)
@Composable
fun DialogConfirmDeleteMockDarkPreview() {
    VLCTheme(darkTheme = true) {
        androidx.compose.material3.Surface(
            modifier = Modifier.padding(16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            tonalElevation = 6.dp
        ) {
            VLCDialogConfirmDelete(
                title = "Delete forever",
                message = "The file will be removed from storage and metadata cleared.",
                iconContent = { Text("🗑", style = MaterialTheme.typography.headlineMedium) }
            )
        }
    }
}

// =========================================================================
// WAVE 1 / compose-2l4.1.5: REAL HOST CONTEXT PREVIEWS FOR VLCDialogConfirmDelete
// (bd compose-j0e - ConfirmDeleteComposeDialog host migration)
// These mirror the *exact* title generation cases + icon decisions from the
// production ConfirmDeleteComposeDialog.kt when-expression and ban-folder branch
// (single file, folders, files+folders, album, playlist, several, ban warning,
// clear history, etc.). They make the Compose host visually regression-testable
// in Studio with zero device. The iconContent here uses simple symbols; the real
// host mapping uses AndroidView + AnimatedVectorDrawableCompat for the looping
// anim_delete vs ic_warning_medium and is exercised live in the Compose Interop
// Lab (ComposeInteropLabActivity.kt).
// See also the DialogConfirmDeleteMock* above (the generic ones from Lab v1).
// =========================================================================

@Preview(
    name = "VLCDialogConfirmDelete - Host Single File (Light)",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun VLCDialogConfirmDeleteHostSingleFileLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCDialogConfirmDelete(
            title = "Delete \"My Video.mp4\"?",
            message = "This action cannot be undone.",
            iconContent = { Text("🗑", style = MaterialTheme.typography.headlineMedium) }
        )
    }
}

@Preview(
    name = "VLCDialogConfirmDelete - Host Ban Folder Warning (Dark)",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun VLCDialogConfirmDeleteHostBanDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCDialogConfirmDelete(
            title = "Ban this folder?",
            message = "This will hide the folder and its contents from the media library.",
            iconContent = { Text("⚠", style = MaterialTheme.typography.headlineMedium) }
        )
    }
}

@Preview(
    name = "VLCDialogConfirmDelete - Host Multi Folders+Files (Light)",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 180
)
@Composable
fun VLCDialogConfirmDeleteHostMultiLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCDialogConfirmDelete(
            title = "Confirm delete folders and files (2 folders, 7 files)?",
            message = "All selected items will be permanently removed from your device.",
            iconContent = { Text("🗑", style = MaterialTheme.typography.headlineMedium) }
        )
    }
}

@Preview(
    name = "VLCDialogConfirmDelete - Host Album + Clear History (Dark)",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 200
)
@Composable
fun VLCDialogConfirmDeleteHostAlbumHistoryDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column {
            VLCDialogConfirmDelete(
                title = "Delete album \"Greatest Hits\"?",
                message = "This will remove the album and its tracks from the library.",
                iconContent = { Text("🗑", style = MaterialTheme.typography.headlineMedium) }
            )
            Spacer(Modifier.height(8.dp))
            VLCDialogConfirmDelete(
                title = "Clear playback history",
                message = "This will clear all playback history. This action cannot be undone.",
                iconContent = { Text("⚠", style = MaterialTheme.typography.headlineMedium) }
            )
        }
    }
}

/**
 * Interop Lab snapshot preview: A condensed vertical slice that approximates
 * what the top of the live ComposeInteropLabActivity looks like.
 * Useful for quick visual regression of the "crown jewel" host itself.
 * (The full interactive version with state + dialog launcher lives on-device
 * via the Lab launched from DebugLogActivity.)
 */
@Preview(
    name = "Interop Lab Snapshot (multiple leaves) - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 380
)
@Composable
fun InteropLabSnapshotLightPreview() {
    VLCTheme(darkTheme = false) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Compose Interop Lab (snapshot)", style = MaterialTheme.typography.titleMedium)
            VLCDropdownItem(text = "SFTP (from Lab)")
            VLCSectionHeader(text = "Section in Lab")
            VLCInfoItem(title = "Audio", subtitle = "Bitrate + codec details (Lab mock)")
            VLCDebugLogLine(text = "12:34:56 [lab] ComposeInteropLabContent running")
            VLCDialogConfirmDelete(
                title = "Confirm (Lab)",
                message = "Dialog leaf inside the interop host demo.",
                iconContent = { Text("⚠") }
            )
        }
    }
}

@Preview(
    name = "Interop Lab Snapshot (multiple leaves) - Dark",
    showBackground = true,
    backgroundColor = 0xFF131313,
    widthDp = 360,
    heightDp = 380
)
@Composable
fun InteropLabSnapshotDarkPreview() {
    VLCTheme(darkTheme = true) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Compose Interop Lab (snapshot)", style = MaterialTheme.typography.titleMedium)
            VLCDropdownItem(text = "SFTP (from Lab)")
            VLCSectionHeader(text = "Section in Lab")
            VLCInfoItem(title = "Audio", subtitle = "Bitrate + codec details (Lab mock)")
            VLCDebugLogLine(text = "12:34:56 [lab] ComposeInteropLabContent running")
        }
    }
}
