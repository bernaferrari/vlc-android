# Plan 001: Render native video inside the shared player

> **Executor instructions**: Follow every step and verification gate. If a STOP condition occurs, report it rather than inventing a platform bridge.
>
> **Drift check**: `git diff --stat 098874d3f..HEAD -- shared/src/commonMain/kotlin/org/videolan/vlc/compose/app/VlcShellDestinations.kt shared/src/commonMain/kotlin/org/videolan/vlc/compose/player/VideoSurfaceHud.kt shared/src/androidMain shared/src/iosMain application/vlc-android/src/org/videolan/vlc/gui/video ios/App`

## Status

- **Priority**: P0
- **Effort**: L
- **Risk**: HIGH — decoder drawable ownership and Activity/ViewController lifecycle are easy to regress.
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `098874d3f`, 2026-07-26
- **Beads**: `compose-3fnh`

## Why this matters

The default shared `PlayerRoute` presents controls over a placeholder rather than a decoder surface. Android retains a separate native `VideoPlayerActivity` path; iOS attaches VLCKit to the entire Compose host. A shared player is not credible until both targets render video inside the same surface slot that the common HUD owns.

## Current state

- `shared/src/commonMain/kotlin/org/videolan/vlc/compose/player/VideoSurfaceHud.kt:45-107` already defines the correct common seam: `VideoSurfaceWithHud(... surface: @Composable BoxScope.() -> Unit)`.
- `shared/src/commonMain/kotlin/org/videolan/vlc/compose/app/VlcShellDestinations.kt:239-273` always passes a black box with `MusicNote` into that seam.
- `application/vlc-android/src/org/videolan/vlc/gui/video/ComposeVideoSurface.kt` is the existing Android `AndroidView`/libVLC pattern. Reuse its surface lifecycle rather than creating a second raw `SurfaceView` implementation.
- `ios/App/AppDelegate.swift:137-149` creates the shared root then calls `VlcKitBackend.shared.attachDrawable(vc.view)`, which is too broad. `ios/App/VlcKitBackend.swift` owns the existing `attachDrawable(_:)` API.
- The project uses common Compose UI with platform-native decoder islands. Match the explicit Koin/host-boundary pattern in `shared/src/commonMain/kotlin/org/videolan/vlc/compose/app/ShellHostCallbacks.kt`.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Shared tests | `./gradlew :shared:allTests --no-configuration-cache --console=plain` | exit 0 |
| Android compile | `./gradlew :application:vlc-android:compileDebugKotlin --no-configuration-cache --console=plain` | exit 0 |
| iOS framework | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --no-configuration-cache --console=plain` | exit 0 |
| iOS build | `xcodebuild -project ios/VLC-iOS.xcodeproj -scheme VLC-iOS -sdk iphonesimulator -configuration Debug build CODE_SIGNING_ALLOWED=NO` | `BUILD SUCCEEDED` |

## Scope

**In scope**: common player destination and surface seam; Android actual/player host; iOS actual/player host; `ios/App/AppDelegate.swift`, `ios/App/VlcKitBackend.swift`; focused tests.

**Out of scope**: codec changes, Android legacy `VideoPlayerActivity` feature removal, PiP/casting/network browsing, replacing the common HUD design.

## Steps

### Step 1: Define a narrow platform video-output contract

Keep `VideoSurfaceWithHud` unchanged as the common HUD. Add an injected/expect platform surface host that renders only when the current `MediaItem` is video and exposes explicit attach/detach lifecycle. The common destination must receive media type and invoke the host in its existing `surface` slot; it must not import Android or UIKit APIs.

**Verify**: `./gradlew :shared:compileKotlinWasmJs :shared:compileKotlinIosSimulatorArm64 --no-configuration-cache --console=plain` exits 0.

### Step 2: Adapt the existing Android native surface

Reuse the attachment/lifecycle behavior from `ComposeVideoSurface.kt`; route the shared player through it only for video. Preserve the legacy `VideoPlayerActivity` path and add a regression test or instrumentation scenario that selecting a video from the shared shell attaches one surface, while selecting audio does not.

**Verify**: Android compile succeeds and a device/emulator plays a local video from the shared Video tab with the common HUD visible.

### Step 3: Build an iOS UIKit surface bridge

Create the drawable `UIView` inside iOS Kotlin/Compose interop and hand it to Swift through an explicit callback/registry established at app launch. Move `attachDrawable(vc.view)` out of `ComposeSharedRoot`; the backend must attach only to the surface view and clear it on disposal. Keep audio playback drawable-free.

**Verify**: iOS simulator build succeeds; a video test stream renders inside the player bounds; navigating back clears `VlcKitBackend.drawableView`.

### Step 4: Add lifecycle regression coverage

Add common tests for the media-type decision and platform-host visibility callbacks. Add the smallest platform smoke test/documented script covering video → back → audio → video and app background/foreground.

**Verify**: all commands in the table pass; no `attachDrawable(vc.view)` remains.

## Done criteria

- [ ] Shared PlayerRoute renders a native video surface for video on Android and iOS.
- [ ] Audio remains an artwork/player surface without a decoder drawable.
- [ ] Exactly one drawable is attached and it is detached when the player route leaves composition.
- [ ] Shared tests, Android compile, iOS framework link, and iOS simulator build pass.

## STOP conditions

- Compose iOS interop cannot safely provide a `UIView` to Swift without a new KMP-visible callback boundary.
- Android playback service requires `VideoPlayerActivity` ownership for a decoder surface.
- Any proposed design makes commonMain import Android/UIKit classes.

## Maintenance notes

Review surface disposal and rotation/background behavior especially closely. PiP, renderer discovery, subtitle controls, and gesture parity remain separate capability work.
