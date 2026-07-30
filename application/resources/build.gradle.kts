plugins {
    alias(libs.plugins.android.library)
}

val rootExtra = rootProject.extra
val appId = rootExtra["appId"] as String
val vlcMajorVersion = rootExtra["vlcMajorVersion"] as Int
val libvlcVersion = rootExtra["libvlcVersion"] as String
val medialibraryVersion = rootExtra["medialibraryVersion"] as String
val openSubtitlesApiKey = providers.environmentVariable("VLC_OPEN_SUBTITLES_API_KEY")
    .orElse(providers.gradleProperty("vlc_open_subtitles_api_key"))
    .orNull
    .orEmpty()

android {
    namespace = "org.videolan.resources"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "APP_ID", "\"$appId\"")
        buildConfigField("String", "VLC_OPEN_SUBTITLES_API_KEY", "\"$openSubtitlesApiKey\"")
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            buildConfigField("String", "APP_ID", "\"$appId.debug\"")
        }
        create("dev") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
        }
        create("vlcBundle") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
    }
    sourceSets {
        named("main") {
            val versionedSources = if (vlcMajorVersion == 4) listOf("src", "vlc4/src") else listOf("src", "vlc3/src")
            java.directories.apply { clear(); addAll(versionedSources) }
            kotlin.directories.apply { clear(); addAll(versionedSources) }
        }
        named("debug") {
            res.directories.apply { clear(); add("flavors/debug/res") }
        }
        named("dev") {
            res.directories.apply { clear(); add("flavors/debug/res") }
        }
    }
    packaging {
        jniLibs.pickFirsts += "**/*.so"
    }
    buildFeatures.buildConfig = true
}

dependencies {
    val localLibVlc = rootProject.findProject(":libvlcjni:libvlc")
    api(libs.kotlin.stdlib.jdk7)
    api(libs.androidx.multidex)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(project(":shared"))
    api(project(":application:tools"))
    if (localLibVlc != null) {
        add("devApi", dependencies.project(mapOf("path" to localLibVlc.path)))
    } else {
        add("devApi", "org.videolan.android:libvlc-all:$libvlcVersion")
    }
    add("devApi", project(":medialibrary"))
    add("debugApi", "org.videolan.android:libvlc-all:$libvlcVersion")
    add("debugApi", "org.videolan.android:medialibrary-all:$medialibraryVersion")
    add("releaseApi", "org.videolan.android:libvlc-all:$libvlcVersion")
    add("releaseApi", "org.videolan.android:medialibrary-all:$medialibraryVersion")
    add("vlcBundleApi", "org.videolan.android:libvlc-all:$libvlcVersion")
    add("vlcBundleApi", "org.videolan.android:medialibrary-all:$medialibraryVersion")
    api(libs.androidx.material)
    api(libs.androidx.preference)
    api(libs.androidx.leanback)
    api(libs.androidx.leanback.preference)
    api(libs.okhttp)
    api(libs.okhttp.logging.interceptor)
    api(libs.ok2curl)
    api(libs.retrofit)
    api(libs.retrofit.converter.moshi)
    api(libs.moshi.adapters)
    api(libs.androidx.core.ktx)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
