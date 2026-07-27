# Plan 004: Make the shared shell own Android and iOS chrome

> **Executor instructions**: Do not remove Android system integrations; move only visual/navigation ownership that duplicates the common shell.
>
> **Drift check**: `git diff --stat 098874d3f..HEAD -- application/vlc-android/src/org/videolan/vlc/gui/helpers/Navigator.kt application/vlc-android/src/org/videolan/vlc/gui/PhoneActivityShell.kt shared/src/commonMain/kotlin/org/videolan/vlc/compose/app/VlcMainShell.kt shared/src/iosMain/kotlin/org/videolan/vlc/compose/app/MainViewController.kt`

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: HIGH — Android navigation, miniplayer, permissions, and video hand-off are mature paths.
- **Depends on**: `plans/001-native-video-surface.md`
- **Category**: tech-debt
- **Planned at**: commit `098874d3f`, 2026-07-26
- **Beads**: `compose-vu88`

## Why this matters

Android enables the shared shell by default but wraps it in a native toolbar, navigation rail/bar, miniplayer, and FAB, while iOS hosts the complete `VlcKoinMainShell`. The result contradicts the product goal that Android and iOS present the same page and multiplies UI work.

## Current state

- `application/vlc-android/src/org/videolan/vlc/gui/helpers/Navigator.kt:92-160` reads `KEY_USE_SHARED_MAIN_SHELL`, adds `VlcKoinMainShell` into `content_placeholder`, and passes `showBottomBar = false` because native chrome stays active.
- `application/vlc-android/src/org/videolan/vlc/gui/PhoneActivityShell.kt:53-125` builds Android toolbar/rail/bottom navigation/FAB hosts.
- `shared/src/commonMain/kotlin/org/videolan/vlc/compose/app/VlcMainShell.kt` already owns `NavigationSuiteScaffold`, Nav3 stack, mini-bar, settings route, and adaptive rail.
- `shared/src/iosMain/kotlin/org/videolan/vlc/compose/app/MainViewController.kt:27-29` hosts the same common shell directly.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Shared tests | `./gradlew :shared:allTests --no-configuration-cache --console=plain` | exit 0 |
| Android compile | `./gradlew :application:vlc-android:compileDebugKotlin --no-configuration-cache --console=plain` | exit 0 |
| Android package | `./gradlew :application:app:assembleDebug --no-configuration-cache --console=plain` | APK produced |
| Wasm regression | `./gradlew :webApp:wasmJsBrowserProductionWebpack --no-configuration-cache --console=plain` | exit 0 |

## Scope

**In scope**: shared top-level chrome/navigation, Android shell host integration, controlled migration flag removal once safe, tests/screenshots for compact and expanded layouts.

**Out of scope**: Android TV, widget UI, legacy standalone VideoPlayerActivity internals, non-phone extension routes.

## Steps

### Step 1: Inventory host-only behavior

For every Android outer-shell element, classify it as common UI, native integration, or obsolete duplicate. Preserve permissions, system bars, scan progress, native media hand-off, and callbacks through a typed host API. Record any missing common representation before removal.

**Verify**: a written mapping exists in the PR and no outer component is removed without a common or host-owned replacement.

### Step 2: Promote shared chrome incrementally

Enable common navigation suite, mini-player, player entry, and settings routing in Android; remove corresponding native visual wrappers only after event parity is proven. Keep the navigation flag only during migration and default it to the common path.

**Verify**: Android compact and tablet paths show the same common tabs/rail and route stack as iOS; hardware/system back works.

### Step 3: Move Android-only behavior behind callbacks

Route import, permission, sharing, subtitles, ringtone, OTG, remote access, and legacy video hand-off through `ShellHostCallbacks`/new explicit host contracts. Do not let common UI import Android classes.

**Verify**: Android compile and smoke flows for each available context action pass; iOS still compiles without Android dependencies.

### Step 4: Remove duplicated visual state

After parity tests, remove unused native navigation chrome and stale feature-flag paths. Keep only native islands with a documented owner.

**Verify**: `rg "KEY_USE_SHARED_MAIN_SHELL|sharedShellAttached" application` returns only intentional migration compatibility code, or no matches.

## Done criteria

- [ ] Android and iOS primary shells use the same common Compose hierarchy.
- [ ] Android retains all platform integrations via callbacks/native islands.
- [ ] Compact and rail layouts have parity screenshots/tests.
- [ ] Shared, Android package, and Wasm regression gates pass.

## STOP conditions

- A native outer element has no equivalent or host callback for a required user flow.
- Video hand-off is not safe until plan 001 is complete.
- Removal would affect TV, widgets, or extensions outside this plan's scope.

## Maintenance notes

The shell should stay a platform-agnostic renderer. New platform integrations belong in typed host callbacks, not additional Android-only chrome.
