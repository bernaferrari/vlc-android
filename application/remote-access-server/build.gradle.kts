plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

val remoteAccessDebug = providers.gradleProperty("vlc_remote_access_debug").orNull ?: "false"

android {
    namespace = "org.videolan.vlc.remoteaccessserver"
    defaultConfig {
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("Boolean", "VLC_REMOTE_ACCESS_DEBUG", remoteAccessDebug)
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
            assets.directories.add("assets")
        }
    }
    compileOptions.isCoreLibraryDesugaringEnabled = true
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    api(project(":shared"))
    add("devImplementation", project(":application:remote-access-client"))
    debugImplementation(project(":application:remote-access-client"))
    releaseImplementation(project(":application:remote-access-client"))
    releaseImplementation(libs.vlc.remote.access)
    add("vlcBundleImplementation", libs.vlc.remote.access)
    implementation(project(":application:vlc-android"))
    implementation(libs.ktor.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.network.tls.certificates)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.sessions)
    debugImplementation(libs.slf4j.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
