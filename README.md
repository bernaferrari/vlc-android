# VLC KMP

<p align="center">
  <strong>A modern, local-first media player built with Kotlin Multiplatform and Compose.</strong><br>
  One adaptive product experience for Android and iOS, with a browser demo powered by the same shared UI.
</p>

VLC KMP is a new multiplatform media application built around the VLC ecosystem's native playback engines. It is not the legacy VLC Android application: the product UI, navigation, state, preferences, and feature contracts live in shared Kotlin. Android and iOS provide focused native integrations for the capabilities that must remain platform-specific.

## Highlights

- **Shared product, native playback:** The library, playlists, history, favorites, player controls, settings, adaptive navigation, and Material 3 design system are shared Compose Multiplatform code.
- **Real local media:** Import and organise your own audio and video instead of relying on demo content.
- **Native where it matters:** Android uses LibVLC; iOS uses upstream VLCKit; the Web demo uses the browser's media engine for formats it supports.
- **Adaptive by default:** The same Navigation 3 shell scales from compact phones to larger screens.
- **Local-first:** Media catalogues and preferences stay on the device. There is no account or product backend.
- **Capability-aware:** The shared UI only offers controls backed by the current target, rather than presenting platform-specific actions as dead ends.

## Targets

| Target | Status | Playback and media intake |
| --- | --- | --- |
| Android | Primary native app | LibVLC, media-library scan, Storage Access Framework import, background media session, PiP, renderer and network browsing |
| iOS | Native host for the shared app | VLCKit, Files and Photos import, media session, PiP, renderer and local-network discovery |
| Web / Wasm | Shared UI demo | Browser file import and browser-supported audio/video playback |
| Desktop JVM | Shared code target | No production media engine yet |

The complete target contract is maintained in [shared/CAPABILITIES.md](shared/CAPABILITIES.md). Codec, container, browser-policy, and device support are ultimately determined by the native engine or browser on that target.

## Architecture

```text
shared/                         Shared Kotlin Multiplatform product
  commonMain/                   Domain, repositories, Koin, UI, Navigation 3, Material 3
  androidMain/ iosMain/         Small platform actuals and storage bridges

application/                    Android host and Android-only integrations
  app/                          Android application packaging
  vlc-android/                  LibVLC, media service, permissions, system integration
  remote-access-server/         Authenticated local remote-access server

ios/                            SwiftUI host and VLCKit integration
webApp/                         Kotlin/Wasm browser host for the shared shell
medialibrary/                   Android media-library integration
```

The shared shell is the source of truth. Native code is intentionally limited to media decoding, import/pickers, OS media sessions, permissions, system surfaces, and other platform APIs.

## Getting started

### Prerequisites

- JDK 17 or newer
- Android Studio with Android SDK Platform 37 for Android development
- Xcode and XcodeGen for iOS development

The project uses the checked-in Gradle wrapper. Gradle provisions the Node, Yarn, and Binaryen tools required by the Wasm target; a separate Node installation is not required for its Gradle tasks.

### Android

```bash
# Build the debug app
./gradlew :application:app:assembleDebug --no-daemon --console=plain

# Install on a connected Android device or emulator
./gradlew :application:app:installDebug --no-daemon --console=plain
```

Android requires API 26 or newer. The packaged store-oriented `vlcBundle` variant has its own API 30 floor.

### Web demo

```bash
# Run the shared app in a local browser development server
./gradlew :webApp:wasmJsBrowserDevelopmentRun --no-daemon --console=plain

# Produce the optimized browser bundle
./gradlew :webApp:wasmJsBrowserProductionWebpack --no-daemon --console=plain
```

The browser target is useful for exploring the shared UI and importing browser-playable files. It is intentionally not a replacement for the Android or iOS native media engines.

### iOS

```bash
./ios/setup.sh
open ios/VLC-iOS.xcodeproj
```

The setup script builds the Kotlin framework and generates the Xcode project. For repeatable simulator and archive checks:

```bash
./ios/verify.sh simulator
./ios/verify.sh archive
```

See [ios/README.md](ios/README.md) for the host layout, VLCKit integration, and framework details.

## Verification

Run the checks appropriate to the code you change:

```bash
# Shared Kotlin tests and optimized Wasm bundle
./gradlew :shared:allTests :webApp:wasmJsBrowserProductionWebpack --no-daemon --console=plain

# Android app package
./gradlew :application:app:assembleDebug --no-daemon --console=plain

# iOS host (macOS only)
./ios/verify.sh simulator
```

Automated builds do not replace device testing for hardware decode, imports, seeking, PiP, system media controls, or browser codec support. Use [ios/RELEASE_SMOKE_TEST.md](ios/RELEASE_SMOKE_TEST.md) and the target capability matrix when preparing a release.

## Contributing

Keep product behaviour and UI in `shared/commonMain` whenever a platform API is not required. Add platform bridges only behind a shared contract, expose them through Koin, and update [shared/CAPABILITIES.md](shared/CAPABILITIES.md) when a capability changes.

Before proposing a change, run the narrowest relevant Gradle task and keep Android and iOS behaviour aligned. Use the browser target as a quick shared-UI check, not as proof of native playback behaviour.

## License and attribution

This repository is licensed under the [GNU GPL v2 or later](COPYING). It builds on the VLC ecosystem and uses VLC native engines where available; their source and licence terms remain in their respective modules and dependencies.
