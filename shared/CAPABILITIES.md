# Shared product capability matrix

The Compose shell is shared across Android, iOS, desktop JVM, and Wasm. Native engines are not.
`platformCapabilities` is the source of truth that prevents common UI and controller code from
offering actions a target cannot execute.

| Capability | Android | iOS | Wasm | JVM |
| --- | --- | --- | --- | --- |
| Shared navigation, library, playlists, history, and settings | Yes | Yes | Yes | Yes |
| Local media intake | Media-library scan and SAF file picker | Files / Photos picker | Files picker; OPFS-backed when available, with persisted favorites and play state | Not yet |
| Media decoding / playback engine | LibVLC | MobileVLCKit SPM | Browser HTML audio/video for user-imported, browser-supported files | Not yet |
| Background audio and lock-screen transport | Android media session | AVAudioSession + Now Playing / Remote Command Center | Browser-managed, limited by browser policy | Not yet |
| Picture in Picture | Yes, when Android activity supports it | Not yet | Not yet | Not yet |
| Renderer / cast selection | LibVLC renderer bridge | Not yet | Not yet | Not yet |
| Network share discovery and browsing | Yes | Not yet | Not yet | Not yet |
| Remote access server setting | Yes | Not exposed | Not exposed | Not exposed |

An unsupported capability is absent from the shared surface and is guarded in
`PlaybackController`/view models. It must not resolve to a no-op bridge. Implementing a new native
bridge requires changing this matrix and adding a target-level integration test before enabling it.

Wasm deliberately keeps its decoder boundary small: imported media is copied into the browser's
origin-private file system when available and reopened as a fresh object URL. Browser codec and
container support, autoplay policy, and storage availability still apply; a rejected format is
reported in the shared player instead of being presented as playable.
