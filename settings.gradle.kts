pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/maven2/")
    }
    plugins {
        id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
        id("org.jetbrains.compose") version "1.10.3"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0"
    }
}

plugins {
    id("com.android.settings") version "9.3.1"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/maven2/") {
            content {
                includeGroupByRegex("org\\.jetbrains\\.(compose|skiko|androidx)(\\..*)?")
            }
        }
        // Keep the Kotlin/Wasm toolchain in settings-owned repositories, as in QuietGuard.
        // This prevents plugin-added project repositories from bypassing the dependency policy.
        ivy {
            name = "Node.js"
            url = uri("https://nodejs.org/dist/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy {
            name = "Yarn"
            url = uri("https://github.com/yarnpkg/yarn/releases/download/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        ivy {
            name = "Binaryen"
            url = uri("https://github.com/WebAssembly/binaryen/releases/download/")
            patternLayout {
                artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

// Single source of truth for the VLC major-version matrix.
// Default is VLC 4 (Compose/KMP/minSdk 26 path). Pass -PforceVlc3 to build the
// legacy VLC 3 dependency line (still needs NDK 21 for native modules).
val vlcMajorVersion = if (providers.gradleProperty("forceVlc3").orNull == "true") 3 else 4
gradle.extra["vlcMajorVersion"] = if (providers.gradleProperty("forceVlc4").orNull == "true") 4 else vlcMajorVersion

android {
    // Ktor 3.5's Netty runtime requires API 26+. Historical VLC 3 native builds can
    // still use NDK 21, but the Android app/library minSdk is 26 for both VLC 3 and 4.
    minSdk = 26
    targetSdk = 36
    // Adaptive Navigation 3's Android artifact requires API 37 at compile time. This only
    // expands the available compile surface; targetSdk remains 36 until its runtime-behaviour
    // migration is separately reviewed.
    compileSdk = 37
    buildToolsVersion = "36.0.0"
}

if (file("libvlcjni/libvlc").isDirectory) {
    include(":libvlcjni:libvlc")
}
include(":medialibrary")
include(":shared")
include(":webApp")
include(
    ":application:tools",
    ":application:resources",
    ":application:compose",
    ":application:mediadb",
    ":application:app",
    ":application:live-plot-graph",
    ":application:television",
    ":application:donations",
    ":application:remote-access-server",
    ":application:vlc-android",
    ":application:moviepedia",
    ":application:remote-access-client",
)
