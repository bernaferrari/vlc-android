plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

val rootExtra = rootProject.extra
val desugarLib = rootExtra["desugarLib"] as String
val remoteAccessVersion = rootExtra["remoteAccessVersion"] as String
val ktorVersion = rootExtra["ktorVersion"] as String
val composeBomVersion = rootExtra["composeBomVersion"] as String
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:$desugarLib")
    api(project(":shared"))
    add("devImplementation", project(":application:remote-access-client"))
    debugImplementation(project(":application:remote-access-client"))
    releaseImplementation(project(":application:remote-access-client"))
    releaseImplementation("org.videolan.android:remote-access:$remoteAccessVersion")
    add("vlcBundleImplementation", "org.videolan.android:remote-access:$remoteAccessVersion")
    implementation(project(":application:vlc-android"))
    implementation("io.ktor:ktor:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    debugImplementation("org.slf4j:slf4j-android:1.7.36")
    val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
