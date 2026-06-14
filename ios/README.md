# VLC KMP Architecture

This document describes the Kotlin Multiplatform (KMP) architecture for the VLC Android/iOS project.

## Module Structure

```
vlc-android/
├── shared/                          # KMP shared module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/              # Pure Kotlin — compiles everywhere
│       │   ├── kotlin/org/videolan/tools/
│       │   │   ├── PreferenceKeys.kt    # ~150 preference key constants
│       │   │   ├── VlcPreferences.kt    # DataStore<Preferences> wrapper
│       │   │   ├── Strings.kt           # String utils (expect/actual)
│       │   │   ├── PathUtils.kt         # Path sanitization
│       │   │   └── Helpers.kt           # coerceInOrDefault
│       │   ├── kotlin/org/videolan/vlc/
│       │   │   ├── model/
│       │   │   │   └── MediaModels.kt       # MediaItem, Playlist, Progress, enums
│       │   │   ├── player/
│       │   │   │   └── PlaybackContract.kt  # PlaybackService interface
│       │   │   ├── repository/
│       │   │   │   └── Repositories.kt      # MediaRepository, PlaylistRepository
│       │   │   ├── platform/
│       │   │   │   └── Platform.kt          # Platform info, VlcLogger (expect/actual)
│       │   │   ├── util/
│       │   │   │   ├── FlagSet.kt           # Bitmask flag set
│       │   │   │   ├── PlaybackAction.kt    # Playback capabilities enum
│       │   │   │   └── ContextOption.kt     # Context menu flags
│       │   │   ├── remoteaccessserver/
│       │   │   │   ├── ServerStatus.kt      # Remote access server status enum
│       │   │   │   └── websockets/WSProtocol.kt # WebSocket protocol types
│       │   │   └── app/
│       │   │       ├── VlcAppContainer.kt   # DI container
│       │   │       └── VlcSharedApi.kt      # Swift-facing API
│       │
│       ├── commonJvmMain/           # JVM-shared (Android + Desktop)
│       │   └── kotlin/org/videolan/tools/
│       │       ├── SingletonHolder.kt
│       │       ├── CloseableUtils.kt
│       │       ├── CoroutineContextProvider.kt
│       │       ├── DependencyProvider.kt
│       │       ├── IOScopedObject.kt
│       │       ├── Logcat.kt
│       │       └── StringsJvm.kt        # JVM actual for Strings expect
│       │
│       ├── androidMain/             # Android-specific
│       │   ├── kotlin/org/videolan/tools/
│       │   │   └── AndroidVlcDataStoreFactory.kt
│       │   └── kotlin/org/videolan/vlc/platform/
│       │       └── Platform.kt          # Android actual
│       │
│       ├── jvmMain/                 # Desktop JVM-specific
│       │   └── kotlin/org/videolan/tools/
│       │       ├── JvmVlcDataStoreFactory.kt
│       │       └── Platform.kt (in vlc.platform)
│       │
│       └── iosMain/                 # iOS-specific
│           ├── kotlin/org/videolan/tools/
│           │   ├── StringsIos.kt        # iOS actual for Strings expect
│           │   └── IosVlcDataStoreFactory.kt
│           └── kotlin/org/videolan/vlc/platform/
│               └── Platform.kt          # iOS actual
│
├── application/                     # Android app modules (unchanged)
│   ├── tools/                       # Depends on :shared (api)
│   ├── resources/
│   ├── vlc-android/
│   │   └── src/org/videolan/vlc/kmp/  # KMP integration adapters
│   │       ├── AndroidMediaRepository.kt   # MediaRepository impl
│   │       ├── AndroidPlaybackService.kt   # PlaybackService impl
│   │       └── VlcKmpInitializer.kt        # DI container setup
│   └── ...
│
├── ios/                             # iOS Xcode project (skeleton)
│   ├── App/
│   │   └── AppDelegate.swift        # SwiftUI app consuming VLCShared
│   ├── setup.sh                     # Project generation script
│   └── README.md
│
└── settings.gradle
```

## Build Targets

| Target | Purpose | Status |
|--------|---------|--------|
| `:shared:compileDebugKotlinAndroid` | Android library | Working |
| `:shared:compileKotlinJvm` | Desktop JVM | Working |
| `:shared:compileKotlinIosArm64` | iOS device (arm64) | Working |
| `:shared:compileKotlinIosX64` | iOS simulator (x86) | Working |
| `:shared:compileKotlinIosSimulatorArm64` | iOS simulator (Apple Silicon) | Working |
| `:shared:linkDebugFrameworkIosSimulatorArm64` | VLCShared.framework | Working |

## What's Shared

### Domain Models (commonMain)
- `MediaItem` — platform-agnostic media metadata (title, uri, type, duration, artist, etc.)
- `Playlist` — playlist with shuffle/repeat/next/previous logic
- `Progress` — playback position
- `MediaType`, `RepeatMode`, `ResumeStatus`, `ABRepeat`, `DelayValues` — enums and value types

### Contracts/Interfaces (commonMain)
- `MediaRepository` — media library access (Flow-based, no LiveData)
- `PlaylistRepository` — playlist CRUD
- `HistoryRepository` — playback history
- `PlaybackService` — playback control (play, pause, seek, shuffle, repeat, volume)
- `PlaybackState` — sealed class for playback state machine

### Utilities (commonMain)
- `VlcPreferences` — DataStore-backed preference wrapper (replaces SharedPreferences in shared code)
- `PreferenceKeys` — all ~150 VLC preference key constants
- `FlagSet` — platform-agnostic bitmask (replaced java.util.EnumSet)
- `PlaybackAction`, `ContextOption` — enums with flag operations
- String utilities (path sanitization, obfuscation, formatting)

### Protocol Types (commonMain)
- `ServerStatus`, `WSIncomingMessage`, `IncomingMessageType`, `WSAuthTicket` — remote access server protocol

## Preferences: DataStore (not SharedPreferences)

The shared module uses Jetpack DataStore (`datastore-preferences-core`) instead of
SharedPreferences. This gives us:

1. **Flow-first API** — `getBooleanFlow(key, default)` returns a `Flow<T>` for reactive UI
2. **Coroutine-based** — reads/writes are `suspend`, no main-thread blocking
3. **KMP-compatible** — works on Android, JVM, and iOS via the `datastore-core-okio` artifact
4. **Platform-specific storage** — each target provides its own factory:
   - Android: `AndroidVlcDataStoreFactory(context)` — uses `preferencesDataStore` delegate
   - JVM: `JvmVlcDataStoreFactory(directory)` — uses `OkioStorage` + `FileSystem.SYSTEM`
   - iOS: `IosVlcDataStoreFactory()` — stores in app Documents directory

### Migration Path

The existing `Settings` singleton (SharedPreferences-based) continues to work unchanged.
New code should use `VlcApp.container.preferences` instead. Migration of existing call sites
can happen incrementally.

## Android Integration

The `:application:vlc-android` module has KMP adapters in `org.videolan.vlc.kmp`:

- `VlcKmpInitializer.initialize(context, medialibrary, playlistManager)` — call in Application.onCreate()
- `AndroidMediaRepository` — bridges MediaRepository to JNI medialibrary
- `AndroidPlaybackService` — bridges PlaybackService to PlaylistManager

After initialization, shared code accesses everything via `VlcApp.container`.

## iOS Integration

The iOS app skeleton is in `ios/`. It's a SwiftUI app that:

1. Consumes the `VLCShared.framework` (built by the :shared module)
2. Calls `VlcSharedApi().platformInfo()` to verify KMP integration
3. Has placeholders for VLCKit integration (media library + player)

### To set up the iOS project:

```bash
cd ios/
./setup.sh
# Or manually: see ios/setup.sh output for Xcode instructions
```

### Next steps for iOS:
1. Wire VLCKit medialibrary → `MediaRepository` implementation
2. Wire VLCKit `VLCMediaPlayer` → `PlaybackService` implementation
3. Build the UI in SwiftUI using the shared domain models
4. Full playback screen with controls
5. Media library browser
6. Playlist management

## Design Decisions

### Why not Compose Multiplatform for UI?
The existing Android UI is deeply tied to Fragments, Activities, XML layouts, TV/Leanback,
and widgets. Compose Multiplatform is viable for new screens but not a full migration.

### Why custom FlagSet instead of kotlin.enums.EnumSet?
`java.util.EnumSet` is JVM-only. The custom `FlagSet` uses a `Long` bitmask and works on
all platforms with identical semantics.

### Why DataStore instead of SharedPreferences?
SharedPreferences is Android-only and synchronous. DataStore provides:
- Async, Flow-based access from common code
- No main-thread blocking
- KMP support across Android/iOS/JVM
- Transactional writes
```
