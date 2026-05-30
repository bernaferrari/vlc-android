# Add project specific ProGuard rules here for the VLC Compose library.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Compose libraries are primarily kept via consumer-rules.pro for downstream apps.
# These rules apply only if this module itself is minified (currently disabled in build.gradle).
# We keep our own public surface defensively.

-keep public class org.videolan.vlc.compose.** { public protected *; }
-keep public class org.videolan.vlc.compose.interop.** { *; }
