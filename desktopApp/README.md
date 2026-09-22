# Compose Desktop design preview

Run `./gradlew :desktopApp:run` from the repository root. The initial 430 × 860 window shows the compact shared shell; resize it to inspect tablet/desktop layouts. The native Preview menu provides repeatable Phone (430 × 860), Narrow (320 × 700), Landscape (850 × 400), and Wide (1100 × 800) presets.

This development-only host renders the same `VlcKoinMainShell` as the app, with the existing JVM Koin graph. It overrides the JVM media source with fictional in-memory fixtures and uses the common `PlaylistEngine` without a decoder. Player controls exercise shared queue/state UI; audio and video are not decoded or played. The window title identifies the preview.

Preferences and generated geometric sample covers live under the system temporary directory in `vlc-compose-desktop-preview`. Real VLC preferences and the device media library are untouched. Fixture playlists, history and media are reset each launch. No network access or media permissions are needed.

For a native macOS application identity (including accessibility/UI inspection), build `./gradlew :desktopApp:createDistributable`. Open `desktopApp/build/compose/binaries/main/app/VLC Design Preview.app`. Its bundle identifier is `org.videolan.vlc.designpreview`; the app bundle is development-only and unsigned.

The **QA surfaces** menu opens 18 production secondary components in resizable native windows, including warnings, rename, casting, equalizer, PIN/pairing, updates, device prompts, guides, playback tools and support pages. The **Preview** menu also supplies 100% and 150% text sizes. Choose size/text settings before opening a gallery window. Gallery actions only change local fixture state; they never install an update, pair a real device, unlock an account or submit feedback. These previews complement platform compilation; they do not verify Android, iOS or TV host behavior on a device.
