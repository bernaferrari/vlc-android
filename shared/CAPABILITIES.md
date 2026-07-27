# Shared product capability matrix

The Compose shell is shared across Android, iOS, desktop JVM, and Wasm. Native engines are not.
`platformCapabilities` is the source of truth that prevents common UI and controller code from
offering actions a target cannot execute.

| Capability | Android | iOS | Wasm | JVM |
| --- | --- | --- | --- | --- |
| Shared navigation, library, playlists, history, and settings | Yes | Yes | Yes | Yes |
| Local media intake | Media-library scan | Files / Photos picker | Not yet | Not yet |
| Native media decoding | LibVLC | MobileVLCKit SPM | Not yet | Not yet |
| Picture in Picture | Yes, when Android activity supports it | Not yet | Not yet | Not yet |
| Renderer / cast selection | LibVLC renderer bridge | Not yet | Not yet | Not yet |
| Network share discovery and browsing | Yes | Not yet | Not yet | Not yet |
| Remote access server setting | Yes | Not exposed | Not exposed | Not exposed |

An unsupported capability is absent from the shared surface and is guarded in
`PlaybackController`/view models. It must not resolve to a no-op bridge. Implementing a new native
bridge requires changing this matrix and adding a target-level integration test before enabling it.
