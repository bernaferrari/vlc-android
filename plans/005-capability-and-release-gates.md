# Plan 005: Enforce capability parity and release verification

> **Executor instructions**: Do not claim feature parity from a compilation result. Every advertised capability needs a target-specific end-to-end check.
>
> **Drift check**: `git diff --stat 098874d3f..HEAD -- shared/src/iosMain/kotlin/org/videolan/vlc/app/VlcModule.kt shared/src/commonMain/kotlin/org/videolan/vlc/platform buildsystem/gitlab ios application`

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED — CI additions can expose previously hidden environment assumptions.
- **Depends on**: plans 001–004
- **Category**: tests
- **Planned at**: commit `098874d3f`, 2026-07-26
- **Beads**: `compose-q2ne`, `compose-9mid`, `compose-nboc`, `compose-icjd`

## Why this matters

Android VLC supports a much wider native feature set than the current iOS/Wasm host. iOS explicitly installs `NoOpPipController` and `NoOpRendererBridge`, and its repository exposes only a Documents root. There are shared unit tests and Android instrumentation tests, but no iOS XCTest files or reliable release artifact gate.

## Current state

- `shared/src/iosMain/kotlin/org/videolan/vlc/app/VlcModule.kt:35-36` installs no-op PiP and renderer bridges.
- `IosMediaLibrary.kt:229-250` creates/scans a Documents root; it does not model network sources.
- `shared/src/commonTest` has nine focused Kotlin tests; `ios/` has no XCTest target/files.
- `buildsystem/gitlab/.gitlab-ci.yml` contains Android assembly jobs but no iOS archive/capability matrix gate.
- Existing Beads `compose-nboc` tracks Android lint/Kotlin metadata compatibility; `compose-icjd` tracks remaining shared-shell localization.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Shared suite | `./gradlew :shared:allTests --no-configuration-cache --console=plain` | exit 0 |
| Android package | `./gradlew :application:app:assembleDebug --no-configuration-cache --console=plain` | APK produced |
| iOS simulator | `xcodebuild -project ios/VLC-iOS.xcodeproj -scheme VLC-iOS -sdk iphonesimulator -configuration Debug test CODE_SIGNING_ALLOWED=NO` | `TEST SUCCEEDED` |
| iOS archive | `xcodebuild -project ios/VLC-iOS.xcodeproj -scheme VLC-iOS -sdk iphoneos -configuration Release archive -archivePath "$PWD/build/VLC-iOS.xcarchive" CODE_SIGNING_ALLOWED=NO` | `ARCHIVE SUCCEEDED` |

## Scope

**In scope**: capability matrix/documentation, availability gating in shared UI, iOS test target, CI/release scripts, local device smoke checklist, lint compatibility follow-up.

**Out of scope**: silently pretending iOS supports every Android LibVLC feature; implementing every unsupported feature without an approved product decision.

## Steps

### Step 1: Publish a target capability matrix

List Android, iOS, and Wasm support for local import, stream playback, video surface, audio background controls, playlists/history, network browsing, subtitles, PiP, renderer/cast, remote access, and settings. Mark each as supported, unsupported-hidden, or deferred; cite its native bridge/verification.

**Verify**: review confirms no UI action is advertised for a no-op capability.

### Step 2: Gate shared UI from typed capabilities

Replace implicit no-op behavior with an injectable capability model. Hide unsupported iOS/Wasm paths or provide an intentional disabled/explainer state; do not expose controls that silently fail.

**Verify**: shared unit tests cover Android/iOS/Wasm availability decisions.

### Step 3: Add build and flow gates

Add iOS XCTest/UI coverage for import, persistence, video/audio playback, navigation/back, and background/foreground state. Add Android shared-shell integration coverage. Add CI jobs for Android packaging, iOS simulator tests, and unsigned device archive; resolve `compose-nboc` so Android lint is a reliable gate.

**Verify**: all commands above run from a clean checkout in CI.

### Step 4: Run release smoke tests on real hardware

Version a checklist covering iPhone/iPad and compact/tablet Android: local import, stream, video route, audio background/lock screen, relaunch, adaptive rail, external share/open, and every supported native capability. Record model/OS/result without credentials or personal media paths.

**Verify**: release candidate cannot be marked ready without completed results for each supported capability.

## Done criteria

- [ ] Capability matrix is reviewed and reflected in UI availability.
- [ ] No visible supported action reaches a no-op bridge.
- [ ] CI produces Android and iOS artifacts and executes defined tests.
- [ ] Physical-device release checklist passes for supported features.

## STOP conditions

- Product owners require an iOS feature that MobileVLCKit/iOS cannot support.
- CI lacks macOS/iOS runners or signing/archive access.
- A capability requires credentials or a privacy/security design not yet approved.

## Maintenance notes

Update the matrix when a native bridge changes. The honest product is a smaller set of dependable features, never a larger set of buttons that do nothing.
