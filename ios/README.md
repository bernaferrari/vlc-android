# VLC KMP / iOS skeleton

Kotlin Multiplatform shared code for VLC Android (and a future iOS app), plus a minimal SwiftUI host.

## Layout

```
vlc-android/
├── shared/                          # KMP shared module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/              # Pure Kotlin + Compose Multiplatform UI
│       │   ├── kotlin/org/videolan/tools/     # Prefs keys, VlcPreferences (DataStore), utils
│       │   ├── kotlin/org/videolan/vlc/
│       │   │   ├── model/, player/, repository/, platform/, util/
│       │   │   ├── compose/components/, compose/theme/, compose/interop/
│       │   │   ├── app/   # VlcKoin, VlcModule, SharedModule, VlcSharedApi
│       │   │   └── remoteaccessserver/        # Protocol types
│       │   └── composeResources/
│       ├── commonJvmMain/           # JVM-shared (Android + Desktop)
│       ├── androidMain/             # Android DataStore factory, platform actuals
│       ├── jvmMain/                 # Desktop JVM DataStore factory
│       └── iosMain/                 # iOS DataStore factory, platform actuals
│
├── application/                     # Android app modules
│   ├── compose/                     # Android-only VLCComposeView interop shim
│   ├── vlc-android/
│   │   └── …/kmp/                   # AndroidMediaRepository, AndroidPlaybackService, VlcKmpInitializer
│   └── …
│
├── ios/                             # Xcode / SwiftUI skeleton
│   ├── App/AppDelegate.swift        # VLCiOSApp + VlcSharedApi demo
│   ├── setup.sh
│   └── README.md                    # this file
│
└── settings.gradle
```

## Build targets (`:shared`)

| Target | Purpose | Notes |
|--------|---------|--------|
| `:shared:compileDebugKotlinAndroid` | Android library | Primary CI path |
| `:shared:compileKotlinJvm` | Desktop JVM | Okio DataStore |
| `:shared:compileKotlinIosArm64` | iOS device | |
| `:shared:compileKotlinIosSimulatorArm64` | iOS simulator (Apple Silicon) | |
| `:shared:linkDebugFrameworkIosSimulatorArm64` | `VLCShared.framework` | Consumed by `ios/` |

There is **no** `iosX64` target in the current `shared/build.gradle.kts` (Apple Silicon–only simulators).

## What’s shared

### Domain / contracts (`commonMain`)
- Models: `MediaItem`, `Playlist`, `Progress`, playback enums
- `MediaRepository`, `PlaylistRepository`, `HistoryRepository`, `PlaybackService` contracts
- `VlcPreferences` + `PreferenceKeys` (DataStore, not Android `SharedPreferences`)
- Remote-access protocol types (`ServerStatus`, websocket messages, …)
- Compose Multiplatform UI components and theme (phone UI source of truth)

### DI / Swift API
- **Koin** via `VlcKoin` / platform `VlcModule` — not a `VlcAppContainer` type
- `VlcSharedApi` — small Swift-facing façade (`platformInfo()`, `getMediaCount`, preference helpers)
- App must start Koin before use (`VlcKmpInitializer` on Android; iOS still needs a real `startKoin` hook)

### Preferences
Platform factories:
- Android: `AndroidVlcDataStoreFactory`
- JVM: `JvmVlcDataStoreFactory`
- iOS: `IosVlcDataStoreFactory`

Legacy Android `org.videolan.tools.Settings` (SharedPreferences) still powers many hot paths; migration is incremental (`compose-88fh`).

## Android integration

- Adapters under `application/vlc-android/src/org/videolan/vlc/kmp/`
- UI components are **not** in `:application:compose` (that module is only `VLCComposeView`)
- Project **minSdk 23** (DataStore 1.2.x / Compose); see root README

## iOS integration (current reality)

`ios/App/AppDelegate.swift` is a **skeleton**:

1. Imports `VLCShared` and constructs `VlcSharedApi()`
2. Shows `platformInfo()` / placeholder media counts in SwiftUI
3. Comments still mention a future `VlcAppContainer` — **that type does not exist**; use Koin + `VlcSharedApi`
4. No VLCKit medialibrary/player wiring yet
5. CMP UI lives in `:shared` commonMain; the iOS app does **not** host those composables yet (SwiftUI shell only)

### Setup

```bash
cd ios/
./setup.sh
# Follow script output for Xcode + framework link steps
```

### Next steps (iOS)
1. Start Koin from Swift with iOS modules + real repository/player adapters
2. Wire VLCKit medialibrary → `MediaRepository`
3. Wire VLCKit `VLCMediaPlayer` → `PlaybackService`
4. Either embed Compose Multiplatform UI from `VLCShared` or keep SwiftUI and call shared domain APIs only
5. Library browser + playlist management

## Design notes

- **Compose Multiplatform in `:shared`**: phone UI components already live here; Android hosts them through activities/interop. iOS UI strategy (CMP vs SwiftUI) is still open.
- **FlagSet**: custom bitmask instead of JVM-only `EnumSet`.
- **DataStore**: Flow/coroutine prefs for common code; minSdk 23 on Android because of androidx.datastore 1.2.x.
