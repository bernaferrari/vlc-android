# `:application:compose` — Android interop shim

This module is **not** the home of VLC’s Compose UI.

## What lives here

Only Android View interop helpers used when a residual XML/View host still needs to embed Compose:

- `org.videolan.vlc.compose.interop.VLCComposeView` — drop-in `AbstractComposeView` for XML or programmatic View hierarchies

```kotlin
composeView.setContent {
    // Prefer shared theme/components from :shared
    org.videolan.vlc.compose.theme.VLCTheme {
        /* … */
    }
}
```

```xml
<org.videolan.vlc.compose.interop.VLCComposeView
    android:id="@+id/my_compose_host"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

## What lives in `:shared` instead

| Content | Location |
|---------|----------|
| Material3 theme / tokens | `shared/.../vlc/compose/theme/` |
| UI components (player chrome, browser rows, settings leaves, …) | `shared/.../vlc/compose/components/` |
| Compose Multiplatform resources | `shared/src/commonMain/composeResources/` |
| Domain + prefs used by UI | `shared/.../vlc/` and `shared/.../tools/` |

`:application:compose` depends on `:shared` and re-exports nothing beyond the Android interop types above.

## Why the module still exists

Historically this was the hybrid-migration bootstrap (Wave 1/2 leaf catalog, interop lab notes, gate log). That migration **completed for phone UI** — active phone screens are Compose-hosted and components moved into KMP `:shared`. Keeping a tiny Android library avoids dragging `AbstractComposeView` into pure commonMain while older View hosts (and some TV/system edges) still need a bridge.

## Consume

```groovy
dependencies {
    implementation project(':application:compose')
    // Components/theme: come transitively via :shared (also depended on by app modules)
}
```

## Validate

```bash
./gradlew :application:compose:build :application:vlc-android:compileDebugKotlin --no-daemon --console=plain
```

See the root [README](../../README.md) for project structure, minSdk policy, and KMP layout. iOS / shared architecture notes: [ios/README.md](../../ios/README.md).
