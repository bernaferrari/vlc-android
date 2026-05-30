# VLC Compose Module

**Isolated Compose entry point for the VLC Android hybrid migration.**

This is a standalone Android library module (`:application:compose`) that provides the foundation for introducing Jetpack Compose into the existing VLC for Android codebase without a big-bang rewrite.

## Why an Isolated Module?

- **Lowest-risk path**: New Compose code lives completely separately from legacy XML/DataBinding/fragment code.
- Permanent exceptions are expected and accepted (native C++ surfaces, complex legacy players, certain dialogs).
- Strategic goal: achieve **~80% migratable surface** over time. The remaining 20% stays in the classic architecture.
- Allows incremental adoption, easy rollback, and parallel development on the bootstrap branch.

## How to Consume

In any other module (most commonly `:application:app` or `:application:television`):

```groovy
// In the consuming module's build.gradle
dependencies {
    implementation project(':application:compose')
}
```

Then import:

```kotlin
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.components.VLCDropdownItem
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
```

## Interop Patterns (Hybrid Migration)

This module deliberately exposes several bridges so Compose can be used immediately inside the existing XML-heavy architecture.

### 1. VLCComposeView (for XML layouts)

Drop-in replacement for `ComposeView`. Use directly inside any legacy layout:

```xml
<org.videolan.vlc.compose.interop.VLCComposeView
    android:id="@+id/my_compose_host"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

In Kotlin (including inside DataBinding fragments/activities):

```kotlin
binding.myComposeHost.setContent {
    VLCTheme {
        MyComposableScreen(...)
    }
}
```

### 2. VLCAbstractComposeWidget (for reusable custom views)

Subclass and implement `WidgetContent()` to create a self-contained Compose widget that can be inflated from XML exactly like any other custom view:

```kotlin
class MyComposeWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    @Composable
    override fun WidgetContent() {
        VLCTheme {
            Text("Hello from Compose widget")
        }
    }
}
```

Use `<your.package.MyComposeWidget ... />` in any layout.

### 3. Full Compose Screens / Destinations

For new screens or major refactors, host entire `@Composable` trees using `VLCTheme` as the root. These can live in Compose-only fragments or be launched via the navigation system once Compose Navigation is introduced.

```kotlin
@Composable
fun MyFullScreen() {
    VLCTheme {
        // Material3 Scaffold, Lists, etc.
    }
}
```

## Current Status

- Basic theming foundation (`VLCTheme`, placeholder semantic tokens mapped from future `colors.xml` + `styles.xml` audit).
- Interop helpers (`VLCComposeView`, `VLCAbstractComposeWidget`).
- One example leaf component: `VLCDropdownItem` (Compose equivalent of legacy `dropdown_item.xml`).
- Preview support for Android Studio (see `PreviewUtils.kt`).
- Proper library packaging (consumer rules, proguard, test setup).

This module is intentionally minimal on purpose — it is the **bootstrap foundation**. Real component migration work happens in follow-up issues.

## Design Principles

- Everything public API is under `org.videolan.vlc.compose.*`
- Never enable ViewBinding or DataBinding inside this module.
- Use Material 3 exclusively.
- All Composables should accept `Modifier` and be previewable.
- Theme tokens will be expanded during the color/style audit (Phase 0 / follow-up work).

## Running / Validating

```bash
# From repo root (worktree)
./gradlew :application:compose:build
./gradlew :application:compose:connectedCheck   # requires device/emulator
```

Android Studio will immediately show `@Preview` renders for the theme and components once the module is synced.

---

**Part of the phase-0-compose-bootstrap effort.**  
See parent tracking issue and reconnaissance notes for the overall 80% migration strategy.
