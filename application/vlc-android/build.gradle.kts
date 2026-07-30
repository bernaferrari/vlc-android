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
val vlcMajorVersion = rootExtra["vlcMajorVersion"] as Int
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
        buildConfigField("int", "VLC_MAJOR_VERSION", vlcMajorVersion.toString())
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
            val versionedSources = if (vlcMajorVersion == 4) listOf("src", "vlc4/src") else listOf("src", "vlc3/src")
            java.directories.apply { clear(); addAll(versionedSources) }
            kotlin.directories.apply { clear(); addAll(versionedSources) }
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
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

dependencies {
    val localLibVlc = rootProject.findProject(":libvlcjni:libvlc")
    add(
        "devApi",
        localLibVlc?.let { dependencies.project(mapOf("path" to it.path)) }
            ?: "org.videolan.android:libvlc-all:$libvlcVersion",
    )
    add("devApi", project(":medialibrary"))
    testImplementation(project(":medialibrary"))
    add("debugApi", "org.videolan.android:libvlc-all:$libvlcVersion")
    add("debugApi", "org.videolan.android:medialibrary-all:$medialibraryVersion")
    listOf("releaseApi", "signedReleaseApi").forEach { configuration ->
        add(configuration, "org.videolan.android:libvlc-all:$libvlcVersion")
        add(configuration, "org.videolan.android:medialibrary-all:$medialibraryVersion")
    }

    api(project(":application:tools"))
    api(project(":application:resources"))
    api(project(":application:compose"))
    api(project(":application:mediadb"))
    api(project(":application:live-plot-graph"))

    api("androidx.activity:activity-ktx:${rootExtra["androidxActivityVersion"]}")
    api("com.google.android.material:material:${rootExtra["androidxMaterialVersion"]}")
    api("androidx.annotation:annotation:${rootExtra["androidxAnnotationVersion"]}")
    api("androidx.constraintlayout:constraintlayout:${rootExtra["constraintLayoutVersion"]}")
    api("androidx.viewpager2:viewpager2:${rootExtra["viewPager2Version"]}")
    api("androidx.multidex:multidex:2.0.1")
    api("androidx.lifecycle:lifecycle-process:${rootExtra["lifecycleVersion"]}")
    api("androidx.lifecycle:lifecycle-service:${rootExtra["lifecycleVersion"]}")
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:${rootExtra["lifecycleVersion"]}")
    api("androidx.lifecycle:lifecycle-runtime-ktx:${rootExtra["lifecycleVersion"]}")
    api("androidx.lifecycle:lifecycle-livedata-ktx:${rootExtra["lifecycleVersion"]}")
    api("androidx.lifecycle:lifecycle-common-java8:${rootExtra["lifecycleVersion"]}")
    api("androidx.room:room-runtime:${rootExtra["roomVersion"]}")
    api("androidx.window:window:${rootExtra["windowVersion"]}")
    api("androidx.media:media:${rootExtra["mediaVersion"]}")
    api("androidx.gridlayout:gridlayout:1.0.0")
    api("androidx.car.app:app:${rootExtra["carVersion"]}")

    ksp("org.xerial:sqlite-jdbc:${rootExtra["jdbcVersion"]}")
    ksp("androidx.room:room-compiler:${rootExtra["roomVersion"]}")
    api("androidx.paging:paging-runtime-ktx:${rootExtra["pagingVersion"]}")

    api("androidx.leanback:leanback:${rootExtra["androidxLeanbackVersion"]}")
    api("androidx.leanback:leanback-preference:${rootExtra["androidxLeanbackVersion"]}")
    api("androidx.tvprovider:tvprovider:${rootExtra["androidxLeanbackVersion"]}")

    api("org.jetbrains.kotlin:kotlin-reflect:${rootExtra["kotlin_version"]}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootExtra["kotlinx_version"]}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:${rootExtra["kotlinx_version"]}")

    implementation(platform("androidx.compose:compose-bom:${rootExtra["composeBomVersion"]}"))
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.glance:glance-appwidget:${rootExtra["glanceVersion"]}")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
    implementation("com.google.zxing:core:3.4.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("com.jaredrummler:colorpicker:1.1.0")

    androidTestImplementation("androidx.test.espresso:espresso-core:${rootExtra["espressoVersion"]}")
    androidTestImplementation("androidx.test.espresso:espresso-intents:${rootExtra["espressoVersion"]}")
    testImplementation("junit:junit:${rootExtra["junitVersion"]}")
    androidTestImplementation("androidx.room:room-testing:${rootExtra["roomVersion"]}")
    testImplementation("androidx.arch.core:core-testing:${rootExtra["archVersion"]}")
    androidTestImplementation("androidx.arch.core:core-testing:${rootExtra["archVersion"]}")
    androidTestImplementation("androidx.test.ext:junit:${rootExtra["junitExtVersion"]}")
    androidTestUtil("androidx.test:orchestrator:${rootExtra["orchestrator"]}")
    testImplementation("androidx.test:core:${rootExtra["testCore"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootExtra["kotlinx_version"]}")
    testImplementation("org.mockito:mockito-core:${rootExtra["mockito"]}")
    testImplementation("io.mockk:mockk:${rootExtra["mockk"]}")
    testImplementation("com.jraska.livedata:testing-ktx:${rootExtra["livedataTest"]}")
    testImplementation("org.robolectric:robolectric:${rootExtra["robolectric"]}")
    androidTestImplementation("androidx.test:rules:${rootExtra["testCore"]}")
    androidTestImplementation("com.jraska:falcon:2.2.0")
    androidTestImplementation("tools.fastlane:screengrab:2.1.0")

    if (providers.gradleProperty("leakCanaryEnabled").orNull?.toBoolean() == true) {
        debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
        add("devImplementation", "com.squareup.leakcanary:leakcanary-android:2.12")
    }
}
