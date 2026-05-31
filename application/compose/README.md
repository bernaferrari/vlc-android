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
- Full Compose Activity screens:
  - `VLCOTPCodeScreen` hosted by `OTPCodeActivity` (former OTP Fragment/layout removed)
  - `VLCBetaWelcomeScreen` hosted by `BetaWelcomeActivity` (former DataBinding XML layout removed)
  - `VLCAuthorsScreen` hosted by `AuthorsActivity` (former DataBinding RecyclerView/list XML removed)
  - `VLCLibrariesScreen` hosted by `LibrariesActivity` (former DataBinding RecyclerView/list XML removed; license detail is Compose in this screen)
  - `VLCAboutScreen` hosted by `AboutActivity` (former About Fragment/XML and About license/version bottom-sheet Fragment paths removed)
  - `VLCFeedbackScreen` hosted by `FeedbackActivity` (former DataBinding XML layout removed)
  - `VLCPinCodeScreen` hosted by `PinCodeActivity` (former DataBinding PIN keypad XML layout removed)
  - `VLCSearchScreen` hosted by `SearchActivity` (former DataBinding Activity and result row XML layouts removed)
  - `VLCEqualizerSettingsScreen` hosted by `EqualizerSettingsActivity` (former DataBinding Activity/list row XML layouts and menu XML removed)
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
- 2026-05-31: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:vlc-android:compileDebugKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 1m 24s` after the worktree was made tolerant of absent local `libvlcjni/libvlc` by falling back to the published `org.videolan.android:libvlc-all` artifact.
- 2026-05-31: `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk gradle :application:compose:build --no-daemon --console=plain` completed with `BUILD SUCCESSFUL in 1m 37s`.
- All current Wave 0/1 hosts (NetworkServerDialog, DebugLogActivity, MediaInfoAdapter/InfoActivity, ComposeInteropLabActivity, OnboardingWelcomeFragment, OTPCodeActivity) participate in the app Kotlin gate.

Future agents: re-run the gate after touching any leaf or host, append the SUCCESS tail + timestamp to bd, and update this section.

---

**Part of the phase-0-compose-bootstrap effort.**  
See parent tracking issue and reconnaissance notes for the overall 80% migration strategy.
