/*****************************************************************************
 * ComposeInteropLabActivity.kt
 *
 * Copyright © 2013-2026 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan.vlc.gui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.videolan.vlc.R

// =============================================================================
// WAVE 1 CROSS-CUTTING INTEROP LAB IMPORTS (compose-2l4.1.8)
// These come from :application:compose (api dependency).
// This Activity is the dedicated dev-only "crown jewel" showcase.
// =============================================================================
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCAbRepeatAddMarkerButton
import org.videolan.vlc.compose.components.VLCAbRepeatChipIcon
import org.videolan.vlc.compose.components.VLCDebugLogLine
import org.videolan.vlc.compose.components.VLCDialogConfirmDelete
import org.videolan.vlc.compose.components.VLCDropdownItem
import org.videolan.vlc.compose.components.VLCInfoItem
import org.videolan.vlc.compose.components.VLCAudioAbRepeatMarker
import org.videolan.vlc.compose.components.VLCAudioHeaderActionButton
import org.videolan.vlc.compose.components.VLCAudioHeaderBackground
import org.videolan.vlc.compose.components.VLCAudioHeaderDivider
import org.videolan.vlc.compose.components.VLCAudioHeaderPlayPauseButton
import org.videolan.vlc.compose.components.VLCAudioHeaderTimeLabel
import org.videolan.vlc.compose.components.VLCAudioHeaderTransportButton
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPill
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPillState
import org.videolan.vlc.compose.components.VLCAudioPlayerChips
import org.videolan.vlc.compose.components.VLCAudioPlayerChipsState
import org.videolan.vlc.compose.components.VLCAudioSeekDelayLabel
import org.videolan.vlc.compose.components.VLCAudioSeekHudButton
import org.videolan.vlc.compose.components.VLCAudioTrackInfoText
import org.videolan.vlc.compose.components.VLCAudioTrackInfoTextStyle
import org.videolan.vlc.compose.components.VLCAudioTimelineTimeLabel
import org.videolan.vlc.compose.components.VLCOnboardingWelcome
import org.videolan.vlc.compose.components.VLCSectionHeader
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
// =============================================================================

// =========================================================================
// ==================== WAVE 1: COMPOSE INTEROP LAB (compose-2l4.1.8) ====================
// Host file: ComposeInteropLabActivity.kt   (cross-cutting task: compose-2l4.1.8 / bd: compose-iju)
// Layout: compose_interop_lab.xml
//
// MISSION: This is the dedicated, dev-only "Compose Interop Lab" - the single most
// important visibility and testability artifact for the entire Wave 1 (and future waves).
// It makes the hybrid interop pattern (VLCComposeView + VLCTheme + leaf Composables)
// immediately tangible, interactive, and copy-paste referenceable for the whole team.
//
// Launched exclusively from DebugLogActivity (additive button) or future dev-only menus.
// Never reachable from production navigation. Safe for permanent inclusion in debug trees.
//
// WHAT IT HOSTS (live + interactive, all in one scrollable Column):
//   Minimum required by acceptance criteria (the 6 Wave 0/1 leaves):
//     1. VLCDropdownItem     - simple presentational, from dropdown_item.xml
//     2. VLCSectionHeader    - list section headers, from recycler_section_header*.xml
//     3. VLCInfoItem         - media track details, from info_item.xml (high reuse)
//     4. VLCDebugLogLine     - log lines, from debug_log_item.xml (also used in DebugLog host)
//     5. VLCDialogConfirmDelete - presentational core used by ConfirmDeleteComposeDialog
//     6. VLCOnboardingWelcome - static branding leaf for the former phone welcome step
//
//   Interactive / richer demos added for educational value + gate visibility:
//     - Sectioned list mock (VLCSectionHeader + multiple VLCInfoItem rows) simulating
//       a real media info or browser list migration target.
//     - Stateful "live log simulator" using VLCDebugLogLine + button that appends lines.
//     - Dialog mock: Button opens a real Material3 AlertDialog whose content is the
//       VLCDialogConfirmDelete leaf + action buttons (proves nesting + interop works).
//     - Onboarding card variant (different subtitle, REAL logo via painterResource in slot).
//     - Explicit light/dark notes + "System theme follows device setting".
//
// TWO PATTERNS DEMONSTRATED (at full-screen lab scale):
//   PATTERN 1 (PRIMARY HERE): VLCComposeView declared in XML layout (this file),
//     wired via findViewById in onCreate exactly as NetworkServerDialog + DebugLogActivity.
//     setContent { VLCTheme { ComposeInteropLabContent(...) } }
//
//   (Pattern 2 - small ComposeView rows in adapters - is shown in DebugLogActivity's
//    DebugLogComposeAdapter. The Lab focuses on the "big picture" host + composition.)
//
// WHY THIS ADVANCES THE WHOLE WAVE (acceptance criteria):
//   - Interop demo: one place that exercises ALL current leaves simultaneously + in
//     realistic combinations (the "rich usage mocks" that also feed PreviewUtils).
//   - Preview + gate enforcement: Every leaf used here has @Preview coverage in
//     PreviewUtils.kt. This host itself participates in the mandatory compile gate
//     (see "BUILD GATE POLICY" below + evidence appended to bd compose-iju).
//   - Educational quality: matches (and extends) the comment density + traceability of
//     the reference hosts NetworkServerDialog.kt (compose-5qk) and DebugLogActivity.kt
//     (compose-2l4.1.2). Future agents copy this header verbatim.
//
// BUILD GATE POLICY (enforced + documented for compose-2l4.1.8 and all future hosts):
//   "Every host must have green compile gate evidence."
//   - Required command (from worktree root):
//       ./gradlew :application:vlc-android:compileDebugKotlin --console=plain -q
//       (or the full :application:compose:build + connected checks for deeper validation)
//   - Evidence MUST be captured (terminal output showing SUCCESS) and referenced in:
//       * This file's header comments
//       * The expanded compose/README.md section on Wave 1 + gates
//       * bd notes on compose-iju (compose-2l4.1.8)
//   - Why: Prevents silent rot of interop hosts as the leaf module evolves.
//     The Lab + DebugLog + NetworkServerDialog are the "canary" hosts.
//   - This pattern is now mandatory for any new host added in Wave 1+.
//
// ROLLBACK (one-file + one-layout + manifest delta):
//   Delete: ComposeInteropLabActivity.kt, compose_interop_lab.xml
//   Remove: the <activity> declaration below + the launch wiring from DebugLogActivity.kt
//           + the single button added to debug_log.xml
//   Result: zero behavior change. The rest of the app (including all prior interop demos)
//   continues to compile and run exactly as the 2026-05 pre-lab baseline.
//
// THEMING GUARANTEE:
//   - Explicit VLCTheme wrapper at the setContent site (this Activity).
//   - Every leaf also wraps VLCTheme internally (defensive + for standalone @Previews).
//   - Follows isSystemInDarkTheme() by default → automatic light/dark matching the
//     real app's Theme.VLC.Appearance / Black variants.
//   - All semantic tokens (headerBackground, audioBrowserSeparator, onboardingBackground,
//     fontAudioLight, listSubtitle, etc.) exercised here live.
//
// Permanent Exceptions boundary (repeated for every Wave 1 agent):
//   Everything in this Lab is migratable. The 20% that stays forever in XML/native:
//   - Native player surfaces (VLCVideoView, subtitles, hardware decoding paths)
//   - Heavy MediaLibrary JNI / medialibrary service surfaces
//   - Certain complex TV overlays and leanback fragments
//   - Legacy WebView-based UIs and a few preference screens with custom prefs
//   See full matrix in bd compose-iju notes and the parent Wave 1 epic artifacts.
//
// Traceability (update when new leaves or hosts land):
//   - All 6 leaves: application/compose/src/main/java/org/videolan/vlc/compose/components/*.kt
//   - Interop + Abstract widget: .../interop/VLCCompose.kt
//   - Theme + tokens: .../theme/VLCTheme.kt (see VLCColorScheme for ?attr/ origins)
//   - Previews (now richer): .../compose/PreviewUtils.kt (sectioned list, dialog mock, etc.)
//   - Launch host: DebugLogActivity.kt (compose-2l4.1.2) + debug_log.xml
//   - Historical demo host: NetworkServerDialog.kt (compose-5qk; now full Compose)
//   - List host example: DebugLogActivity.kt + debug_log.xml (compose-2l4.1.2 / bd compose-5wg)
//   - Decoration + browser host: Recycler*ItemDecoration + active media lists (compose-2l4.1.4 / bd compose-95d)
//   - Info surfaces host: MediaInfoAdapter.kt + InfoActivity.kt (compose-2l4.1.3 / bd compose-l94)
//   - Onboarding first-run flow: OnboardingActivity.kt (full Compose phone flow)
//   - This cross-cutting task: compose-2l4.1.8 (bd: compose-iju)
//   - Earlier bootstrap: compose-cb5, compose-5wg (closed)
//   - Epic context: Wave 1 leaf migrations after phase-0-compose-bootstrap
//
// At end of session (MANDATORY):
//   - bd update compose-iju --notes "..." (detailed evidence + pointers)
//   - bd close compose-iju --reason "Interop Lab delivered; gate policy documented + enforced; acceptance substantially advanced"
//   - git pull --rebase
//   - bd dolt push
//   - git push
//   - git status MUST report "up to date with origin/..."
//   - Clean stashes etc.
// =========================================================================

/**
 * Dev-only Activity that hosts the full Compose Interop Lab.
 *
 * Thin wrapper: inflation + single VLCComposeView wiring. All the interesting
 * content (the scrollable educational + interactive examples) lives in Compose.
 *
 * This pattern (new Activity whose entire UI is a Compose host via interop) is
 * now the recommended template for any future "full screen" dev tooling or
 * for production destinations once Compose Navigation lands.
 */
class ComposeInteropLabActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_interop_lab)

        // ---------------------------------------------------------------------
        // ACTUAL WIRING - Pattern 1 at full Lab scale (the entire screen)
        // ---------------------------------------------------------------------
        // This is the canonical "new dedicated host" example for compose-2l4.1.8.
        // One VLCComposeView fills the content area. The Composable below builds
        // a realistic scrollable column exercising every current Wave 1 leaf live.
        //
        // Note the explicit VLCTheme { } wrapper at the host boundary (required
        // for correct token resolution even though leaves also wrap defensively).
        // ---------------------------------------------------------------------
        val composeLabHost = findViewById<VLCComposeView>(R.id.compose_interop_lab_host)
        composeLabHost?.setContent {
            VLCTheme {
                ComposeInteropLabContent()
            }
        }
    }

    companion object {
        const val TAG = "VLC/ComposeInteropLabActivity"
    }
}

// =============================================================================
// THE LAB CONTENT COMPOSABLE (the crown jewel implementation)
// =============================================================================

/**
 * The complete interactive lab UI.
 *
 * Implemented as a single @Composable so it can also be previewed / extracted
 * into PreviewUtils.kt for the "richer usage mock" requirement.
 *
 * Structure:
 *   - Intro blurb + gate reminder
 *   - Individual leaf cards (with live controls where useful)
 *   - Combined realistic mocks (sectioned list, dialog launcher, onboarding)
 *   - Live log simulator (exercises DebugLogLine + mutable state)
 *
 * All examples are wrapped in Cards or simple containers using VLCTheme tokens
 * for visual separation (educational).
 */
@Composable
fun ComposeInteropLabContent() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // -----------------------------------------------------------------
        // INTRO / PURPOSE (educational, always visible at top of lab)
        // -----------------------------------------------------------------
        Text(
            text = "Compose Interop Lab — Wave 1 Foundation",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "This screen exercises every Wave 0/1 leaf Composable live inside a real legacy Activity host via VLCComposeView + VLCTheme. " +
                    "It is the single source of truth for hybrid pattern correctness and the mandatory compile gate canary.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Build gate: ./gradlew :application:vlc-android:compileDebugKotlin must succeed (evidence in bd compose-iju + README). " +
                    "Dev-only: launched only from DebugLogActivity.",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(8.dp))

        // -----------------------------------------------------------------
        // 1. VLCDropdownItem (live examples + variants)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "1. VLCDropdownItem (dropdown_item.xml)")
        VLCDropdownItem(text = "FTP (live interop example)")
        VLCDropdownItem(text = "SFTP — long protocol name that should not overflow")
        Text(
            "Used in: NetworkServerDialog (first real interop demo). " +
            "Simple presentational leaf — no state, just themed Text in a min-size Box.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // 2. VLCSectionHeader (phone + tv variant simulation)
        // -----------------------------------------------------------------
        // WAVE 1 update (compose-2l4.1.4 / bd compose-95d): the Decoration hosts
        // (Recycler*ItemDecoration) are now the primary interop targets for this
        // leaf in real media lists. This Lab already exercises it live. The new
        // AudioBrowserSectionedList*Previews in PreviewUtils.kt were added as part
        // of the same task to keep the "rich mock" requirement fresh.
        VLCSectionHeader(text = "2. VLCSectionHeader (recycler_section_header*.xml)")
        VLCSectionHeader(text = "Recently Played")
        VLCSectionHeader(text = "Audio Books & Extremely Long Section Titles That Must Ellipsize Gracefully")
        VLCSectionHeader(text = "TV Variant Simulation", isTv = true)
        Text(
            "Token: headerBackground + audioBrowserSeparator. High reuse across audio browser, playlists, media lists. Decoration interop path documented in RecyclerSectionItemDecoration.kt.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // WAVE 2. VLCAudioPlayerChips (audio_player.xml quick actions)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "Wave 2. VLCAudioPlayerChips (audio_player.xml)")
        VLCAudioHeaderBackground(modifier = Modifier.fillMaxWidth().height(40.dp))
        VLCAudioHeaderDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
        VLCAudioPlayerChips(
            state = VLCAudioPlayerChipsState(
                speedText = "1.25x",
                sleepText = "12:55 AM",
                speedUsesGlobalRate = true
            ),
            speedIconContent = {
                Text("S", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.audioChipsTextColor)
            },
            sleepIconContent = {
                Text("Z", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.audioChipsTextColor)
            }
        )
        VLCAudioHeaderTimeLabel(text = "10:42")
        Column {
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
        Row(horizontalArrangement = Arrangement.spacedBy(72.dp)) {
            VLCAudioTimelineTimeLabel(text = "3:42")
            VLCAudioTimelineTimeLabel(text = "-12:08")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VLCAudioHeaderActionButton(contentDescription = "Search") {
                Text("S", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.audioMenuIcon)
            }
            VLCAudioHeaderActionButton(contentDescription = "Show playlist") {
                Text("P", color = MaterialTheme.colorScheme.primary)
            }
            VLCAudioHeaderActionButton(contentDescription = "Advanced") {
                Text("⋮", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.audioMenuIcon)
            }
            VLCAudioHeaderActionButton(contentDescription = "Reset A-B marker") {
                Text("A", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.audioMenuIcon)
            }
            VLCAudioHeaderActionButton(contentDescription = "Stop A-B repeat") {
                Text("B", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.audioMenuIcon)
            }
            VLCAudioHeaderPlayPauseButton(contentDescription = "Pause") {
                Text("Ⅱ", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            VLCAbRepeatChipIcon {
                Text("AB", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAbRepeatAddMarkerButton(text = "Set start point", onClick = {})
            VLCAudioAbRepeatMarker {
                Text("A", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioAbRepeatMarker {
                Text("B", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VLCAudioHeaderTransportButton(contentDescription = "Shuffle") {
                Text("S", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Previous") {
                Text("<", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Pause", size = 56.dp) {
                Text("P", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Next") {
                Text(">", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Repeat") {
                Text("R", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VLCAudioHeaderTransportButton(contentDescription = "Previous chapter", size = 40.dp) {
                Text("<", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Next chapter", size = 40.dp) {
                Text(">", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VLCAudioHeaderTransportButton(contentDescription = "Move audio controls to the left screen", size = 40.dp) {
                Text("L", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioHeaderTransportButton(contentDescription = "Move audio controls to the right screen", size = 40.dp) {
                Text("R", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VLCAudioSeekHudButton(contentDescription = "Previous bookmark") {
                Text("B", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioSeekHudButton(contentDescription = "Rewind 10 seconds") {
                Text("<", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioSeekDelayLabel(text = "10")
            VLCAudioSeekHudButton(contentDescription = "Forward 10 seconds") {
                Text(">", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioSeekHudButton(contentDescription = "Next bookmark") {
                Text("B", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        }
        Text(
            "Hosted in the real AudioPlayer fragment by replacing the playback_chips ChipGroup with VLCComposeView. " +
            "The collapsed header background and divider are Compose-hosted under the existing header_background and header_divider IDs so slide alpha transitions stay intact. " +
            "The collapsed header time label is also Compose-hosted under the existing header_time ID. " +
            "The full-player elapsed/length timeline labels are Compose-hosted under time and length. " +
            "The landscape title/subtitle/track-detail text stack is Compose-hosted under song_title/song_subtitle/song_track_info. " +
            "The search, playlist switch, and overflow actions are Compose-hosted under their existing IDs. " +
            "The AB-repeat reset/stop header actions are also Compose-hosted under ab_repeat_reset and ab_repeat_stop while the service helper still controls their visibility. " +
            "The A-B repeat timeline markers are Compose-hosted under ab_repeat_marker_a and ab_repeat_marker_b while the guidelines still control their positions. " +
            "The shared A-B repeat add-marker chip icon and add-marker button are Compose-rendered through AbRepeatChipIconView and AbRepeatAddMarkerButtonView inside ab_repeat_controls.xml for both audio and video HUD hosts. " +
            "The mini play/pause button is Compose-hosted under header_play_pause with the long-press stop action preserved. " +
            "The tablet header transport strip is Compose-hosted under the existing header_shuffle/header_previous/header_large_play_pause/header_next/header_repeat IDs. " +
            "The full-player bottom shuffle/previous/play_pause/next/repeat transport controls now use the same Compose leaf while preserving previous/next long seek. " +
            "The landscape chapter chevrons are Compose-hosted under previous_chapter and next_chapter. " +
            "The foldable hinge left/right affordances are Compose-hosted under hinge_go_left and hinge_go_right. " +
            "The cover-mode seek/bookmark HUD row is Compose-hosted under the existing audio_rewind/audio_forward IDs with delay labels driven from Settings.audioJumpDelay. " +
            "The player gesture/switcher surface stays outside this slice; the speed and sleep quick actions are now Compose.",
            style = MaterialTheme.typography.bodySmall
        )
        VLCAudioQueueProgressPill(
            state = VLCAudioQueueProgressPillState(
                text = "Track 3 / 12  •  10:42 / 48:12",
                contentDescription = "Track 3 of 12. 10 minutes 42 seconds out of 48 minutes 12 seconds."
            )
        )
        Text(
            "The queue progress pill is also hosted in AudioPlayer via VLCComposeView under the existing audio_play_progress ID, " +
            "so snackbar anchoring and ConstraintLayout transitions keep working while the HUD chrome is Compose-rendered.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // 3. VLCInfoItem (multiple realistic rows)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "3. VLCInfoItem (info_item.xml — track details)")
        VLCInfoItem(
            title = "Audio",
            subtitle = "Bitrate: 320 kb/s • Codec: mp3 • Channels: 2 • Language: eng",
            leadingContent = { Text("♪", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.fontAudioLight) }
        )
        VLCInfoItem(
            title = "Video",
            subtitle = "1920×1080 • 23.97 fps • Codec: h264 • Profile: High@5.1",
            leadingContent = { Text("📺", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.fontAudioLight) }
        )
        VLCInfoItem(
            title = "Text",
            subtitle = "Language: eng • Codec: subrip • Forced: no"
        )
        Text(
            "Slot-based leadingContent for icons. Colors: fontAudioLight + listSubtitle. Primary migration target for MediaInfoAdapter (compose-2l4.1.3 / bd compose-l94). See MediaInfoAdapter.kt for the full RecyclerView + ComposeView Pattern 2 implementation (old info_item.xml path 100% preserved in comments) + InfoActivity.kt host comments.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // 4. VLCDebugLogLine (live simulator - interactive!)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "4. VLCDebugLogLine (debug_log_item.xml) — LIVE SIMULATOR")
        var logLines by remember { mutableStateOf(listOf(
            "VLC media player 3.6.0 Vetinari",
            "[8f3a2b] main libvlc: Running vlc with the default interface",
            "I/DEBUG: signal 11 (SIGSEGV) example"
        )) }

        Column {
            logLines.forEach { line ->
                VLCDebugLogLine(text = line)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                logLines = logLines + "D/VLC: [compose-lab] Appended live log line #${logLines.size + 1} at ${System.currentTimeMillis() % 100000}"
            }) {
                Text("Append log line")
            }
            OutlinedButton(onClick = { logLines = logLines.dropLast(1) }) {
                Text("Remove last")
            }
            OutlinedButton(onClick = { logLines = listOf("VLC media player 3.6.0 Vetinari") }) {
                Text("Reset")
            }
        }
        Text(
            "Stateful demo inside the interop host. Matches exactly what DebugLogActivity renders for real logs (both the header demo and the custom adapter rows).",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // 5. VLCDialogConfirmDelete (interactive: real dialog launcher + host context)
        // WAVE 1 / compose-2l4.1.5 (bd compose-j0e) enhancement:
        // This now exercises the leaf with *real host context* pulled directly from
        // the title-generation + icon logic in ConfirmDeleteComposeDialog.kt
        // (the production Compose bottom sheet for deletes, bans, history clears,
        // TV app data clear, etc.).
        //
        // Variants below mirror the title when-expression in ConfirmDeleteComposeDialog
        // (single file/folder, multi files+folders, album, playlist, ban-folder special
        // case with warning icon, clear history, several media). The iconContent slot
        // demonstrates the mapping site for the AnimatedVectorDrawable "anim_delete"
        // (looping) vs static ic_warning_medium.
        //
        // This is the "crown jewel" live proof for the migrated Compose content:
        // the AlertDialog here is a lightweight lab analogue of the production
        // Compose bottom sheet, which now owns actions, result sending, and TV
        // focus setup directly.
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "5. VLCDialogConfirmDelete (Compose confirm delete host context)")

        var showDeleteDialog by remember { mutableStateOf(false) }
        var deleteVariant by remember { mutableStateOf("single_file") }

        // Real-host-context launcher buttons (mirrors production call sites)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { deleteVariant = "single_file"; showDeleteDialog = true }) {
                    Text("Single file")
                }
                OutlinedButton(onClick = { deleteVariant = "multi"; showDeleteDialog = true }) {
                    Text("Multi (files+folders)")
                }
                OutlinedButton(onClick = { deleteVariant = "ban"; showDeleteDialog = true }) {
                    Text("Ban folder (warning)")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { deleteVariant = "album"; showDeleteDialog = true }) {
                    Text("Album")
                }
                OutlinedButton(onClick = { deleteVariant = "playlist"; showDeleteDialog = true }) {
                    Text("Playlist")
                }
                OutlinedButton(onClick = { deleteVariant = "clear_history"; showDeleteDialog = true }) {
                    Text("Clear history")
                }
            }
        }

        if (showDeleteDialog) {
            val (title, message, iconLabel) = when (deleteVariant) {
                "single_file" -> Triple(
                    "Delete \"My Video.mp4\"?",
                    "This action cannot be undone.",
                    "🗑"
                )
                "multi" -> Triple(
                    "Confirm delete folders and files (3 folders, 12 files)?",
                    "All selected items will be permanently removed from your device.",
                    "🗑"
                )
                "ban" -> Triple(
                    "Ban this folder?",
                    "This will hide the folder and its contents from the media library. Use the ban folder explanation from real host.",
                    "⚠"
                )
                "album" -> Triple(
                    "Delete album \"Greatest Hits\"?",
                    "This will remove the album and its tracks from the library.",
                    "🗑"
                )
                "playlist" -> Triple(
                    "Delete playlist \"Workout Mix\"?",
                    "The playlist will be deleted (media files remain).",
                    "🗑"
                )
                "clear_history" -> Triple(
                    "Clear playback history", // mirrors real usage in PreferencesAdvanced + Compose history screen + TV paths
                    "This will clear all playback history. This action cannot be undone.",
                    "⚠"
                )
                else -> Triple("Delete this media?", "This will permanently remove the selected file.", "🗑")
            }

            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { /* title provided by the leaf for fidelity */ },
                text = {
                    VLCDialogConfirmDelete(
                        title = title,
                        message = message,
                        // In the real ConfirmDeleteComposeDialog host the iconContent uses
                        // AndroidView + the exact AnimatedVectorDrawableCompat setup
                        // (anim_delete looping or ic_warning_medium). Here we approximate
                        // for the Lab demo.
                        iconContent = {
                            Text(iconLabel, style = MaterialTheme.typography.headlineMedium)
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(if (deleteVariant == "ban") "BAN" else "DELETE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        Text(
            "Leaf = icon + title + message. " +
            "Scaffolding + actions by Material3 AlertDialog here, matching the production ConfirmDeleteComposeDialog bottom sheet. " +
            "Variants above are live simulations of the title logic from ConfirmDeleteComposeDialog.kt (task compose-2l4.1.5 / bd compose-j0e).",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // 6. VLCOnboardingWelcome (two variants)
        // -----------------------------------------------------------------
        // This section keeps exercising the reusable welcome branding leaf. The
        // production phone onboarding flow is now rendered directly by full Compose
        // content in OnboardingActivity.
        VLCSectionHeader(text = "6. VLCOnboardingWelcome")
        VLCOnboardingWelcome(
            title = "Welcome to VLC for Android",
            subtitle = "The best open source video and audio player, now on your mobile device. Free. No ads. No tracking."
        )

        Spacer(Modifier.height(12.dp))

        // Variant with REAL logo (painterResource) exercising the slot used by the
        // full Compose phone onboarding flow.
        VLCOnboardingWelcome(
            title = "Onboarding (real VLC icon via logoContent slot)",
            subtitle = "This mirrors the visual branding used by the first-run Compose onboarding flow.",
            logoContent = {
                Image(
                    painter = painterResource(R.drawable.ic_icon),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
            }
        )
        Text(
            "Uses onboardingBackground token (see VLCTheme.kt + DarkVLCColors). The full onboarding flow (theme choice, permissions, scan) remains future work. " +
            "Exercise in both Lab + the new host + enhanced dark previews in PreviewUtils (compose-2l4.1.6).",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // COMBINED REALISTIC MOCK: Sectioned list (directly feeds PreviewUtils expansion)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "COMBINED MOCK: Sectioned Media Info List (SectionHeader + InfoItem)")
        VLCSectionHeader(text = "Video Tracks")
        VLCInfoItem(title = "H.264", subtitle = "1920×1080 @ 23.97fps • 8.2 Mbps • Profile High@5.1")
        VLCInfoItem(title = "MPEG-4", subtitle = "1280×720 @ 29.97fps • 4.1 Mbps")
        VLCSectionHeader(text = "Audio Tracks")
        VLCInfoItem(title = "English (AAC)", subtitle = "48 kHz • 2 channels • 256 kb/s")
        VLCInfoItem(title = "Español (MP3)", subtitle = "44.1 kHz • Stereo • 192 kb/s")
        VLCSectionHeader(text = "Subtitle Tracks")
        VLCInfoItem(title = "English", subtitle = "SRT • Forced: no • Default: yes")
        Text(
            "This exact composition (plus state) is extracted into PreviewUtils.kt as a reusable @Preview mock for documentation and regression.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // FOOTER / GATE REMINDER
        // -----------------------------------------------------------------
        Spacer(Modifier.height(24.dp))
        Text(
            "— End of Compose Interop Lab —\n" +
            "Every leaf above is production-ready for hybrid hosting. " +
            "Add new leaves → add example here + Preview + host compile gate run → update bd compose-iju + README.",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
