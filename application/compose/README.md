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
  - `VLCAudioPlayerChips` (Wave 2 playback speed + sleep quick-action chips from audio_player.xml)
  - `VLCAudioQueueProgressPill` (Wave 2 audio queue progress HUD chip from audio_player.xml)
  - `VLCAudioMiniProgressBar` (Wave 2 collapsed audio-player mini playback progress bar from audio_player.xml)
  - `VLCAudioPlayerBackground` (Wave 2 audio-player blurred cover background from audio_player.xml)
  - `VLCAudioPlayerGradient` (Wave 2 audio-player top/bottom gradient overlays from audio_player.xml)
  - `VLCAudioPlaylistItem` (Wave 2 audio/video playlist row)
  - `VLCAudioPlaylistSearchField` (Wave 2 audio-player playlist search input from audio_player.xml)
  - `VLCAudioTimelineSlider` (Wave 2 audio-player full-player timeline seekbar from audio_player.xml)
  - `VLCAudioResumeVideoHint` (Wave 2 audio-player restore-video hint from AudioPlayer.onResume)
  - `VLCAudioHeaderBackground` / `VLCAudioHeaderDivider` (Wave 2 collapsed audio-player header decorative surfaces from audio_player.xml)
  - `VLCAudioHeaderTimeLabel` (Wave 2 collapsed audio-player header time label from audio_player.xml)
  - `VLCAudioHeaderActionButton` (Wave 2 collapsed audio-player search/playlist/overflow + AB-repeat reset/stop actions from audio_player.xml)
  - `VLCAudioHeaderPlayPauseButton` (Wave 2 collapsed audio-player mini play/pause control from audio_player.xml)
  - `VLCAudioHeaderTransportButton` (Wave 2 audio-player header + full-player transport + landscape chapter + foldable hinge controls from audio_player.xml)
  - `VLCAudioSeekHudButton` / `VLCAudioSeekDelayLabel` (Wave 2 audio-player cover-mode seek/bookmark HUD controls from audio_player.xml)
  - `VLCPlayerOptionItem` (Wave 2 shared player options row from player_option_item.xml)
  - `VLCBookmarkRow` (Wave 2 shared bookmark list row from bookmark_item.xml)
  - `VLCAbRepeatControls` (Wave 2 shared A-B repeat add-marker chip root from ab_repeat_controls.xml)
  - `VLCAbRepeatChipIcon` (Wave 2 shared A-B repeat add-marker chip icon from ab_repeat_controls.xml)
  - `VLCAbRepeatAddMarkerButton` (Wave 2 shared A-B repeat add-marker button from ab_repeat_controls.xml)
  - `VLCAudioAbRepeatMarker` (Wave 2 audio-player A-B repeat timeline markers from audio_player.xml)
  - `VLCBookmarkMarkers` (Wave 2 audio/video bookmark timeline markers from audio_player.xml + player_hud.xml)
  - `VLCAudioTrackInfoText` (Wave 2 landscape audio-player title/subtitle/detail text from audio_player.xml)
  - `VLCAudioTimelineTimeLabel` (Wave 2 audio-player elapsed/length labels from audio_player.xml)
- Full Compose Activity screens:
  - `VLCOTPCodeScreen` hosted by `OTPCodeActivity` (former OTP Fragment/layout removed)
  - `VLCBetaWelcomeScreen` hosted by `BetaWelcomeActivity` (former DataBinding XML layout removed)
  - `VLCAuthorsScreen` hosted by `AuthorsActivity` (former DataBinding RecyclerView/list XML removed)
  - `VLCLibrariesScreen` hosted by `LibrariesActivity` (former DataBinding RecyclerView/list XML removed; license detail is Compose in this screen)
  - `VLCAboutScreen` hosted by `AboutActivity` (former About Fragment/XML and About license/version bottom-sheet Fragment paths removed)
  - `VLCFeedbackScreen` hosted by `FeedbackActivity` (former DataBinding XML layout removed)
- Compose-hosted root screens with remaining subpage follow-up:
  - `PreferencesActivity` root settings list (former phone root preference Fragment removed; deeper `BasePreferenceFragment` subpages still tracked separately)
  - `VLCPinCodeScreen` hosted by `PinCodeActivity` (former DataBinding PIN keypad XML layout removed)
  - `VLCSearchScreen` hosted by `SearchActivity` (former DataBinding Activity and result row XML layouts removed)
  - `VLCEqualizerSettingsScreen` hosted by `EqualizerSettingsActivity` (former DataBinding Activity/list row XML layouts and menu XML removed)
  - `VLCEqualizerEditorDialogContent` hosted by a non-Fragment player bottom sheet (former `EqualizerFragmentDialog`, vertical slider views, and dialog XML layouts removed)
  - `VLCWhatsNewDialogContent` hosted by a non-Fragment bottom sheet from `WhatsNewManager` (former `WhatsNewDialog` Fragment and DataBinding XML removed)
- Fragment-free player hosts:
  - `AudioPlayerContainerActivity` now instantiates `AudioPlayer` as a bottom-sheet controller inside the `audioplayer.xml` `FrameLayout` host (former `FragmentContainerView` shell and `AudioPlayer : Fragment` path removed; bd compose-55n)
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
  - Info surfaces host (MediaInfoAdapter + InfoActivity): `application/vlc-android/src/org/videolan/vlc/gui/video/MediaInfoAdapter.kt` + `application/vlc-android/src/org/videolan/vlc/gui/InfoActivity.kt` (compose-2l4.1.3 / bd compose-l94)
  - Section header hosts + decorations (compose-2l4.1.4 / bd compose-95d)
  - Dialog content host (ConfirmDeleteDialog - keep shell/swap content pattern): `application/vlc-android/src/org/videolan/vlc/gui/dialogs/ConfirmDeleteDialog.kt` (compose-2l4.1.5 / bd compose-j0e)
  - Onboarding first-run host (high-visibility welcome flow): `application/vlc-android/src/org/videolan/vlc/gui/onboarding/OnboardingWelcomeFragment.kt` (compose-2l4.1.6 / bd compose-mdj) — real logo slot + land variant noted, layouts 100% preserved
  - Media/browser row hosts: `PlaylistScreen.kt`, `VideoScreen.kt`, `MainBrowserScreen.kt`, `MRLAdapter.kt`, `HistoryAdapter.kt`, `AudioBrowserAdapter.kt`, and `AudioAlbumTracksAdapter.kt` (`VLCBrowserItemRow` / `VLCBrowserItemCard`; compose-q9r.1 / compose-q9r.6 / compose-q9r.7; active paths no longer inflate the corresponding browser, history, MRL card, or album-track row XML layouts)
  - Audio/video playlist queue hosts: `application/vlc-android/src/org/videolan/vlc/gui/audio/AudioPlayer.kt` + `AudioPlaylistQueue.kt` + `AudioPlaylistMediaItem.kt` + `AudioPlaylistTipsDelegate.kt` + `application/vlc-android/src/org/videolan/vlc/gui/video/VideoPlayerActivity.kt` + `VideoPlayerOverlayDelegate.kt` + `player.xml` + `audio_player.xml` / `layout-land/audio_player.xml` (`VLCAudioPlaylistItem`; compose-68e; replaces the former shared `PlaylistAdapter` RecyclerView path plus the `playlist_item.xml` bridge with Compose LazyColumn hosts and programmatic Compose tips rows)
  - Audio player chrome hosts: `application/vlc-android/src/org/videolan/vlc/gui/AudioPlayerContainerActivity.kt` + `application/vlc-android/src/org/videolan/vlc/gui/audio/AudioPlayer.kt` + `AudioPlaylistTipsDelegate.kt` + `audioplayer.xml` + `audio_player.xml` / `layout-land/audio_player.xml` (`VLCAudioPlayerChips`, `VLCAudioQueueProgressPill`, `VLCAudioMiniProgressBar`, `VLCAudioPlayerBackground`, `VLCAudioPlayerGradient`, `VLCAudioPlaylistSearchField`, `VLCAudioTimelineSlider`, `VLCAudioResumeVideoHint`, `VLCAudioHeaderBackground`, `VLCAudioHeaderDivider`, `VLCAudioHeaderTimeLabel`, `VLCAudioHeaderActionButton`, `VLCAudioHeaderPlayPauseButton`, `VLCAudioHeaderTransportButton`, `VLCAudioSeekHudButton`, `VLCAudioSeekDelayLabel`, `VLCAudioAbRepeatMarker`, `VLCBookmarkMarkers`, `VLCAudioTrackInfoText`, `VLCAudioTimelineTimeLabel`; compose-q9r.3 / compose-68e / compose-55n; includes the Fragment-free bottom-sheet controller shell plus blurred cover background, gradient overlays, playlist search input, timeline seekbar, restore-video hint, foldable hinge affordances, A-B repeat markers, bookmark timeline markers, landscape track info text, and full-player timeline labels)
  - Shared playback bookmark hosts: `application/vlc-android/src/org/videolan/vlc/gui/helpers/BookmarkListDelegate.kt` + `BookmarkMarkerContainerView.kt` + `BookmarksPanelView.kt` + `bookmarks.xml` + `audio_player.xml` / `layout-land/audio_player.xml` / `player_hud.xml` (`VLCBookmarkMarkers`, `VLCBookmarkRow`; compose-68e; timeline markers plus the shared bookmarks panel used by AudioPlayer and VideoPlayerOverlayDelegate)
  - Shared playback A-B repeat include: `application/vlc-android/res/layout/ab_repeat_controls.xml` + `application/vlc-android/src/org/videolan/vlc/gui/view/AbRepeatControlsView.kt` (`VLCAbRepeatControls`, `VLCAbRepeatChipIcon`, `VLCAbRepeatAddMarkerButton`; compose-68e; used by AudioPlayer and VideoPlayerOverlayDelegate through the shared include)
  - Shared playback option panel: `application/vlc-android/src/org/videolan/vlc/gui/helpers/PlayerOptionsDelegate.kt` + `PlayerOptionsPanelView.kt` + `player_options.xml` (`VLCPlayerOptionItem`; compose-68e; used by audio/video/player remote option panels through PlayerOptionsDelegate)
  - Preferences root host: `application/vlc-android/src/org/videolan/vlc/gui/preferences/PreferencesActivity.kt` + `PreferencesRootScreen.kt` (bd compose-qeh; removes the phone root preference Fragment while preserving existing subpage preference fragments as follow-up work)
  - Crown jewel cross-cutting Lab (this milestone): `application/vlc-android/src/org/videolan/vlc/gui/ComposeInteropLabActivity.kt` + `compose_interop_lab.xml`
  - All leaves + interop + theme: `application/compose/src/main/java/org/videolan/vlc/compose/{components,interop,theme}/*`
  - Richer mocks derived from the Lab: `application/compose/src/main/java/org/videolan/vlc/compose/PreviewUtils.kt`
  - Manifest registration (additive): `application/vlc-android/AndroidManifest.xml`
  - Tracking: bd issue `compose-iju` (labeled `compose-2l4.1.8`)

### The "Every Host Must Have Green Compile Gate Evidence" Policy

**Enforced starting with compose-2l4.1.8**:

> Every interop host (NetworkServerDialog, DebugLogActivity, MediaInfoAdapter/InfoActivity, ComposeInteropLabActivity, OnboardingWelcomeFragment, and all future ones) **must** have documented evidence that it compiles cleanly against the current `:application:compose` leaves.

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
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after replacing the audio-player `FragmentContainerView` shell with a plain `FrameLayout` host and constructing `AudioPlayer` as a Fragment-free bottom-sheet controller from `AudioPlayerContainerActivity`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the phone root preference Fragment screen with `PreferencesRootScreen` hosted by `PreferencesActivity`, preserving root setting toggles/list dialogs and endpoint routing into existing subpages.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after moving `AudioAlbumTracksAdapter` active album track rows to `ComposeView` + `VLCBrowserItemRow`, preserving track-number/current-track/subtitle/action behavior while keeping `audio_album_track_item.xml` buildable as rollback.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 9s` after deleting `playlist_item.xml` and `PlaylistItemView.kt`, moving the app-side row artwork/action bridge into `AudioPlaylistMediaItem.kt`, and creating the audio playlist tips fake rows programmatically as Compose widgets.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after deleting `playlist_item.xml` and `PlaylistItemView.kt`, moving the app-side row artwork/action bridge into `AudioPlaylistMediaItem.kt`, and creating the audio playlist tips fake rows programmatically as Compose widgets.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the video-player overlay `video_playlist` RecyclerView host with the shared VLCComposeView-backed `AudioPlaylistQueue` LazyColumn, moving `VideoPlayerActivity`/`VideoPlayerOverlayDelegate` off the former shared `PlaylistAdapter` path and deleting `PlaylistAdapter.kt`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 17s` after replacing the audio-player `songs_list` RecyclerView host with a VLCComposeView-backed `AudioPlaylistQueue` LazyColumn, moving AudioPlayer off `PlaylistAdapter`/`ItemTouchHelper` while preserving playlist row actions, context menu, stop-after-this, swipe removal, and inline tablet move/delete controls in Compose.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 14s` after replacing `playlist_item.xml` DataBinding content with `PlaylistItemView` + `VLCAudioPlaylistItem`, including the audio playlist tips fake rows, while keeping the existing RecyclerView swipe/drag host for the next list-surface migration.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 17s` after replacing the shared `bookmarks.xml` toolbar/RecyclerView/empty state/bottom controls, `bookmark_item.xml` DataBinding rows, and the bookmark overflow menu XML with `BookmarksPanelView` + `VLCBookmarkRow`, keeping the existing audio/video bookmark ViewStub hosts and marker container callbacks.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 18s` after replacing the shared `player_options.xml` `BrowseFrameLayout`/`RecyclerView` shell and row bridge with `PlayerOptionsPanelView`, keeping the existing player option ViewStub hosts and dynamic option callbacks.
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
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 13s` after replacing the shared A-B repeat add-marker button in `ab_repeat_controls.xml` with `VLCAbRepeatAddMarkerButton` via `AbRepeatAddMarkerButtonView`, with audio/video hosts driven by the callback-based `manageAbRepeatStep` overload.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 21s` after replacing the shared A-B repeat add-marker chip icon in `ab_repeat_controls.xml` with `VLCAbRepeatChipIcon` via `AbRepeatChipIconView`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 22s` after replacing the audio-player elapsed/length timeline labels with `VLCAudioTimelineTimeLabel`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the landscape audio-player track info title/subtitle/detail text with `VLCAudioTrackInfoText`.
- 2026-06-01: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 20s` after replacing the audio-player A-B repeat timeline markers with `VLCAudioAbRepeatMarker`.
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
- All current Wave 0/1 hosts (NetworkServerDialog, DebugLogActivity, MediaInfoAdapter/InfoActivity, ComposeInteropLabActivity, OnboardingWelcomeFragment, OTPCodeActivity) participate in the app Kotlin gate.

Future agents: re-run the gate after touching any leaf or host, append the SUCCESS tail + timestamp to bd, and update this section.

---

**Part of the phase-0-compose-bootstrap effort.**  
See parent tracking issue and reconnaissance notes for the overall 80% migration strategy.
