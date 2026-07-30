plugins {
    alias(libs.plugins.android.library)
}

val rootExtra = rootProject.extra
val appId = rootExtra["appId"] as String
val vlcMajorVersion = rootExtra["vlcMajorVersion"] as Int
val libvlcVersion = rootExtra["libvlcVersion"] as String
val medialibraryVersion = rootExtra["medialibraryVersion"] as String
val androidxPreferencesVersion = rootExtra["androidxPreferencesVersion"] as String
val androidxLeanbackVersion = rootExtra["androidxLeanbackVersion"] as String
val retrofitVersion = rootExtra["retrofit"] as String
val moshiVersion = rootExtra["moshi"] as String
val androidxCoreVersion = rootExtra["androidxCoreVersion"] as String
val testCore = rootExtra["testCore"] as String
val junitExtVersion = rootExtra["junitExtVersion"] as String
val kotlinxVersion = rootExtra["kotlinx_version"] as String
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
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}")
    api("androidx.multidex:multidex:2.0.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxVersion")
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
    api("com.google.android.material:material:${rootExtra["androidxMaterialVersion"]}")
    api("androidx.preference:preference-ktx:$androidxPreferencesVersion")
    api("androidx.leanback:leanback:$androidxLeanbackVersion")
    api("androidx.leanback:leanback-preference:$androidxLeanbackVersion")
    api("com.squareup.okhttp3:okhttp:4.9.3")
    api("com.squareup.okhttp3:logging-interceptor:4.9.3")
    api("com.github.mrmike:ok2curl:0.8.0")
    api("com.squareup.retrofit2:retrofit:$retrofitVersion")
    api("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    api("com.squareup.moshi:moshi-adapters:$moshiVersion")
    api("androidx.core:core-ktx:$androidxCoreVersion")
    testImplementation("androidx.test:core:$testCore")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
}
