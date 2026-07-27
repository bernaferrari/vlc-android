# VLC KMP / iOS app

The iOS app hosts the same Compose Multiplatform VLC shell as Android, with
Swift limited to iOS integration islands: MobileVLCKit, document/photo intake,
and the SwiftUI application lifecycle.

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
├── ios/                             # Xcode host for the shared shell
│   ├── App/AppDelegate.swift        # SwiftUI lifecycle + native integrations
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
| `:shared:linkDebugFrameworkIosSimulatorArm64` | simulator Debug framework | Xcode simulator builds |
| `:shared:linkReleaseFrameworkIosSimulatorArm64` | simulator Release framework | simulator release checks |
| `:shared:linkDebugFrameworkIosArm64` | device Debug framework | Xcode device builds |
| `:shared:linkReleaseFrameworkIosArm64` | device Release framework | Xcode archives |

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

## iOS integration

`ios/App/AppDelegate.swift` starts the iOS Koin module, installs the
MobileVLCKit playback backend, and asks `MediaImporter` to reconcile Documents
and cached imports. `MainViewController()` is the Compose root used by SwiftUI;
it receives a native video surface only where a video needs one.

### Setup

```bash
cd ios/
./setup.sh
# Follow script output for Xcode + framework link steps
```

### Framework selection

`ios/project.yml` is the source of truth for the checked-in Xcode project. It
selects `iosSimulatorArm64` for `iphonesimulator` and `iosArm64` for
`iphoneos`, then selects `debugFramework` or `releaseFramework` from the Xcode
configuration. Its pre-build phase invokes the matching Gradle task. Run
`xcodegen generate` after changing the manifest.

## Design notes

- **Compose Multiplatform in `:shared`**: phone UI components already live here; Android hosts them through activities/interop. iOS UI strategy (CMP vs SwiftUI) is still open.
- **FlagSet**: custom bitmask instead of JVM-only `EnumSet`.
- **DataStore**: Flow/coroutine prefs for common code; minSdk 23 on Android because of androidx.datastore 1.2.x.


## VLCKit integration

1. `IosKoinBootstrap.start()` from app launch.
2. `VlcKitBackend` (`ios/App/VlcKitBackend.swift`) is attached at launch:
   `IosPlaybackService.shared.setBackend(backend: VlcKitBackend.shared)`.
3. Import media through the Files/Photos controls; the persistent catalog is
   reconciled on launch and refresh.
4. Control playback through `VlcSharedApi.playFirst` / pause / resume.

## Compose Multiplatform shell

iOS now hosts the **same** `VlcSharedApp` as Android (`Library` / `Player` / `Settings`):

- Kotlin entry: `MainViewController()` in `shared/.../compose/app/MainViewController.kt`
- Swift host: `ComposeSharedRoot` in `ios/App/AppDelegate.swift`
- ViewModels: `LibraryViewModel`, `PlayerViewModel`, `SettingsViewModel` (commonMain)
- A first run stays empty until the user imports media; Documents are scanned
  and reconciled without demo injection.
- `IosPlaybackService.shared.setBackend(VlcKitBackend.shared)` installs real
  decode when MobileVLCKit resolves.

Android lab entry: **More → VLC Shared** (`SharedAppActivity`).

## Production iOS path (this tree)

### One-shot setup
```bash
./ios/setup.sh          # builds VLCShared frameworks + xcodegen
open ios/VLC-iOS.xcodeproj
```

### What is wired
1. **MobileVLCKit (SPM)** — `ios/project.yml` packages.MobileVLCKit
2. **Real decode** — `AppDelegate` → `IosPlaybackService.shared.setBackend(VlcKitBackend.shared)`
3. **Drawable** — the shared player route attaches its native `UIView` as the
   VLCKit drawable and detaches it on disposal
4. **Library intake**
   - Documents/Caches recursive scan (`MediaImporter.rescanLocalFolders`)
   - Files picker (multi-select audiovisual types)
   - Photos picker (PHPicker videos)
   - "Open in VLC" URL handoff
5. **UI** — same `VlcSharedApp` CMP shell as Android (`MainViewController`)

### Verify decode
- First Xcode open resolves SPM (network).
- Run on Simulator; import a file; play.
- A resolved MobileVLCKit package is required for real decode. If it cannot be
  imported, playback is unavailable rather than silently presenting demo media.
