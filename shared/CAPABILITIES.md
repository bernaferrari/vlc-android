# Shared product capability matrix

The Compose shell is shared across Android, iOS, desktop JVM, and Wasm. Native engines are not.
`platformCapabilities` is the source of truth that prevents common UI and controller code from
offering actions a target cannot execute.

| Capability | Android | iOS | Wasm | JVM |
| --- | --- | --- | --- | --- |
| Shared navigation, library, playlists, history, and settings | Yes | Yes | Yes | Yes |
| Local media intake | Media-library scan and SAF file picker | Files / Photos picker | Files picker; OPFS-backed when available, with persisted favorites and play state | Not yet |
| Media decoding / playback engine | LibVLC | Pinned upstream VLCKit SPM | Browser HTML audio/video for user-imported, browser-supported files | Not yet |
| Background audio and lock-screen transport | Android media session | AVAudioSession + Now Playing / Remote Command Center | Browser-managed, limited by browser policy | Not yet |
| Picture in Picture | Yes, when Android activity supports it | Yes, with the public VLCKit PiP drawable on a physical device | Not yet | Not yet |
| Renderer / cast selection | LibVLC renderer bridge | VLCKit renderer discovery and switching | Not yet | Not yet |
| Network share discovery and browsing | LibVLC discovery and browser | VLCKit LAN discovery and folder parsing | Not yet | Not yet |
| Remote access server setting | Yes | Authenticated local Wi-Fi media transfer | Not exposed | Not exposed |

An unsupported capability is absent from the shared surface and is guarded in
`PlaybackController`/view models. It must not resolve to a no-op bridge. Implementing a new native
bridge requires changing this matrix and adding a target-level integration test before enabling it.

iOS Picture in Picture uses the public `VLCPictureInPictureDrawable` and
`VLCPictureInPictureWindowControlling` contracts exported by pinned upstream VLCKit `4.0.0-a22`.
The Compose surface, full-screen output, and PiP share the same `VLCMediaPlayer`; there is no
AVPlayer fallback or second decoder. PiP is only usable after VLCKit supplies its controller, so it
must be verified on a physical device before release.

The iOS transfer service is deliberately narrow: it is an authenticated, foreground-only local
upload endpoint. Each enable creates a new bearer token, and incoming files are streamed to
Documents then reconciled by the shared catalog. It is not a replacement for Android's full
remote-control server; that broader protocol remains Android-owned until its portable behaviors
are specified behind shared contracts.

Wasm deliberately keeps its decoder boundary small: imported media is copied into the browser's
origin-private file system when available and reopened as a fresh object URL. Browser codec and
container support, autoplay policy, and storage availability still apply; a rejected format is
reported in the shared player instead of being presented as playable.
