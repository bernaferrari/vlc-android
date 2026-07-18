# VLC for Android

This is the official **Android** port of [VLC](https://videolan.org/vlc/), with an in-progress Kotlin Multiplatform (KMP) shared layer and a Compose Multiplatform UI stack.

VLC on Android plays all the same files as the classical version of VLC, and features a media database for Audio and Video files and stream.

- [Project Structure](#project-structure)
- [minSdk policy](#minsdk-policy)
- [LibVLC](#libvlc)
- [License](#license)
- [Build](#build)
  - [Build Application](#build-application)
  - [Build LibVLC](#build-libvlc)
- [Contribute](#contribute)
  - [Pull requests](#pull-requests)
  - [Translations](#translations)
- [Issues and feature requests](#issues-and-feature-requests)
- [Support](#support)

## Project Structure

```
vlc-android/
├── shared/                         # KMP module (commonMain + android/jvm/ios)
│   ├── domain models, repositories, preferences (DataStore)
│   ├── Compose Multiplatform UI components + theme
│   └── expect/actual platform bridges
├── application/                    # Android app modules
│   ├── app/                        # Application shell + merged manifest
│   ├── vlc-android/                # Phone UI hosts, PlaybackService, KMP adapters
│   ├── compose/                    # Thin Android interop shim (VLCComposeView)
│   ├── television/                 # Android TV UI
│   ├── remote-access-server/       # On-device Ktor remote access
│   ├── remote-access-client/       # Packaged web client assets (release from Maven)
│   ├── tools/, resources/, mediadb/, moviepedia/, donations/, live-plot-graph/
│   └── …
├── ios/                            # SwiftUI skeleton consuming VLCShared.framework
├── medialibrary/                   # Medialibrary JNI / gradle module
├── libvlcjni/ (optional checkout)  # LibVLC gradle module; VLC sources under vlc/
├── buildsystem/                    # Build scripts, CI, maven publication
└── settings.gradle
```

### Current architecture (ground truth)

| Area | Status |
|------|--------|
| Phone UI | Full Jetpack Compose / Compose-hosted screens (no phone Fragments / layout XML left in the active path) |
| Shared UI components | Live in `:shared` (`org.videolan.vlc.compose.*`) via Compose Multiplatform |
| `:application:compose` | **Interop shim only** — `VLCComposeView` for embedding Compose in residual Android View hosts. Components do **not** live here. |
| KMP `:shared` | commonMain models, DataStore preferences, repository/playback contracts, CMP UI; android/jvm/ios actuals |
| Android KMP adapters | `application/vlc-android/.../kmp/` (`AndroidMediaRepository`, `AndroidPlaybackService`, `VlcKmpInitializer`) — wiring in progress |
| iOS | Skeleton SwiftUI app in `ios/`; builds against `VLCShared.framework`. No full VLCKit player yet. DI uses Koin (`VlcKoin` / `VlcSharedApi`), not a `VlcAppContainer` type. |
| Remote access | `:application:remote-access-server` (Ktor + Compose share UI) |
| Permanent native islands | LibVLC video surface, medialibrary JNI, some system/widget/TV edges |

More detail: `application/compose/README.md` (Android interop shim) and `ios/README.md` (KMP / iOS skeleton).

## minSdk policy

| Surface | minSdk | Notes |
|---------|--------|--------|
| Project default (`settings.gradle`) | **23** | Required by DataStore 1.2.x, modern AndroidX, Compose |
| `:shared` Android target | **23** | Same floor — avoids manifest merger failures |
| `vlcBundle` app variant | **30** | Store bundle floor |
| VLC 3 **native** NDK | NDK 21 | Still used when `vlcMajorVersion == 3` for ABI/native API 17-era toolchains; **Java/Kotlin app code is min 23** |

Optional NDK path: set `android.ndkPath` (and optionally `android.ndkFullVersion`) in `local.properties`. Root ext property is `toolchainNdkPath`.

## LibVLC

LibVLC is the Android library embedding VLC engine, which provides a lot of multimedia features, like:

- Play every media file formats, every codec and every streaming protocols
- Hardware and efficient decoding on every platform, up to 8K
- Network browsing for distant filesystems (SMB, FTP, SFTP, NFS...) and servers (UPnP, DLNA)
- Playback of Audio CD, DVD and Bluray with menu navigation
- Support for HDR, including tonemapping for SDR streams
- Audio passthrough with SPDIF and HDMI, including for Audio HD codecs, like DD+, TrueHD or DTS-HD
- Support for video and audio filters
- Support for 360 video and 3D audio playback, including Ambisonics
- Ability to cast and stream to distant renderers, like Chromecast and UPnP renderers.

And more.

![LibVLC stack](https://images.videolan.org/images/libvlc_stack.png)

You can use our LibVLC module to power your own Android media player.
Download the `.aar` directly from [Maven](https://search.maven.org/artifact/org.videolan.android/libvlc-all) or build from source.

Have a look at our [sample codes](https://code.videolan.org/videolan/libvlc-android-samples).

## License

VLC for Android is licensed under [GPLv2 (or later)](COPYING). Android libraries make this, de facto, a GPLv3 application.

VLC engine *(LibVLC)* for Android is licensed under [LGPLv2](libvlc/COPYING.LIB).

## Build

Native libraries are published on Maven. So you can:

- Build the application and get libraries via gradle dependencies (JVM build only)
- Build the whole app (LibVLC + Medialibrary + Application)
- Build LibVLC only, and get an .aar package

### Build Application

VLC-Android build relies on gradle build modes:

- `Release` & `Debug` will get LibVLC and Medialibrary from Maven, and build application source code only.
- `SignedRelease` also, but it will allow you to sign application apk with a local keystore.
- `Dev` will build LibVLC, Medialibrary, and then build the application with these binaries. (via build scripts only)

Focused gates used during Compose/KMP work:

```bash
# Shared KMP Android compile
./gradlew :shared:compileDebugKotlinAndroid --no-daemon --console=plain

# Phone app Kotlin + interop shim
./gradlew :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain

# Manifest merge (minSdk / DataStore sanity)
./gradlew :application:app:processDebugMainManifest --no-daemon --console=plain
```

Force VLC 4 dependency line: `-PforceVlc4`.

### Build LibVLC

You will need a recent Linux distribution to build VLC.
It should work with Windows 10, and macOS, but there is no official support for this.

#### Setup

Check our [AndroidCompile wiki page](https://wiki.videolan.org/AndroidCompile/), especially for build dependencies.

Here are the essential points:

On Debian/Ubuntu, install the required dependencies:
```bash
sudo apt install automake ant autopoint cmake build-essential libtool-bin \
    patch pkg-config protobuf-compiler ragel subversion unzip git \
    openjdk-17-jre openjdk-17-jdk flex python3 wget
```

Setup the build environment:
Set `$ANDROID_SDK` to point to your Android SDK directory
`export ANDROID_SDK=/path/to/android-sdk`

Set `$ANDROID_NDK` to point to your Android NDK directory
`export ANDROID_NDK=/path/to/android-ndk`

Then, you are ready to build!

#### Build

`buildsystem/compile.sh -l -a <ABI>`

ABI can be `arm`, `arm64`, `x86`, `x86_64` or `all` for a multi-abis build

You can do a library release build with `-r` argument

#### Medialibrary

Build Medialibrary with `-ml` instead of `-l`

## Contribute

VLC is a libre and open source project, we welcome all contributions.

Just respect our [Code of Conduct](https://wiki.videolan.org/CoC/), and if you want do contribute to the UI or add a new feature, please open an issue first so there can be a discussion about it.


### Pull requests

Pull requests must be proposed on our [gitlab server](https://code.videolan.org/videolan/vlc-android/).

So you must create an account, fork vlc-android project, and propose your merge requests from it.

**Except for translations**, see the section below.

### Translations

You can help improving translations too by joining the [transifex vlc project](https://app.transifex.com/yaron/vlc-trans/dashboard/)

Translations merge requests are then generated from transifex work.

## Issues and feature requests

VLC for Android bugtracker is hosted on [VideoLAN gitlab](https://code.videolan.org/videolan/vlc-android/issues)  
Please look for existing issues and provide as much useful details as you can (e.g. vlc app version, device and Android version).

A template is provided, please use it!

Issues without relevant information will be ignored, we cannot help in this case.

## Support

- For usage support, use the in-app feedback option in the `About` screen
- Android mailing list: android@videolan.org
- bugtracker: https://code.videolan.org/videolan/vlc-android/issues
- IRC: *#videolan* channel on [libera](https://libera.chat/)
- VideoLAN forum: https://forum.videolan.org/viewforum.php?f=35
