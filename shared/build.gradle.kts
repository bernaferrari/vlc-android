@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

/*
 * Shared KMP module for VLC Android.
 *
 * Hosts platform-agnostic Kotlin code for the VLC media player app.
 *
 * Source set hierarchy (manual, no default template):
 *
 *   commonMain
 *     ├── commonJvmMain          (JVM: java.io, synchronized, DecimalFormat)
 *     │   ├── androidMain        (Android: Context, DataStore delegate, Koin-Android)
 *     │   └── jvmMain            (Desktop JVM: Okio DataStore)
 *     └── iosMain                (iOS: darwin, darwinLog)
 *         ├── iosArm64Main
 *         └── iosSimulatorArm64Main
 *
 * Package is `org.videolan` (sub-packages match original modules) so that
 * existing imports across the Android codebase resolve without changes.
 *
 * All dependency versions are tracked in gradle/libs.versions.toml.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // AGP 9 no longer supports the legacy Android-library target alongside
    // Kotlin Multiplatform. This plugin keeps the Android target genuinely KMP.
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "org.videolan.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
        // Keep common tests executable against Android as the KMP target evolves.
        withHostTest {}
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    wasmJs {
        browser()
        useEsModules()
    }

    // iOS targets (no x86 simulator — Apple Silicon only)
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries {
            framework {
                baseName = "VLCShared"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                // KMP DataStore — provides DataStore<Preferences> for all targets
                api(libs.androidx.datastore.preferences.core)
                api(libs.androidx.datastore.core.okio)
                api(libs.okio)
                // Koin — dependency injection for KMP
                api(libs.koin.core)
                api(libs.koin.compose)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                // Compose Multiplatform — shared UI across all targets
                api(libs.jetbrains.compose.runtime)
                api(libs.jetbrains.compose.foundation)
                api(libs.jetbrains.compose.material3)
                api(libs.jetbrains.compose.animation)
                api(libs.jetbrains.compose.ui)
                api(libs.jetbrains.compose.resources)
                // QuietGuard's common, deterministic tonal scheme generator. It keeps every
                // selected VLC seed coherent on Android, iOS, JVM, and Wasm.
                api(libs.material.kolor)
                // Navigation 3 is available for Android, iOS, JVM, and Wasm on Compose 1.10+.
                api(libs.jetbrains.navigation3.ui)
                // Uses a bottom bar on compact layouts and a navigation rail on wider hosts.
                api(libs.compose.adaptive.navigation.suite)
                // Nav3 list-detail scenes keep a library visible beside its selected detail on wide hosts.
                api(libs.compose.adaptive.navigation3)
                // Common lifecycle ownership matches Navigation 3 entries on Android, iOS, JVM, and Wasm.
                api(libs.jetbrains.lifecycle.runtime.compose)
                api(libs.jetbrains.lifecycle.viewmodel.navigation3)
                // Paging 3 — KMP common + Compose Multiplatform integration
                api(libs.androidx.paging.common)
                api(libs.androidx.paging.compose)
            }
        }

        // ── JVM intermediate (shared between android and jvm) ──
        val commonJvmMain = create("commonJvmMain") {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.moshi)
            }
        }

        androidMain {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.datastore.preferences)
                api(libs.koin.android)
                implementation(libs.jetbrains.compose.ui.tooling.preview)
            }
        }

        jvmMain {
            dependsOn(commonJvmMain)
        }

        wasmJsMain {
            dependencies {
                // Browser DOM + media declarations used by the Wasm-only player and importer.
                // This is the same explicit Wasm boundary QuietGuard uses for browser APIs.
                implementation(libs.kotlinx.browser)
            }
        }

        // ── iOS intermediate (shared between all iOS architectures) ──
        val iosMain = create("iosMain") {
            dependsOn(commonMain.get())
        }

        iosArm64Main { dependsOn(iosMain) }
        iosSimulatorArm64Main { dependsOn(iosMain) }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// Keep explicit Kotlin target metadata close to the KMP target declaration.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Android-KMP does not currently assign an output directory to Compose's
// Android asset-copy task. Without it, an Android application can compile the
// shared UI but cannot package its resource bundle. Use one stable generated
// directory that every Android host variant can consume.
tasks.configureEach {
    if (name == "copyAndroidMainComposeResourcesToAndroidAssets") {
        // The Compose task type is internal, but its Gradle property is public
        // at runtime. Configure it reflectively until the plugin exposes a
        // stable DSL.
        val outputDirectory = javaClass.getMethod("getOutputDirectory")
            .invoke(this) as org.gradle.api.file.DirectoryProperty
        outputDirectory.set(layout.buildDirectory.dir("generated/assets/sharedComposeResources"))
    }
}

dependencies {
    // Android-KMP has one publishable variant. Keep inspection tooling available
    // to local Android runtime/Preview without leaking it into the shared AAR.
    add("androidRuntimeClasspath", libs.jetbrains.compose.ui.tooling)
}
