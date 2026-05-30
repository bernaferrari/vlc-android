# Consumer ProGuard / R8 rules for the VLC Compose module.
# These rules are packaged inside the AAR and automatically applied to any
# consuming application (e.g. :application:app) when it runs its own minification.
#
# This module is the isolated Compose entry point for the VLC Android hybrid migration.
# Goal: provide an 80% migratable surface while allowing permanent native/XML exceptions.

# --- Core Jetpack Compose keep rules (official patterns + practical library needs) ---
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.ui.** { *; }

-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }

# Material 3 (and M2 fallback) - required for components we expose and internal reflection
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }

# Explicitly keep @Composable functions and their generated machinery
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable *;
}

# Preserve annotation and signature information Compose relies on
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Common warnings that are safe to ignore for Compose libs
-dontwarn androidx.compose.**
-dontwarn com.google.errorprone.annotations.**
-dontnote androidx.compose.**

# --- VLC Compose public + interop API must survive minification in consumers ---
-keep public class org.videolan.vlc.compose.** { public protected *; }

# The interop helpers are instantiated from XML layouts and Java/Kotlin callers
# (VLCComposeView, VLCAbstractComposeWidget). They must never be removed or renamed.
-keep public class org.videolan.vlc.compose.interop.VLCComposeView { *; }
-keep public class org.videolan.vlc.compose.interop.VLCAbstractComposeWidget { *; }

# Theme entry point is the root of all usage
-keep public class org.videolan.vlc.compose.theme.VLCTheme { *; }

# Leaf components that may be referenced from adapters or legacy code
-keep public class org.videolan.vlc.compose.components.** { *; }
