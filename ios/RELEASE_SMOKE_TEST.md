# VLC KMP release smoke test

This checklist is a release gate for the shared VLC phone product. It is not a
substitute for automated build/test jobs: it captures the native behavior that
requires real media, real operating systems, and a physical audio/video route.

Start each candidate by recording the candidate revision and device details.
Do not mark a candidate ready when any **Required** row is blank, failed, or
not supported by the capability matrix.

Candidate: `________`
Date: `________`
Tester: `________`

| Surface | Device / OS | Required checks | Result | Notes / issue |
| --- | --- | --- | --- | --- |
| Android phone | `________` | Import local audio + video; Video, Audio, Browse, Playlists, More; video decode; audio background/lock-screen controls; relaunch/history; Files/open-with handoff | ☐ Pass ☐ Fail | `________` |
| Android tablet | `________` | Same library and playback flow; resize/orientation; adaptive rail; back behavior; mini-player and player route | ☐ Pass ☐ Fail | `________` |
| Android phone | `________` | Where supported: PiP and renderer/cast selection; network browsing; remote-access server | ☐ Pass ☐ Fail ☐ N/A | `________` |
| iPhone | `________` | Files and Photos import; persistent catalog after relaunch; video decode surface; audio playback; player/back navigation; open-with handoff | ☐ Pass ☐ Fail | `________` |
| iPad | `________` | Same flow; compact/regular-width navigation and safe-area behavior; Files picker popover; rotation | ☐ Pass ☐ Fail | `________` |
| iPhone / iPad | `________` | Background/foreground recovery and lock-screen audio controls, if MobileVLCKit exposes them on this build | ☐ Pass ☐ Fail ☐ N/A | `________` |
| iPhone / iPad + second device | `________` | Enable local transfer on Wi-Fi; open the authenticated address on the second device; upload a media file; confirm catalog import/playback; disable and verify the address no longer accepts requests | ☐ Pass ☐ Fail | `________` |
| Wasm browser | `________` | Shared shell renders; import a browser-supported local audio/video file; playback, player collapse, and OPFS-backed relaunch; unsupported native actions remain hidden | ☐ Pass ☐ Fail | `________` |

## Automated evidence required before device testing

- `gradle :shared:allTests :webApp:wasmJsBrowserProductionWebpack`
- `gradle :application:app:assembleDebug :application:app:assembleRelease`
- `ios/verify.sh simulator`
- `ios/verify.sh archive`

The iOS checks require Xcode, XcodeGen, and a network-reachable MobileVLCKit
SPM resolution. Their unsigned builds deliberately do not prove App Store
signing or real-device decode; the rows above do.

## Capability guardrails

Use [`shared/CAPABILITIES.md`](../shared/CAPABILITIES.md) as the source of
truth. Do not turn an unsupported Wasm/iOS control into a required test merely
because Android supports it. Conversely, do not promote a build by marking a
supported capability as N/A without a linked decision.
