import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.FilterConfiguration
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
}

val rootExtra = rootProject.extra
val appId = rootExtra["appId"] as String
val appVersionCode = rootExtra["versionCode"] as Int
val appVersionName = rootExtra["versionName"] as String
val sharedComposeAssets = rootProject.layout.buildDirectory.dir("generated/assets/sharedComposeResources")
val abiCodes = mapOf("armeabi-v7a" to 5, "arm64-v8a" to 6, "x86" to 7, "x86_64" to 8)

val verifyDebugMedialibraryJni = tasks.register("verifyDebugMedialibraryJni") {
    group = "verification"
    description = "Verifies that every universal debug APK packages libmla for each supported ABI."
    dependsOn("packageDebug")

    doLast {
        val debugApks = fileTree(layout.buildDirectory.dir("outputs/apk/debug").get().asFile) {
            include("**/*.apk")
        }.files
        check(debugApks.isNotEmpty()) {
            "No debug APK was produced to verify medialibrary JNI packaging."
        }

        val expectedEntries = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            .map { abi -> "lib/$abi/libmla.so" }
        debugApks.forEach { apk ->
            ZipFile(apk).use { zip ->
                val missing = expectedEntries.filter { entry -> zip.getEntry(entry) == null }
                check(missing.isEmpty()) {
                    "${apk.name} is missing medialibrary JNI entries: ${missing.joinToString()}"
                }
            }
        }
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(verifyDebugMedialibraryJni)
}

android {
    namespace = "org.videolan.mobile.app"

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        applicationId = appId
        versionCode = appVersionCode
        versionName = appVersionName
        multiDexEnabled = true
        testInstrumentationRunner = "org.videolan.vlc.MultidexTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables.useSupportLibrary = true
    }

    packaging {
        jniLibs.pickFirsts += setOf(
            "lib/armeabi-v7a/libc++_shared.so",
            "lib/armeabi/libc++_shared.so",
            "lib/arm64-v8a/libc++_shared.so",
            "lib/x86/libc++_shared.so",
            "lib/x86_64/libc++_shared.so",
        )
        resources.excludes += setOf(
            "META-INF/*",
            "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
        )
    }

    flavorDimensions += "abi"

    signingConfigs {
        create("release") {
            val keyStoreFile = providers.gradleProperty("keyStoreFile").orNull
            if (keyStoreFile != null) {
                storeFile = file(keyStoreFile)
                keyAlias = providers.gradleProperty("storealias").orNull
                val keyStorePassword = providers.environmentVariable("PASSWORD_KEYSTORE").orNull
                    ?.takeIf { it.isNotBlank() }
                    ?: providers.gradleProperty("storepwd").orNull
                storePassword = keyStorePassword
                keyPassword = keyStorePassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("signedRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            matchingFallbacks += "release"
        }
        create("vlcBundle") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
        create("vlcBundleAmazon") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isJniDebuggable = true
        }
        create("dev") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
        }
    }

    sourceSets {
        named("release") { manifest.srcFile("flavors/release/AndroidManifest.xml") }
        named("signedRelease") { manifest.srcFile("flavors/release/AndroidManifest.xml") }
        listOf("debug", "dev", "release", "signedRelease", "vlcBundle", "vlcBundleAmazon").forEach { variant ->
            named(variant) { assets.directories.add(sharedComposeAssets.get().asFile.absolutePath) }
        }
    }

    bundle {
        language { enableSplit = false }
    }

    splits {
        abi {
            val isReleaseBuild = gradle.startParameter.taskNames.any {
                it.matches(Regex("assemble.*release", RegexOption.IGNORE_CASE))
            }
            isEnable = isReleaseBuild
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }

    lint {
        abortOnError = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("vlcBundle")) { variantBuilder ->
        variantBuilder.minSdk = 30
    }
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            val abiName = output.filters
                .find { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
                ?: "all"
            val abiVersionCode = if (variant.buildType == "vlcBundle") 9 else abiCodes[abiName] ?: 0
            output.versionCode.set(10_000_000 + appVersionCode + abiVersionCode)
            val debugSuffix = if (variant.buildType == "debug") "-debug" else ""
            output.outputFileName.set(
                "VLC-Android-${appVersionName.replace(" ", "-")}$debugSuffix-$abiName.apk",
            )
        }
    }
}

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xno-param-assertions") }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":application:vlc-android"))
    implementation(project(":application:television"))
    implementation(project(":application:remote-access-server"))
    testImplementation(project(":application:television"))
    testImplementation(project(":application:remote-access-server"))

    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    androidTestUtil(libs.androidx.test.orchestrator)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockk)
    testImplementation(libs.livedata.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.falcon)
    androidTestImplementation(libs.screengrab)
}

tasks.configureEach {
    if (name in setOf(
            "mergeDebugAssets",
            "mergeDevAssets",
            "mergeReleaseAssets",
            "mergeSignedReleaseAssets",
            "mergeVlcBundleAssets",
            "mergeVlcBundleAmazonAssets",
        )
    ) {
        dependsOn(":shared:copyAndroidMainComposeResourcesToAndroidAssets")
    }
}
