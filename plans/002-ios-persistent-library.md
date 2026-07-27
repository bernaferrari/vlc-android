# Plan 002: Persist iOS media, playlists, history, and favorites

> **Executor instructions**: Follow every step and stop if durable storage cannot preserve the existing repository contract without a deliberate migration.
>
> **Drift check**: `git diff --stat 098874d3f..HEAD -- shared/src/iosMain/kotlin/org/videolan/vlc/app/IosMediaLibrary.kt shared/src/commonMain/kotlin/org/videolan/vlc/app/VlcSharedApi.kt shared/src/iosMain/kotlin/org/videolan/vlc/compose/app/MainViewController.kt ios/App/AppDelegate.swift ios/App/MediaImporter.swift`

## Status

- **Priority**: P0
- **Effort**: L
- **Risk**: HIGH — migration mistakes can lose imported metadata or playlists.
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `098874d3f`, 2026-07-26
- **Beads**: `compose-as7n`

## Why this matters

`IosMediaLibrary` keeps every catalog structure in `MutableStateFlow`; only files themselves survive a relaunch. Worse, `ImportToolbar` refresh scans folders and immediately calls `seedDemoLibrary`, whose `replaceLibrary` discards the scanned catalog. Production must never replace user-visible media with demos.

## Current state

- `shared/src/iosMain/kotlin/org/videolan/vlc/app/IosMediaLibrary.kt:30-38` defines in-memory media, folders, playlists, favorites, and history.
- `IosMediaLibrary.kt:51-61` replaces/upserts catalog entries; `:152-224` mutates playlists and history.
- `shared/src/iosMain/kotlin/org/videolan/vlc/compose/app/MainViewController.kt:17-20` seeds demos when the initial catalog is empty.
- `ios/App/AppDelegate.swift:112-115` refreshes local folders and then seeds demos unconditionally.
- `shared/build.gradle.kts` already exposes KMP DataStore preferences. Keep repository APIs stable and add durable catalog storage behind the iOS repository boundary.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Shared tests | `./gradlew :shared:allTests --no-configuration-cache --console=plain` | exit 0 |
| iOS compile | `./gradlew :shared:compileKotlinIosSimulatorArm64 --no-configuration-cache --console=plain` | exit 0 |
| iOS build | `xcodebuild -project ios/VLC-iOS.xcodeproj -scheme VLC-iOS -sdk iphonesimulator -configuration Debug build CODE_SIGNING_ALLOWED=NO` | `BUILD SUCCEEDED` |

## Scope

**In scope**: iOS media repository/storage, importer and root behavior, common serialization/storage support if required, repository tests.

**Out of scope**: Android medialibrary persistence, remote network roots, cloud sync, changing the public `MediaRepository`/`PlaylistRepository` contract unless required for correct persistence.

## Steps

### Step 1: Add a durable iOS catalog store

Create a versioned storage representation for catalog items, playlist membership/order, favorite playlists, and history. Use one repository-owned persistence seam; write atomically after mutations and load before exposing flows. Preserve media URI as the stable reconciliation key and use serializable DTOs if common models are not serialization-safe.

**Verify**: add unit tests that save then reconstruct media, duplicate-URI upsert, playlist order, favorites, and history; shared tests pass.

### Step 2: Reconcile scans instead of replacing state

Change `scanDocumentsFolder`/`MediaImporter.merge` so a scan updates known file records, adds new files, and removes only missing local-file records after a full successful scan. Do not remove streams or externally handed-off URLs during a Documents reconciliation.

**Verify**: tests cover scan add/update/remove and preserve a non-file stream and playlist references.

### Step 3: Remove production demo replacement

Delete automatic demo seeding from the iOS root and refresh button. If development fixtures remain useful, gate them behind an explicit debug-only action that cannot execute in a release build. Render a clear empty state with Files/Photos import affordances for first run.

**Verify**: `rg -n "seedDemoLibrary" ios shared/src/iosMain` finds only an explicitly debug-gated fixture, or no result.

### Step 4: Validate relaunch behavior

Add an iOS simulator smoke flow: import a media file, create/reorder a playlist, favorite media, play it, relaunch, refresh, and assert all metadata remains and no demo rows appear.

**Verify**: iOS compile/build and the documented smoke flow pass.

## Done criteria

- [ ] Imported catalog, playlists, favorites, and history survive relaunch.
- [ ] Refresh is idempotent and never replaces user media with demos.
- [ ] First-run empty state is deliberate and import-oriented.
- [ ] New persistence/reconciliation tests pass.

## STOP conditions

- Existing model types cannot be safely represented without a schema/version migration.
- File scans cannot distinguish user-managed local records from network or share-extension records.
- Persistence would require storing decoder credentials or security-scoped bookmarks without a reviewed security design.

## Maintenance notes

Version the stored schema from day one. A future network-browser implementation must own credentials separately and must not serialize secrets into this catalog.
