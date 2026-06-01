# VLC Compose Module

**Isolated Compose entry point for the VLC Android hybrid migration.**

This is a standalone Android library module (`:application:compose`) that provides the foundation for introducing Jetpack Compose into the existing VLC for Android codebase without a big-bang rewrite.

## Why an Isolated Module?

- **Lowest-risk path**: New Compose code lives completely separately from legacy XML/DataBinding/fragment code.
- Permanent exceptions are expected and accepted (native C++ surfaces, complex legacy players, certain dialogs).
- Strategic goal: achieve **~80% migratable surface** over time. The remaining 20% stays in the classic architecture.
- Allows incremental adoption, easy rollback, and parallel development on the bootstrap branch.

## How to Consume

In any other module (most commonly `:application:app` or `:application:television`):

```groovy
// In the consuming module's build.gradle
dependencies {
    implementation project(':application:compose')
}
```

Then import:

```kotlin
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.components.VLCDropdownItem
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
```

## Interop Patterns (Hybrid Migration)

This module deliberately exposes several bridges so Compose can be used immediately inside the existing XML-heavy architecture.

### 1. VLCComposeView (for XML layouts)

Drop-in replacement for `ComposeView`. Use directly inside any legacy layout:

```xml
<org.videolan.vlc.compose.interop.VLCComposeView
    android:id="@+id/my_compose_host"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

In Kotlin (including inside DataBinding fragments/activities):

```kotlin
binding.myComposeHost.setContent {
    VLCTheme {
        MyComposableScreen(...)
    }
}
```

### 2. VLCAbstractComposeWidget (for reusable custom views)

Subclass and implement `WidgetContent()` to create a self-contained Compose widget that can be inflated from XML exactly like any other custom view:

```kotlin
class MyComposeWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    @Composable
    override fun WidgetContent() {
        VLCTheme {
            Text("Hello from Compose widget")
        }
    }
}
```

Use `<your.package.MyComposeWidget ... />` in any layout.

### 3. Full Compose Screens / Destinations

For new screens or major refactors, host entire `@Composable` trees using `VLCTheme` as the root. These can live in Compose-only fragments or be launched via the navigation system once Compose Navigation is introduced.

```kotlin
@Composable
fun MyFullScreen() {
    VLCTheme {
        // Material3 Scaffold, Lists, etc.
    }
}
```

## Current Status

- Basic theming foundation (`VLCTheme`, placeholder semantic tokens mapped from future `colors.xml` + `styles.xml` audit). Tokens expanded with Wave 1 additions (headerBackground, audioBrowserSeparator, onboardingBackground).
- Interop helpers (`VLCComposeView`, `VLCAbstractComposeWidget`).
- Leaf Composables (Wave 1):
  - `VLCDropdownItem` (original)
  - `VLCSectionHeader` (recycler_section_header.xml + tv variant)
  - `VLCInfoItem` (info_item.xml - high reuse in track info)
  - `VLCDebugLogLine` (debug_log_item.xml)
  - `VLCDialogConfirmDelete` (presentational parts of dialog_confirm_delete.xml)
  - `VLCOnboardingWelcome` (static parts of onboarding_welcome.xml)
  - `VLCBrowserItemRow` / `VLCBrowserItemCard` (Wave 2 media browser rows/cards from browser_item/audio_browser_item/audio_browser_card_item/history/mrl row patterns)
  - `VLCEmptyState` (Wave 2 shared loading/empty surfaces for audio/video/browser/playlist screens)
  - `VLCAudioPlayerChips` (Wave 2 playback speed + sleep quick-action chips from audio_player.xml)
  - `VLCVideoQuickActions` (Wave 2 video HUD quick-action chips now hosted by the Compose top/right HUD overlay)
  - `VLCAudioQueueProgressPill` (Wave 2 audio queue progress HUD chip from audio_player.xml)
  - `VLCAudioMiniProgressBar` (Wave 2 collapsed audio-player mini playback progress bar from audio_player.xml)
  - `VLCAudioPlayerBackground` (Wave 2 audio-player blurred cover background from audio_player.xml)
  - `VLCAudioPlayerGradient` (Wave 2 audio-player top/bottom gradient overlays from audio_player.xml)
  - `VLCAudioPlaylistItem` (Wave 2 audio/video playlist row)
  - `VLCAudioPlaylistSearchField` (Wave 2 audio-player playlist search input from audio_player.xml)
  - `VLCAudioTimelineSlider` (Wave 2 audio/video full-player timeline seekbars from audio_player.xml and player_hud.xml)
  - `VLCAudioResumeVideoHint` (Wave 2 audio-player restore-video hint from AudioPlayer.onResume)
  - `VLCAudioHeaderBackground` / `VLCAudioHeaderDivider` (Wave 2 collapsed audio-player header decorative surfaces from audio_player.xml)
  - `VLCAudioHeaderTimeLabel` (Wave 2 collapsed audio-player header time label from audio_player.xml)
  - `VLCAudioHeaderActionButton` (Wave 2 collapsed audio-player search/playlist/overflow + AB-repeat reset/stop actions from audio_player.xml)
  - `VLCAudioHeaderPlayPauseButton` (Wave 2 collapsed audio-player mini play/pause control from audio_player.xml)
  - `VLCAudioHeaderTransportButton` (Wave 2 audio-player header + full-player transport + landscape chapter + foldable hinge controls from audio_player.xml)
  - `VLCAudioSeekHudButton` / `VLCAudioSeekDelayLabel` (Wave 2 audio-player cover-mode seek/bookmark HUD controls from audio_player.xml)
  - `VLCPlayerOptionItem` (Wave 2 shared player options row from player_option_item.xml)
  - `VLCBookmarkRow` (Wave 2 shared bookmark list row from bookmark_item.xml)
  - `VLCAbRepeatControls` (Wave 2 shared A-B repeat add-marker chip root now hosted directly in audio/video HUD layouts)
  - `VLCAbRepeatChipIcon` (Wave 2 shared A-B repeat add-marker chip icon now hosted directly in audio/video HUD layouts)
  - `VLCAbRepeatAddMarkerButton` (Wave 2 shared A-B repeat add-marker button now hosted directly in audio/video HUD layouts)
  - `VLCAudioAbRepeatMarkers` / `VLCAudioAbRepeatMarker` (Wave 2 audio/video A-B repeat timeline marker strip from audio_player.xml + player_hud.xml)
  - `VLCBookmarkMarkers` (Wave 2 audio/video bookmark timeline markers from audio_player.xml + player_hud.xml)
  - `VLCAudioTrackInfoText` (Wave 2 landscape audio-player title/subtitle/detail text from audio_player.xml)
  - `VLCAudioTimelineTimeLabel` (Wave 2 audio-player elapsed/length labels from audio_player.xml)
- Full Compose Activity screens:
  - `VLCOTPCodeScreen` hosted by `OTPCodeActivity` (former OTP XML layout removed)
  - `VLCBetaWelcomeScreen` hosted by `BetaWelcomeActivity` (former DataBinding XML layout removed)
  - `VLCAuthorsScreen` hosted by `AuthorsActivity` (former DataBinding RecyclerView/list XML removed)
  - `VLCLibrariesScreen` hosted by `LibrariesActivity` (former DataBinding RecyclerView/list XML removed; license detail is Compose in this screen)
  - `VLCAboutScreen` hosted by `AboutActivity` (former About XML and license/version bottom-sheet paths removed)
  - `VLCFeedbackScreen` hosted by `FeedbackActivity` (former DataBinding XML layout removed)
- Compose-hosted root screens:
  - `PreferencesActivity` root settings list plus Interface, Video, Subtitles, Audio, Casting, Parental Control, Remote Access, Android Auto, Advanced, and Optional Features subpages (former XML preference subpages removed)
  - `VLCPinCodeScreen` hosted by `PinCodeActivity` (former DataBinding PIN keypad XML layout removed)
  - `VLCSearchScreen` hosted by `SearchActivity` (former DataBinding Activity and result row XML layouts removed)
  - `VLCEqualizerSettingsScreen` hosted by `EqualizerSettingsActivity` (former DataBinding Activity/list row XML layouts and menu XML removed)
  - `VLCEqualizerEditorDialogContent` hosted by a Compose player bottom sheet (former vertical slider views and dialog XML layouts removed)
  - `VLCWhatsNewDialogContent` hosted by a Compose bottom sheet from `WhatsNewManager` (former DataBinding XML removed)
- Compose player hosts:
  - `AudioPlayerContainerActivity` now instantiates `AudioPlayer` as a bottom-sheet controller inside a programmatic `AudioPlayerHostView` (former XML host path removed; bd compose-55n / compose-68e)
- All new leaves respect full VLCTheme tokens + typography, have rich light/dark @Previews, and full KDoc traceability to original XML paths.
- Preview support for Android Studio (see `PreviewUtils.kt`).
- Proper library packaging (consumer rules, proguard, test setup).

**Wave 1 Cross-cutting Milestone (compose-2l4.1.8)**: The dedicated **Compose Interop Lab** now exists as the crown jewel dev-only interactive showcase (see below). It hosts live examples of all six leaves + realistic combined usage (sectioned lists, dialog-in-context, live simulators). This advances both the "interop demo" and "preview + gate enforcement" acceptance criteria for the wave.

This module is intentionally minimal on purpose — it is the **bootstrap foundation**. Real component migration work happens in follow-up issues.

## Design Principles

- Everything public API is under `org.videolan.vlc.compose.*`
- Never enable ViewBinding or DataBinding inside this module.
- Use Material 3 exclusively.
- All Composables should accept `Modifier` and be previewable.
- Theme tokens will be expanded during the color/style audit (Phase 0 / follow-up work).

## Running / Validating

```bash
# From repo root (worktree)
gradle :application:compose:build --no-daemon --console=plain
gradle :application:compose:connectedCheck --no-daemon --console=plain   # requires device/emulator
```

Android Studio will immediately show `@Preview` renders for the theme and components once the module is synced.

## Wave 1 Interop Lab & Build Gate Policy (compose-2l4.1.8)

The **Compose Interop Lab** (`ComposeInteropLabActivity` + `compose_interop_lab.xml`) is the single most important visibility artifact delivered in Wave 1.

- **Launch**: Dev-only button added to `DebugLogActivity` (the primary developer entrypoint, itself reachable from Advanced Preferences). See `debug_log.xml` for the button + its documentation block.
- **What it contains**: A scrollable, interactive column exercising **all six Wave 0/1 leaves live**:
  - `VLCDropdownItem`, `VLCSectionHeader`, `VLCInfoItem`, `VLCDebugLogLine`, `VLCDialogConfirmDelete`, `VLCOnboardingWelcome`
  - Plus richer combined mocks (sectioned list using Header+InfoItem, live log simulator with mutable state, real `AlertDialog` wrapping the delete leaf, onboarding variants, etc.).
- **Pattern**: Full-screen demonstration of **Pattern 1** (VLCComposeView in legacy layout XML + `setContent { VLCTheme { ... } }`). The Activity is intentionally thin.
- **Educational quality**: Matches the exhaustive comment style of the reference hosts `NetworkServerDialog.kt` (first demo, compose-5qk) and `DebugLogActivity.kt` (first real Wave 1 host migration, compose-2l4.1.2 / bd compose-5wg). Every file contains rollback instructions, traceability, Permanent Exceptions notes, and cross-references.
- **Pointers to host files** (update this list as new hosts land):
  - Primary interop demo host: `application/vlc-android/src/org/videolan/vlc/gui/dialogs/NetworkServerDialog.kt` + `network_server_dialog.xml`
  - First real Wave 1 host (list + adapter): `application/vlc-android/src/org/videolan/vlc/gui/DebugLogActivity.kt` + `debug_log.xml` (also contains the Lab launch button)
  - Decoration + browser hosts: Recycler*Decorations + BaseAudioBrowser/Playlist (compose-2l4.1.4)
  - Info surfaces host: `application/vlc-android/src/org/videolan/vlc/gui/InfoActivity.kt` (VLCInfoItem track rows inside a full Compose media-info screen; compose-2l4.1.3 / bd compose-l94 plus compose-l12)
  - Section header hosts + decorations (compose-2l4.1.4 / bd compose-95d)
  - Dialog content host (ConfirmDeleteDialog - keep shell/swap content pattern): `application/vlc-android/src/org/videolan/vlc/gui/dialogs/ConfirmDeleteDialog.kt` (compose-2l4.1.5 / bd compose-j0e)
  - Onboarding first-run host (high-visibility welcome flow): Compose onboarding welcome content (compose-2l4.1.6 / bd compose-mdj) — real logo slot + land variant noted, layouts 100% preserved
  - Media/browser row hosts: `PlaylistScreen.kt`, `VideoScreen.kt`, `MainBrowserScreen.kt`, `HistoryAdapter.kt`, `AudioBrowserAdapter.kt`, and `AudioAlbumTracksAdapter.kt` (`VLCBrowserItemRow` / `VLCBrowserItemCard`; compose-q9r.1 / compose-q9r.6 / compose-q9r.7; active paths no longer inflate the corresponding browser, history, stream card, or album-track row XML layouts)
  - Shared empty/loading hosts: `AudioScreen.kt`, `VideoScreen.kt`, `MainBrowserScreen.kt`, and `PlaylistScreen.kt` (`VLCEmptyState`; compose-q9r.2; replaces duplicated private empty/loading composables and removes the unused legacy empty-loading XML view)
  - Audio/video playlist queue hosts: `application/vlc-android/src/org/videolan/vlc/gui/audio/AudioPlayer.kt` + `AudioPlaylistQueue.kt` + `AudioPlaylistMediaItem.kt` + `AudioPlaylistTipsDelegate.kt` + `AudioPlaylistTipsView.kt` + `application/vlc-android/src/org/videolan/vlc/gui/video/VideoPlayerActivity.kt` + `VideoPlayerOverlayDelegate.kt` + `player.xml` + `audio_player.xml` / `layout-land/audio_player.xml` (`VLCAudioPlaylistItem`; compose-68e / compose-7py; replaces the former shared `PlaylistAdapter` RecyclerView path, `playlist_item.xml` bridge, and `audio_playlist_tips.xml` ViewStub payload with Compose LazyColumn hosts and a Compose playlist tips overlay)
  - Audio player chrome hosts: `application/vlc-android/src/org/videolan/vlc/gui/AudioPlayerContainerActivity.kt` + `application/vlc-android/src/org/videolan/vlc/gui/AudioPlayerHostView.kt` + `application/vlc-android/src/org/videolan/vlc/gui/audio/AudioPlayer.kt` + `AudioPlaylistTipsDelegate.kt` + `audio_player.xml` / `layout-land/audio_player.xml` (`VLCAudioPlayerChips`, `VLCAudioQueueProgressPill`, `VLCAudioMiniProgressBar`, `VLCAudioPlayerBackground`, `VLCAudioPlayerGradient`, `VLCAudioPlaylistSearchField`, `VLCAudioTimelineSlider`, `VLCAudioResumeVideoHint`, `VLCAudioHeaderBackground`, `VLCAudioHeaderDivider`, `VLCAudioHeaderTimeLabel`, `VLCAudioHeaderActionButton`, `VLCAudioHeaderPlayPauseButton`, `VLCAudioHeaderTransportButton`, `VLCAudioSeekHudButton`, `VLCAudioSeekDelayLabel`, `VLCAudioAbRepeatMarkers`, `VLCBookmarkMarkers`, `VLCAudioTrackInfoText`, `VLCAudioTimelineTimeLabel`; compose-q9r.3 / compose-68e / compose-55n; includes the bottom-sheet controller shell plus blurred cover background, gradient overlays, playlist search input, timeline seekbar, restore-video hint, foldable hinge affordances, A-B repeat markers, bookmark timeline markers, landscape track info text, and full-player timeline labels)
  - Phone shell scan progress host: `application/vlc-android/src/org/videolan/vlc/gui/PhoneActivityShell.kt` + `AudioPlayerContainerActivity.kt` + `ScanProgressView.kt` (`ScanProgressView`; compose-f5c; replaces the shared `scan_progress.xml` ViewStub payload with a direct Compose host while preserving media-library discovery text, determinate/indeterminate parsing progress, and CoordinatorLayout anchoring/margins)
  - Video player lightweight overlay hosts: `application/vlc-android/src/org/videolan/vlc/gui/video/VideoPlayerOverlayDelegate.kt` + `VideoHudOverlayViews.kt` + `VideoStatsDelegate.kt` + `VideoTipsDelegate.kt` + `VideoDelayDelegate.kt` + `VideoTouchDelegate.kt` + `player.xml` + `player_remote_control.xml` + `player_hud.xml` (`VideoInfoOverlayView`, `VideoTipsHostView`, `VideoVerticalProgressOverlayView`, `VideoDelayOverlayView`, `VideoSeekOverlayView`, `VideoHudRightOverlayView`, `SwipeToUnlockView`, `VideoTimelineSeekBarView`, `VideoTimelineTimeLabelView`, `AbRepeatMarkerContainerView`, `VLCAudioAbRepeatMarkers`, `VideoHudIconButtonView`, `VideoHudSeekJumpLabelView`, `VideoStatsOverlayView`; compose-91m / compose-68e / compose-slm / compose-6o9 / compose-oxh / compose-f7r; replaces the transient info ViewStub XML, the video tips ViewStub payload, brightness/volume progress ViewStub payloads, the audio/subtitle delay settings ViewStub payload, the double-tap seek/fast-play ViewStub payload, the top/right HUD ViewStub/DataBinding payload, the remaining bottom HUD `ViewStubCompat` host, the swipe-to-unlock XML payload, the video timeline `AccessibleSeekBar`, the video elapsed/length `FocusableTextView` labels, A-B repeat marker guideline/DataBinding islands, video A-B repeat reset/stop/warning `ImageView` icon buttons, static video HUD close/tracks/previous/rewind/forward/next/resize/more action icon buttons, the orientation icon button, seek jump number labels, the video play/pause icon button, the stats panel `NestedScrollView`/dynamic grid/`PlotView`/`LegendView` XML island, the remaining `player_hud.xml` DataBinding variables/expressions, and the generated `PlayerHudBinding` view holder; `player_hud.xml` remains an XML shell pending controller migration)
  - Video player quick-action hosts: `application/vlc-android/src/org/videolan/vlc/gui/video/VideoPlayerOverlayDelegate.kt` + `VideoHudRightOverlayView.kt` (`VLCVideoQuickActions`; compose-8pj / compose-f7r; replaces the Material `HorizontalScrollView`/`ChipGroup` quick-action strip and now hosts it inside the full Compose top/right HUD while preserving orientation, speed, sleep, subtitle-delay, and audio-delay callbacks)
  - Shared playback bookmark hosts: `application/vlc-android/src/org/videolan/vlc/gui/helpers/BookmarkListDelegate.kt` + `BookmarkMarkerContainerView.kt` + `BookmarksPanelView.kt` + `audio_player.xml` / `layout-land/audio_player.xml` / `player_hud.xml` (`VLCBookmarkMarkers`, `VLCBookmarkRow`; compose-68e / compose-8gz; timeline markers plus the directly hosted shared bookmarks panel used by AudioPlayer and VideoPlayerOverlayDelegate)
  - Shared playback A-B repeat root: `application/vlc-android/src/org/videolan/vlc/gui/view/AbRepeatControlsView.kt` hosted directly in `audio_player.xml`, `layout-land/audio_player.xml`, and `player_hud.xml` (`VLCAbRepeatControls`, `VLCAbRepeatChipIcon`, `VLCAbRepeatAddMarkerButton`; compose-68e; former shared include XML removed)
  - Shared playback option panel: `application/vlc-android/src/org/videolan/vlc/gui/helpers/PlayerOptionsDelegate.kt` + `PlayerOptionsPanelView.kt` + `audio_player.xml` / `layout-land/audio_player.xml` / `player.xml` / `player_remote_control.xml` (`VLCPlayerOptionItem`; compose-68e / compose-8gz; directly hosted by audio/video/player remote option panels through PlayerOptionsDelegate)
  - TV audio-player row host: `application/television/src/main/java/org/videolan/television/ui/audioplayer/AudioPlayerActivity.kt` + `PlaylistAdapter.kt` (`TvPlaylistRow`; compose-st7; replaces the `tv_playlist_item.xml` DataBinding row and stale TV bookmark/player-options ViewStub layout refs with Compose-backed row/panel hosts)
  - Preferences root/subpage host: `application/vlc-android/src/org/videolan/vlc/gui/preferences/PreferencesActivity.kt` + `PreferencesRootScreen.kt` + `PreferencesComposeSubpages.kt` (bd compose-qeh / compose-0ep; removes the phone root and all phone XML preference subpages, with TV preferences tracked separately)
  - Crown jewel cross-cutting Lab (this milestone): `application/vlc-android/src/org/videolan/vlc/gui/ComposeInteropLabActivity.kt` + `compose_interop_lab.xml`
  - All leaves + interop + theme: `application/compose/src/main/java/org/videolan/vlc/compose/{components,interop,theme}/*`
  - Richer mocks derived from the Lab: `application/compose/src/main/java/org/videolan/vlc/compose/PreviewUtils.kt`
  - Manifest registration (additive): `application/vlc-android/AndroidManifest.xml`
  - Tracking: bd issue `compose-iju` (labeled `compose-2l4.1.8`)

### The "Every Host Must Have Green Compile Gate Evidence" Policy

**Enforced starting with compose-2l4.1.8**:

> Every interop host (NetworkServerDialog, DebugLogActivity, InfoActivity, ComposeInteropLabActivity, OnboardingWelcomeFragment, and all future ones) **must** have documented evidence that it compiles cleanly against the current `:application:compose` leaves.

**The gate command** (run from the worktree root):
```bash
ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:vlc-android:compileDebugKotlin --no-daemon --console=plain
# or the stricter full suite:
ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain
```

**Where evidence lives**:
- Terminal output captured during the session (SUCCESS blocks).
- Appended to bd notes on the cross-cutting task (`compose-iju`).
- Referenced in the host file headers (see the massive comment blocks in the three hosts).
- Summarized in this README (below) and in future PR descriptions.

**Why this policy**:
- Prevents the hybrid bridge from rotting silently as leaves or tokens evolve.
- The Lab + the two earlier hosts act as canaries.
- Part of the "preview + gate enforcement" acceptance criteria for Wave 1.

**Current gate evidence**:
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 1m` after replacing the generated `PlayerHudBinding` view holder with `VideoHudOverlayViews` and converting `player_hud.xml` from a DataBinding `<layout>` wrapper to a plain `ConstraintLayout` root. Focused active-code scans found no `PlayerHudBinding`, `DataBindingUtil`, DataBinding tags/expressions, `player_hud_stub`, `ViewStubCompat`, or orphan video HUD binding adapters in the active video HUD path.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 59s` after replacing the remaining `player_hud_stub` `ViewStubCompat` hosts in `player.xml` and `player_remote_control.xml` with direct `player_hud` includes and moving `player_hud.xml` progress/click DataBinding expressions into explicit `VideoPlayerOverlayDelegate` observers/listeners. Focused scans found no `player_hud_stub`, `ViewStubCompat`, `<data>`, `@{}`, or orphan video HUD binding adapters in the active video HUD path.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the video HUD stats panel XML internals with `VideoStatsOverlayView`, preserving media stat sampling, track rows, graph/legend rendering, close action, scroll callbacks, and portrait/landscape layout switching while removing the old `NestedScrollView`, dynamic `info_grids`, `PlotView`, `LegendView`, and `stats_close` binding path.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 26s` after replacing the video HUD play/pause `ImageView` with `VideoHudIconButtonView`, preserving the existing `player_overlay_play` ID, click/focus/visibility/enabled/content-description contract, play/pause/disabled icon updates, and removing the video-only animated-vector setup from `VideoPlayerActivity`/`VideoPlayerOverlayDelegate`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 24s` after replacing the video HUD orientation `ImageView` with `VideoHudIconButtonView` and the rewind/forward jump-number `TextView`s with `VideoHudSeekJumpLabelView`, preserving dynamic orientation resource updates, click/long-click dispatch, seek label text/visibility updates, and accessibility suppression.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 23s` after replacing the static video HUD close/tracks/previous/rewind/forward/next/resize/more `ImageView` buttons with `VideoHudIconButtonView`, preserving existing IDs, click and long-click dispatch, enabled/icon updates, key listeners, and layout constraints while leaving only the dynamic orientation and animated play/pause `ImageView`s in that control row.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 7s` after replacing the video HUD A-B repeat reset/stop/warning `ImageView` buttons with `VideoHudIconButtonView`, preserving the existing IDs, content descriptions, click listeners, visibility hooks, and icon resources.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the video HUD elapsed/length `FocusableTextView` labels with `VideoTimelineTimeLabelView`, preserving the existing IDs, click-to-toggle remaining time behavior, focus highlight, lock enable state, margins, and bookmark height anchoring.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 30s` after replacing the video HUD `AccessibleSeekBar` with `VideoTimelineSeekBarView`, sharing the Compose timeline bridge with the audio player, preserving max/progress binding, seek callbacks, TalkBack announcements, and deleting the obsolete native seekbar class.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 17s` after replacing `swipe_to_unlock.xml` with the Compose-backed `SwipeToUnlockView`, preserving touch drag, RTL handling, DPAD long-press unlock, focus handoff, and deleting the obsolete rounded background XML resource.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 16s` after replacing the video top/right HUD `ViewStub`/DataBinding payload with `VideoHudRightOverlayView`, preserving title/warning display, nav/screenshot/renderer/secondary-display/playlist actions, TV clock visibility, and quick-action chip state while deleting `player_hud_right.xml` and the obsolete quick-action XML bridge.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 15s` after replacing the video double-tap seek and tap-and-hold fast-play `ViewStub` payload with `VideoSeekOverlayView`, preserving direction text, arrow animation, TV-shaped seek panels, fast-play title/indicator, and deleting `player_overlay_seek.xml` plus the orphan `HalfCircleView`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 15s` after replacing the video audio/subtitle delay settings `ViewStub` payload with `VideoDelayOverlayView`, preserving delay value updates, marker state, reset/apply/close actions, focus handoff, and deleting `player_overlay_settings.xml`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 27s` after replacing the phone shell `scan_progress.xml` ViewStub payload with `ScanProgressView`, preserving media-library discovery/progress updates and CoordinatorLayout anchoring while deleting the obsolete scan background XML resources.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after replacing the video brightness/volume `ViewStub` payloads with `VideoVerticalProgressOverlayView`, preserving value updates, fade-out handling, and audio-boost double-range rendering while deleting `player_overlay_brightness.xml` and `player_overlay_volume.xml`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 23s` after replacing the video player tips `ViewStub`/`player_tips.xml` overlay with `VideoTipsHostView`, preserving dismiss/next/control selection sequencing through `VideoTipsDelegate`, and hosting the Compose overlay directly in `player.xml` and `player_remote_control.xml`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin :application:television:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 10s` after replacing the TV audio-player playlist DataBinding row with a ComposeView-backed `TvPlaylistRow`, deleting `tv_playlist_item.xml`, preserving play/pause row refresh, and direct-hosting the TV bookmark/player-options Compose panels instead of stale stub layout refs.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 26s` after replacing the video HUD quick-action `HorizontalScrollView`/Material chip strip with `VLCVideoQuickActions`, and deleting the audio playlist tips XML payload in favor of `AudioPlaylistTipsHostView` + `AudioPlaylistTipsView`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 23s` after adding `VLCEmptyState`, hosting it directly in the phone audio, video, browser, and playlist Compose screens, adding Lab/PreviewUtils coverage, and deleting the unused legacy empty-loading XML view/styleable.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 11s` after routing the phone Advanced and Optional Features preference subpages through `PreferencesComposeSubpages.kt`, preserving DB clear/dump/export/restore actions, network caching validation, LibVLC restart side effects, nightly install/update/debug actions, and feature-flag warning flow.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after routing the phone Subtitles preference subpage through `PreferencesComposeSubpages.kt`, preserving preset reset behavior, text encoding/preferred-language lists, dependent background/shadow/outline rows, color and opacity controls, and deferred media-pipeline restart on page exit.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after routing the phone Interface preference subpage through `PreferencesComposeSubpages.kt`, preserving theme default migration, locale/theme restart prompts, list-title/include-missing restart state, sleep timer summary/dialog refresh, media-seen result propagation, incognito dependency, and phone visibility rules.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after routing the phone Audio preference subpage through `PreferencesComposeSubpages.kt`, preserving preferred-language locale entries, `PreferenceVisibilityManager`-gated rows, digital-output summary, headset detection callback, ReplayGain dependencies/formatting/restart behavior, and SoundFont picker.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 12s` after routing the phone Video preference subpage through `PreferencesComposeSubpages.kt`, preserving the video-settings disclaimer, preferred-resolution restart, popup permission checks, and secondary-display rows.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 12s` after routing the phone Remote Access preference subpage through `PreferencesComposeSubpages.kt`, including its onboarding menu, server status action, server start/stop, restart trigger, and media-library multi-select behavior.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 11s` after routing Casting, Parental Control, and Android Auto phone preference subpages through `PreferencesComposeSubpages.kt`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after replacing the audio-player XML shell with a plain `FrameLayout` host and constructing `AudioPlayer` as a bottom-sheet controller from `AudioPlayerContainerActivity`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the phone root preference XML screen with `PreferencesRootScreen` hosted by `PreferencesActivity`, preserving root setting toggles/list dialogs and endpoint routing into existing subpages.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after moving `AudioAlbumTracksAdapter` active album track rows to `ComposeView` + `VLCBrowserItemRow`, preserving track-number/current-track/subtitle/action behavior while keeping `audio_album_track_item.xml` buildable as rollback.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after deleting `playlist_item.xml` and `PlaylistItemView.kt`, moving the app-side row artwork/action bridge into `AudioPlaylistMediaItem.kt`, and creating the audio playlist tips fake rows programmatically as Compose widgets.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after deleting `playlist_item.xml` and `PlaylistItemView.kt`, moving the app-side row artwork/action bridge into `AudioPlaylistMediaItem.kt`, and creating the audio playlist tips fake rows programmatically as Compose widgets.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the video-player overlay `video_playlist` RecyclerView host with the shared VLCComposeView-backed `AudioPlaylistQueue` LazyColumn, moving `VideoPlayerActivity`/`VideoPlayerOverlayDelegate` off the former shared `PlaylistAdapter` path and deleting `PlaylistAdapter.kt`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 17s` after replacing the audio-player `songs_list` RecyclerView host with a VLCComposeView-backed `AudioPlaylistQueue` LazyColumn, moving AudioPlayer off `PlaylistAdapter`/`ItemTouchHelper` while preserving playlist row actions, context menu, stop-after-this, swipe removal, and inline tablet move/delete controls in Compose.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 14s` after replacing `playlist_item.xml` DataBinding content with `PlaylistItemView` + `VLCAudioPlaylistItem`, including the audio playlist tips fake rows, while keeping the existing RecyclerView swipe/drag host for the next list-surface migration.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 17s` after replacing the shared bookmark panel toolbar/RecyclerView/empty state/bottom controls, `bookmark_item.xml` DataBinding rows, and the bookmark overflow menu XML with `BookmarksPanelView` + `VLCBookmarkRow`, with marker container callbacks preserved.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 18s` after replacing the shared player options `BrowseFrameLayout`/`RecyclerView` shell and row bridge with `PlayerOptionsPanelView`, preserving dynamic option callbacks.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 23s` after replacing `player_option_item.xml` DataBinding rows with `PlayerOptionItemView` + `VLCPlayerOptionItem` in `PlayerOptionsDelegate`, preserving the existing options panel RecyclerView/focus shell.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 19s` after replacing the shared A-B repeat include root `ConstraintLayout`/child bridge views with `AbRepeatControlsView` + `VLCAbRepeatControls`, with audio/video hosts driving the include root directly.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after replacing the audio-player restore-video snackbar hint with `VLCAudioResumeVideoHint` hosted under the existing audio player surface via `resume_video_hint` in portrait and landscape.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after replacing the shared audio/video bookmark marker overlay `ConstraintLayout` + dynamic `ImageView` marker population with `VLCBookmarkMarkers` through `BookmarkMarkerContainerView` under the existing `bookmark_marker_container` IDs.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the audio-player blurred cover background `ImageView` with `VLCAudioPlayerBackground` through an XML-friendly Compose widget under the existing `backgroundView` ID.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after replacing the audio-player full-player timeline `AccessibleSeekBar` with `VLCAudioTimelineSlider` through an XML-friendly Compose widget under the existing `timeline` ID.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the audio-player playlist search `TextInputLayout`/`EditText` with `VLCAudioPlaylistSearchField` through an XML-friendly Compose widget under the existing `playlist_search_text` ID.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 25s` after replacing the audio-player top/bottom gradient overlay `View`s with `VLCAudioPlayerGradient` through XML-friendly Compose widgets under the existing `top_gradient` and `bottom_gradient` IDs.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the audio-player collapsed mini `ProgressBar` with `VLCAudioMiniProgressBar` via `AudioMiniProgressBarView`, preserving the existing `progressBar` max/progress and slide-height contract.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the audio-player collapsed header background/divider XML Views with `VLCAudioHeaderBackground` / `VLCAudioHeaderDivider` under the existing animated IDs.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 13s` after replacing the shared A-B repeat add-marker button with `VLCAbRepeatAddMarkerButton` via the shared Compose root, with audio/video hosts driven by the callback-based `manageAbRepeatStep` overload.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the shared A-B repeat add-marker chip icon with `VLCAbRepeatChipIcon` via the shared Compose root.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after replacing the audio-player elapsed/length timeline labels with `VLCAudioTimelineTimeLabel`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the landscape audio-player track info title/subtitle/detail text with `VLCAudioTrackInfoText`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after initially replacing the audio-player A-B repeat timeline marker leaves with `VLCAudioAbRepeatMarker`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 35s` after replacing the audio/video A-B repeat marker guideline/DataBinding islands with `AbRepeatMarkerContainerView` + `VLCAudioAbRepeatMarkers`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 35s` after replacing the transient video info `ViewStub`/`player_overlay_info.xml` overlay with `VideoInfoOverlayView`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 24s` after replacing the remaining audio/video player options and bookmarks stub wrappers with direct `PlayerOptionsPanelView` and `BookmarksPanelView` hosts, deleting the two wrapper layouts.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 24s` after removing the remaining `audio_player.xml` / `layout-land/audio_player.xml` DataBinding variables and XML expressions, with shuffle visibility now driven from `AudioPlayer`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the audio-player foldable hinge left/right affordances with `VLCAudioHeaderTransportButton`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the landscape audio-player chapter chevrons with `VLCAudioHeaderTransportButton`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the audio-player AB-repeat reset/stop header actions with `VLCAudioHeaderActionButton`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the audio-player cover-mode seek/bookmark HUD buttons and delay labels with `VLCAudioSeekHudButton` / `VLCAudioSeekDelayLabel`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the full-player bottom audio transport controls with `VLCAudioHeaderTransportButton`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the tablet audio-player header transport strip with `VLCAudioHeaderTransportButton`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the collapsed audio-player mini play/pause button with `VLCAudioHeaderPlayPauseButton`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 23s` after replacing the collapsed audio-player search, playlist switch, and overflow action icons with `VLCAudioHeaderActionButton`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the collapsed audio-player header time label with `VLCAudioHeaderTimeLabel`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 25s` after replacing the audio queue progress pill with `VLCAudioQueueProgressPill`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 10s` after replacing the audio-player quick-action ChipGroup with `VLCAudioPlayerChips`.
- 2026-05-31: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 1m 24s` after the worktree was made tolerant of absent local `libvlcjni/libvlc` by falling back to the published `org.videolan.android:libvlc-all` artifact.
- 2026-05-31: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 1m 37s`.
- All current Wave 0/1 hosts (NetworkServerDialog, DebugLogActivity, InfoActivity, ComposeInteropLabActivity, OnboardingWelcomeFragment, OTPCodeActivity) participate in the app Kotlin gate.

Future agents: re-run the gate after touching any leaf or host, append the SUCCESS tail + timestamp to bd, and update this section.

---

**Part of the phase-0-compose-bootstrap effort.**  
See parent tracking issue and reconnaissance notes for the overall 80% migration strategy.
