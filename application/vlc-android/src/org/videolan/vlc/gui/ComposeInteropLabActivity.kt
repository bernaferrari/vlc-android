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
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import androidx.compose.foundation.layout.width
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
import org.videolan.vlc.compose.components.VLCAbRepeatControls
import org.videolan.vlc.compose.components.VLCDebugLogLine
import org.videolan.vlc.compose.components.VLCDialogConfirmDelete
import org.videolan.vlc.compose.components.VLCDropdownItem
import org.videolan.vlc.compose.components.VLCInfoItem
import org.videolan.vlc.compose.components.VLCAudioAbRepeatMarkers
import org.videolan.vlc.compose.components.VLCBookmarkRow
import org.videolan.vlc.compose.components.VLCBookmarkMarkers
import org.videolan.vlc.compose.components.VLCAudioHeaderActionButton
import org.videolan.vlc.compose.components.VLCAudioHeaderBackground
import org.videolan.vlc.compose.components.VLCAudioHeaderDivider
import org.videolan.vlc.compose.components.VLCAudioHeaderPlayPauseButton
import org.videolan.vlc.compose.components.VLCAudioHeaderTimeLabel
import org.videolan.vlc.compose.components.VLCAudioHeaderTransportButton
import org.videolan.vlc.compose.components.VLCAudioMiniProgressBar
import org.videolan.vlc.compose.components.VLCAudioPlayerBackground
import org.videolan.vlc.compose.components.VLCAudioPlayerGradient
import org.videolan.vlc.compose.components.VLCAudioPlayerGradientEdge
import org.videolan.vlc.compose.components.VLCAudioPlaylistItem
import org.videolan.vlc.compose.components.VLCAudioPlaylistSearchField
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPill
import org.videolan.vlc.compose.components.VLCAudioQueueProgressPillState
import org.videolan.vlc.compose.components.VLCAudioResumeVideoHint
import org.videolan.vlc.compose.components.VLCAudioPlayerChips
import org.videolan.vlc.compose.components.VLCAudioPlayerChipsState
import org.videolan.vlc.compose.components.VLCAudioSeekDelayLabel
import org.videolan.vlc.compose.components.VLCAudioSeekHudButton
import org.videolan.vlc.compose.components.VLCAudioTrackInfoText
import org.videolan.vlc.compose.components.VLCAudioTrackInfoTextStyle
import org.videolan.vlc.compose.components.VLCAudioTimelineSlider
import org.videolan.vlc.compose.components.VLCAudioTimelineTimeLabel
import org.videolan.vlc.compose.components.VLCBrowserItemCard
import org.videolan.vlc.compose.components.VLCBrowserItemRow
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.components.VLCOnboardingWelcome
import org.videolan.vlc.compose.components.VLCPlayerOptionItem
import org.videolan.vlc.compose.components.VLCSectionHeader
import org.videolan.vlc.compose.components.VLCVideoQuickAction
import org.videolan.vlc.compose.components.VLCVideoQuickActions
import org.videolan.vlc.compose.theme.VLCTheme
// =============================================================================

/**
 * Dev-only Compose lab for exercising migration leaves in one interactive screen.
 */
class ComposeInteropLabActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCTheme {
                        ComposeInteropLabContent()
                    }
                }
            }
        )
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
            text = "This screen exercises every Wave 0/1 leaf Composable live inside a direct Compose Activity host. " +
                    "It is the single source of truth for leaf behavior and the mandatory compile gate canary.",
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
        // WAVE 1 update (compose-2l4.1.4 / bd compose-95d): section headers now
        // live as Compose list content in migrated browser/list surfaces.
        VLCSectionHeader(text = "2. VLCSectionHeader (Compose list host)")
        VLCSectionHeader(text = "Recently Played")
        VLCSectionHeader(text = "Audio Books & Extremely Long Section Titles That Must Ellipsize Gracefully")
        VLCSectionHeader(text = "TV Variant Simulation", isTv = true)
        Text(
            "Token: headerBackground + audioBrowserSeparator. High reuse across audio browser, playlists, and media lists.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // WAVE 2. VLCBrowserItemRow/Card (browser_item + card variants)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "Wave 2. VLCBrowserItemRow/Card (browser_item.xml variants)")
        VLCBrowserItemRow(
            title = "Big Buck Bunny",
            subtitle = "Video - 1920x1080 - 9:56",
            artworkContent = { Text("V", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.primary) },
            primaryActionContent = { Text("P", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.primary) },
            moreActionContent = { Text("M", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.listSubtitle) }
        )
        VLCBrowserItemRow(
            title = "Selected playlist from the shared browser row leaf",
            subtitle = "42 tracks",
            selected = true,
            artworkContent = { Text("L", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.primary) },
            primaryActionContent = { Text("P", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.primary) },
            moreActionContent = { Text("M", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.listSubtitle) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VLCBrowserItemCard(
                title = "Camera",
                subtitle = "/storage/emulated/0/DCIM",
                artworkContent = { Text("F", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.primary) },
                moreActionContent = { Text("M", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.listSubtitle) },
                modifier = Modifier.weight(1f)
            )
            VLCBrowserItemCard(
                title = "Network share",
                subtitle = "smb://media.local",
                selected = true,
                artworkContent = { Text("N", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.primary) },
                moreActionContent = { Text("M", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.listSubtitle) },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            "This shared leaf is now hosted by playlist and video Compose screens, replacing duplicated private row/card code while preserving app-side icons through slots.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // WAVE 2. VLCEmptyState (shared loading/empty states)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "Wave 2. VLCEmptyState (empty/loading surfaces)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VLCEmptyState(
                loading = true,
                text = "Loading",
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp),
                compact = true
            )
            VLCEmptyState(
                loading = false,
                text = "No media found",
                icon = painterResource(R.drawable.ic_empty),
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp),
                compact = true
            )
        }
        Text(
            "Hosted by the phone audio, video, browser, and playlist Compose screens. The unused legacy XML wrapper has been removed.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // WAVE 2. VLCAudioPlayerChips (the former audio player XML shell quick actions)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "Wave 2. VLCAudioPlayerChips (the former audio player XML shell)")
        VLCAudioPlayerBackground(
            bitmap = null,
            overlayColor = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )
        VLCAudioHeaderBackground(modifier = Modifier.fillMaxWidth().height(40.dp))
        VLCAudioHeaderDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
        VLCAudioMiniProgressBar(
            progressFraction = 0.42f,
            modifier = Modifier.fillMaxWidth().height(4.dp)
        )
        Column(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            VLCAudioPlayerGradient(
                edge = VLCAudioPlayerGradientEdge.Top,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
            VLCAudioPlayerGradient(
                edge = VLCAudioPlayerGradientEdge.Bottom,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
        VLCAudioPlaylistSearchField(
            query = "bach",
            hint = "Search media",
            focusRequest = 0,
            onQueryChange = {},
            modifier = Modifier.fillMaxWidth().height(60.dp)
        )
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
        VLCVideoQuickActions(
            actions = listOf(
                VLCVideoQuickAction(
                    id = R.id.orientation_quick_action,
                    icon = R.drawable.ic_player_lock_landscape,
                    contentDescription = "Orientation locked"
                ),
                VLCVideoQuickAction(
                    id = R.id.playback_speed_quick_action,
                    icon = R.drawable.ic_speed_all,
                    text = "1.25x",
                    contentDescription = "Playback speed 1.25x"
                ),
                VLCVideoQuickAction(
                    id = R.id.sleep_quick_action,
                    icon = R.drawable.ic_sleep,
                    text = "12:55 AM",
                    contentDescription = "Sleep timer 12:55 AM"
                ),
                VLCVideoQuickAction(
                    id = R.id.spu_delay_quick_action,
                    icon = R.drawable.ic_subtitles,
                    text = "+300 ms",
                    contentDescription = "Subtitle delay plus 300 milliseconds"
                ),
                VLCVideoQuickAction(
                    id = R.id.audio_delay_quick_action,
                    icon = R.drawable.ic_player_volume,
                    text = "-200 ms",
                    contentDescription = "Audio delay minus 200 milliseconds"
                )
            ),
            modifier = Modifier.fillMaxWidth()
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
        VLCAudioTimelineSlider(
            progress = 222_000,
            max = 720_000,
            contentDescription = "3 minutes 42 seconds out of 12 minutes",
            onUserProgressChange = {},
            modifier = Modifier.fillMaxWidth()
        )
        VLCBookmarkMarkers(
            markerFractions = listOf(0.08f, 0.28f, 0.62f, 0.88f),
            modifier = Modifier.fillMaxWidth()
        )
        VLCBookmarkRow(
            title = "Opening scene",
            timeText = "12:38",
            timeContentDescription = "12 minutes 38 seconds",
            moreContentDescription = "More Actions"
        ) {
            Text("...", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.fontDefault)
        }
        VLCAudioPlaylistItem(
            title = "Symphony No. 1",
            subtitle = "Beethoven",
            contentDescription = "Symphony No. 1, Beethoven",
            trackNumberText = "1.",
            showTrackNumber = true,
            showReorderButtons = true,
            showDeleteButton = true,
            coverContent = {
                Text("♪", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.fontDefault)
            },
            playingContent = {
                Text("|||", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            },
            stopAfterContent = {
                Text("S", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            },
            moveUpContent = {
                Text("^", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            },
            moveDownContent = {
                Text("v", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            },
            deleteContent = {
                Text("x", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            },
            moreContent = {
                Text("...", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        )
        VLCAudioResumeVideoHint(message = "Long tap the cover to restore the video")
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
            VLCAbRepeatControls(markerText = "Set start point") {
                Text("AB", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCAudioAbRepeatMarkers(
                startFraction = 0.28f,
                stopFraction = 0.72f,
                modifier = Modifier.width(160.dp)
            ) {
                Text("AB", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
        }
        Column {
            VLCPlayerOptionItem(title = "Playback speed") {
                Text("1x", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
            }
            VLCPlayerOptionItem(title = "Repeat") {
                Text("R", color = org.videolan.vlc.compose.theme.VLCThemeDefaults.colors.playerIconColor)
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
            "Hosted in the real AudioPlayer bottom-sheet controller by replacing the playback_chips ChipGroup with VLCComposeView. " +
            "The collapsed header background and divider are Compose-hosted under the existing header_background and header_divider IDs so slide alpha transitions stay intact. " +
            "The collapsed mini progress bar is Compose-backed under progressBar while keeping the existing max/progress and slide-height contract. " +
            "The blurred cover background is direct Compose-hosted under backgroundView with AudioPlayerAnimator still owning cover loading and blur generation. " +
            "The top and bottom audio-player gradient overlays are direct Compose-hosted under top_gradient and bottom_gradient while keeping their layout constraints. " +
            "The playlist search input is direct Compose-hosted under playlist_search_text with AudioPlayer owning query and focus state. " +
            "The full-player timeline seekbar is Compose-backed under timeline with a max/progress and drag callback bridge replacing AccessibleSeekBar. " +
            "The collapsed header time label is also Compose-hosted under the existing header_time ID. " +
            "The full-player elapsed/length timeline labels are Compose-hosted under time and length. " +
            "The landscape title/subtitle/track-detail text stack is Compose-hosted under song_title/song_subtitle/song_track_info. " +
            "The search, playlist switch, and overflow actions are Compose-hosted under their existing IDs. " +
            "The AB-repeat reset/stop header actions are also Compose-hosted under ab_repeat_reset and ab_repeat_stop while the service helper still controls their visibility. " +
            "The A-B repeat timeline markers are Compose-positioned by the audio and video HUD hosts, replacing the old guideline/DataBinding marker islands. " +
            "The bookmark timeline markers are Compose-drawn under bookmark_marker_container in audio and video HUD hosts, with BookmarkListDelegate now pushing normalized media positions instead of dynamic ImageViews. " +
            "The restore-video hint is Compose-hosted under resume_video_hint instead of using a transient Material Snackbar from AudioPlayer.onResume. " +
            "The shared A-B repeat add-marker chip root is Compose-rendered through AbRepeatControlsHost in the video HUD and a plain Compose host in audio. " +
            "The shared player options panel is hosted through PlayerOptionsPanelHost on plain VLCComposeView roots, replacing the former stub wrapper plus BrowseFrameLayout shell and player_option_item.xml rows. " +
            "The mini play/pause button is Compose-hosted under header_play_pause with the long-press stop action preserved. " +
            "The tablet header transport strip is Compose-hosted under the existing header_shuffle/header_previous/header_large_play_pause/header_next/header_repeat IDs. " +
            "The full-player bottom shuffle/previous/play_pause/next/repeat transport controls now use the same Compose leaf while preserving previous/next long seek. " +
            "The landscape chapter chevrons are Compose-hosted under previous_chapter and next_chapter. " +
            "The foldable hinge left/right affordances are Compose-hosted under hinge_go_left and hinge_go_right. " +
            "The cover-mode seek/bookmark HUD row is Compose-hosted under the existing audio_rewind/audio_forward IDs with delay labels driven from Settings.audioJumpDelay. " +
            "The shared bookmarks panel is directly hosted through BookmarksPanelView in audio/video HUD roots, replacing the former stub wrapper plus toolbar, list, empty state, bookmark_item.xml rows, and bottom seek/bookmark controls. " +
            "The audio and video playlist overlays now share AudioPlaylistQueue in VLCComposeView-backed hosts, replacing the former PlaylistAdapter wiring and the remaining playlist_item.xml bridge, with the audio and playlist gesture tips overlays now direct-hosted by VLCComposeView. " +
            "The video HUD top/right overlay is Compose-hosted through VideoHudRightOverlayHost installed on a plain VLCComposeView while preserving orientation, speed, sleep, subtitle-delay, and audio-delay actions. " +
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
            "Slot-based leadingContent for icons. Colors: fontAudioLight + listSubtitle. InfoActivity now renders its media track details directly in Compose with VLCInfoItem, replacing the old info_item.xml and adapter bridge.",
            style = MaterialTheme.typography.bodySmall
        )

        // -----------------------------------------------------------------
        // 4. VLCDebugLogLine (live simulator - interactive!)
        // -----------------------------------------------------------------
        VLCSectionHeader(text = "4. VLCDebugLogLine — LIVE SIMULATOR")
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
            "Stateful demo matching the full-Compose DebugLogActivity log rows.",
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
