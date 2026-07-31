import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date
import com.android.build.api.dsl.LibraryExtension
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

val rootExtra = rootProject.extra
val appId = rootExtra["appId"] as String
val appVersionCode = rootExtra["versionCode"] as Int
val appVersionName = rootExtra["versionName"] as String
val libvlcVersion = rootExtra["libvlcVersion"] as String
val medialibraryVersion = rootExtra["medialibraryVersion"] as String
val buildRevision = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim()

fun generateTranslation(): String {
    val locales = fileTree(project(":application:resources").projectDir.resolve("src/main/res")) {
        include("**/strings.xml")
    }.files
        .map { stringsFile ->
            stringsFile.parentFile.name
                .removePrefix("values-")
                .replace("-r", "-")
                .ifEmpty { "en" }
        }
        .sorted()
    return locales.joinToString(prefix = "new String[]{\"", separator = "\",\"", postfix = "\"}")
}

fun buildTime(): String =
    if (providers.gradleProperty("forceDeterministic").isPresent) {
        providers.exec {
            commandLine("git", "show", "--no-patch", "--format=%cd", "--date=format-local:%Y-%m-%d")
        }.standardOutput.asText.get().trim()
    } else {
        SimpleDateFormat("yyyy-MM-dd").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

fun hostName(): String =
    if (providers.gradleProperty("forceDeterministic").isPresent) {
        providers.gradleProperty("forcedHostName").orNull ?: "Unknown"
    } else {
        "${System.getProperty("user.name")}@${InetAddress.getLocalHost().hostName}"
    }

fun dav1dVersion(): String {
    val rules = rootProject.file("libvlcjni/vlc/contrib/src/dav1d/rules.mak")
    return rules.takeIf { it.exists() }
        ?.useLines { lines ->
            lines.firstOrNull { it.contains("DAV1D_VERSION := ") }
                ?.removePrefix("DAV1D_VERSION := ")
        }
        ?: "0.0.0"
}

fun changelog(): String {
    val news = rootProject.file("NEWS")
    if (!news.exists()) return ""
    return news.readLines()
        .dropWhile { !it.contains("---") }
        .drop(1)
        .takeWhile { it.isNotBlank() }
        .joinToString("\\n") { line ->
            line.trim().replace(Regex("(@|'|\"|\\\\|\\(|\\)|=|\\?)"), "\\\\$0")
        }
}

fun isBeta(): String = appVersionName.lowercase()
    .let { version -> version.contains("beta") || version.contains("rc") || version.contains("alpha") || version.contains("dev") }
    .toString()

android {
    namespace = "org.videolan.vlc"

    defaultConfig {
        resValue("string", "build_time", buildTime())
        resValue("string", "build_host", hostName())
        resValue("string", "build_revision", buildRevision)
        resValue("string", "changelog", changelog())
        resValue("string", "dav1d_version", dav1dVersion())
        resValue("string", "tv_provider_authority", "$appId.tv")
        buildConfigField("String", "LIBVLC_VERSION", "\"$libvlcVersion\"")
        buildConfigField("String", "ML_VERSION", "\"$medialibraryVersion\"")
        buildConfigField("String", "APP_ID", "\"$appId\"")
        buildConfigField("int", "VLC_VERSION_CODE", appVersionCode.toString())
        buildConfigField("String", "VLC_VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("String[]", "TRANSLATION_ARRAY", generateTranslation())
        testInstrumentationRunner = "org.videolan.vlc.MultidexTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables.useSupportLibrary = true
        ndkVersion = rootExtra["toolchainNdkVersion"] as String
        (rootExtra["toolchainNdkPath"] as String?)?.let { ndkPath = it }
    }

    packaging {
        jniLibs.pickFirsts += "**/*.so"
    }

    testOptions {
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        getByName("release") {
            proguardFile("proguard.cfg")
            buildConfigField("boolean", "BETA", isBeta())
            resValue("string", "benchmark_package_name", "org.videolan.vlcbenchmark")
        }
        getByName("debug") {
            buildConfigField("boolean", "BETA", "false")
            resValue("string", "benchmark_package_name", "org.videolan.vlcbenchmark.debug")
            buildConfigField("String", "APP_ID", "\"$appId.debug\"")
            resValue("string", "tv_provider_authority", "$appId.debug.tv")
            buildConfigField("boolean", "NO_TV", "false")
            multiDexEnabled = true
        }
        create("vlcBundle") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
        create("signedRelease") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
        create("dev") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
        }
    }

    sourceSets {
        named("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.directories.apply { clear(); addAll(listOf("src", "vlc4/src")) }
            kotlin.directories.apply { clear(); addAll(listOf("src", "vlc4/src")) }
            resources.directories.apply { clear(); add("src") }
            aidl.directories.apply { clear(); add("src") }
            res.directories.apply { clear(); add("res") }
            assets.directories.apply { clear(); add("assets") }
        }
        named("debug") {
            res.directories.apply { clear(); add("flavors/debug/res") }
            assets.directories.apply { clear(); add("flavors/debug/assets") }
        }
        named("dev") {
            res.directories.apply { clear(); add("flavors/debug/res") }
            assets.directories.apply { clear(); add("flavors/debug/assets") }
        }
        named("test") {
            java.directories.apply { clear(); addAll(listOf("test", "test-common")) }
            kotlin.directories.apply { clear(); addAll(listOf("test", "test-common")) }
            assets.directories.apply { clear(); add("flavors/debug/assets") }
        }
        named("androidTest") {
            java.directories.apply { clear(); addAll(listOf("androidTest", "test-common")) }
            kotlin.directories.apply { clear(); addAll(listOf("androidTest", "test-common")) }
            assets.directories.apply { clear(); addAll(listOf("flavors/debug/assets", "assets/schemas")) }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        resValues = true
        compose = true
    }
}

tasks.register<Jar>("generateSources") {
    from(extensions.getByType<LibraryExtension>().sourceSets.getByName("main").java.directories)
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
    }
    outputs.upToDateWhen { false }
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}

dependencies {
    val localLibVlc = rootProject.findProject(":libvlcjni:libvlc")
    add(
        "devApi",
        localLibVlc?.let { dependencies.project(mapOf("path" to it.path)) }
            ?: "org.videolan.android:libvlc-all:$libvlcVersion",
    )
    // Use the in-tree medialibrary bridge for every Android variant. The published artifact's
    // native binaries are extracted by :medialibrary, while its callback implementation is kept
    // in sync with this app and cannot reintroduce the playback-time JNI race.
    listOf("devApi", "debugApi", "releaseApi", "signedReleaseApi").forEach { configuration ->
        add(configuration, project(":medialibrary"))
    }
    testImplementation(project(":medialibrary"))
    listOf("releaseApi", "signedReleaseApi").forEach { configuration ->
        add(configuration, "org.videolan.android:libvlc-all:$libvlcVersion")
    }

    api(project(":application:tools"))
    api(project(":application:resources"))
    api(project(":application:compose"))
    api(project(":application:mediadb"))
    api(project(":application:live-plot-graph"))

    api(libs.androidx.activity.ktx)
    api(libs.androidx.material)
    api(libs.androidx.annotation)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.viewpager2)
    api(libs.androidx.multidex)
    api(libs.androidx.lifecycle.process)
    api(libs.androidx.lifecycle.service)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.lifecycle.livedata)
    api(libs.androidx.lifecycle.common.java8)
    api(libs.androidx.room.runtime)
    api(libs.androidx.window)
    api(libs.androidx.media3.session)
    api(libs.androidx.gridlayout)
    api(libs.androidx.car.app)

    ksp(libs.sqlite.jdbc)
    ksp(libs.androidx.room.compiler)
    api(libs.androidx.paging.runtime)

    api(libs.androidx.leanback)
    api(libs.androidx.leanback.preference)
    api(libs.androidx.tvprovider)

    api(libs.kotlin.reflect)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.ktor.client.cio)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.glance.appwidget)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.konfetti.xml)
    implementation(libs.zxing.core)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.colorpicker)

    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestUtil(libs.androidx.test.orchestrator)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockk)
    testImplementation(libs.livedata.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.falcon)
    androidTestImplementation(libs.screengrab)

    if (providers.gradleProperty("leakCanaryEnabled").orNull?.toBoolean() == true) {
        debugImplementation(libs.leakcanary.android)
        add("devImplementation", libs.leakcanary.android)
    }
}
