pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.android.settings") version "9.3.1"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
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

android {
    minSdk = 26
    targetSdk = 36
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
