# Plan 003: Build target-aware iOS release artifacts

> **Executor instructions**: Keep `project.yml` and the generated Xcode project synchronized. Never make a device archive consume a simulator/debug framework.
>
> **Drift check**: `git diff --stat 098874d3f..HEAD -- shared/build.gradle.kts ios/project.yml ios/VLC-iOS.xcodeproj/project.pbxproj ios/setup.sh`

## Status

- **Priority**: P0
- **Effort**: M
- **Risk**: MED — build-system edits can break local simulator iteration.
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `098874d3f`, 2026-07-26
- **Beads**: `compose-glk3`

## Why this matters

The iOS pre-build script selects an iosArm64 framework for a device build, but the linked/embedded framework path is hard-coded to `iosSimulatorArm64/debugFramework`. That makes the checked-in project structurally unable to guarantee a valid device Release archive.

## Current state

- `shared/build.gradle.kts:55-75` declares separate static iOS frameworks for `iosArm64` and `iosSimulatorArm64`.
- `ios/project.yml:38-55` lists both search paths but embeds only `../shared/build/bin/iosSimulatorArm64/debugFramework/VLCShared.framework`.
- `ios/VLC-iOS.xcodeproj/project.pbxproj:33` hard-codes the same simulator framework file reference for Debug and Release.
- `ios/project.yml:57-68` already runs the appropriate Gradle link task based on `PLATFORM_NAME`; retain that source-of-truth concept for configuration and binary selection.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Simulator framework | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --no-configuration-cache --console=plain` | exit 0 |
| Device framework | `./gradlew :shared:linkReleaseFrameworkIosArm64 --no-configuration-cache --console=plain` | exit 0 |
| Project generation | `cd ios && xcodegen generate` | exits 0, project changes only as expected |
| Simulator build | `xcodebuild -project ios/VLC-iOS.xcodeproj -scheme VLC-iOS -sdk iphonesimulator -configuration Debug build CODE_SIGNING_ALLOWED=NO` | `BUILD SUCCEEDED` |
| Device archive | `xcodebuild -project ios/VLC-iOS.xcodeproj -scheme VLC-iOS -sdk iphoneos -configuration Release archive -archivePath "$PWD/build/VLC-iOS.xcarchive" CODE_SIGNING_ALLOWED=NO` | `ARCHIVE SUCCEEDED` |

## Scope

**In scope**: iOS target declarations, XCFramework/framework creation, Xcodegen configuration, project file, setup/build scripts, artifact inspection documentation.

**Out of scope**: signing identities, App Store metadata, MobileVLCKit source changes, general Kotlin dependency upgrades.

## Steps

### Step 1: Choose one target-aware artifact mechanism

Prefer a release/debug XCFramework containing iosArm64 and iosSimulatorArm64 slices, generated from `:shared`. If the installed Kotlin/Gradle version cannot create an XCFramework reliably, use generated Xcode build settings that select the exact target/configuration framework path. Do not keep a static simulator `PBXFileReference` as a fallback.

**Verify**: build both link tasks and inspect each framework binary with `lipo -info`/`file`; each reports the expected platform architecture.

### Step 2: Update the Xcodegen source and regenerate

Make `ios/project.yml` the sole declaration of the linked/embedded VLCShared artifact and regenerate `VLC-iOS.xcodeproj`. Ensure Debug and Release invoke corresponding Gradle framework tasks, and that physical-device configurations cannot resolve `iosSimulatorArm64`.

**Verify**: `xcodegen generate` followed by `git diff -- ios/VLC-iOS.xcodeproj/project.pbxproj` shows only expected generated changes.

### Step 3: Add artifact verification

Add a small checked-in script or CI step that archives without signing, locates `VLCShared.framework` inside the archive, and rejects simulator architecture/slice paths in a device archive.

**Verify**: simulator build and device archive commands pass; artifact check exits 0.

## Done criteria

- [ ] Simulator Debug and device Release resolve and embed different correct target artifacts.
- [ ] The checked-in project is reproducible from `project.yml`.
- [ ] A no-signing Release archive passes binary/slice inspection.

## STOP conditions

- MobileVLCKit cannot be linked with the selected Kotlin framework packaging mechanism.
- `xcodegen generate` is unavailable or produces a project that differs beyond this plan's scope.
- A device archive requires developer signing to reach link validation; report the exact tool limitation rather than weakening the verification.

## Maintenance notes

Any new iOS architecture target must be added to the artifact matrix and CI. Do not restore hard-coded framework paths for convenience.
