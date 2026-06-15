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
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.videolan.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 17
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
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
                // Compose Multiplatform — shared UI across all targets
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.ui)
                api(compose.components.resources)
            }
        }

        // ── JVM intermediate (shared between android and jvm) ──
        val commonJvmMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.koin.annotations)
                api(libs.moshi)
                api(libs.retrofit)
                api("com.squareup.retrofit2:converter-moshi:${libs.versions.retrofit.get()}")
            }
        }

        androidMain {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.androidx.datastore.preferences)
                api(libs.koin.android)
            }
        }

        jvmMain {
            dependsOn(commonJvmMain)
        }

        // ── iOS intermediate (shared between all iOS architectures) ──
        val iosMain by creating {
            dependsOn(commonMain.get())
        }

        iosArm64Main { dependsOn(iosMain) }
        iosSimulatorArm64Main { dependsOn(iosMain) }
    }
}

// Override the root build.gradle's allprojects JVM 1.8 — we use 17 in :shared.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Koin compiler — compile-time verification of module bindings via KSP.
dependencies {
    add("kspAndroid", libs.koin.ksp.compiler)
    add("kspJvm", libs.koin.ksp.compiler)
}
