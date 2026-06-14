/*
 * Shared KMP module for VLC Android.
 *
 * Hosts platform-agnostic Kotlin code for the VLC media player app.
 *
 * Source set hierarchy (manual, no default template):
 *
 *   commonMain
 *     ├── commonJvmMain          (JVM: java.io, synchronized, DecimalFormat)
 *     │   ├── androidMain        (Android: Context, DataStore delegate)
 *     │   └── jvmMain            (Desktop JVM: Okio DataStore)
 *     └── iosMain                (iOS: darwin, darwinLog)
 *         ├── iosArm64Main
 *         └── iosSimulatorArm64Main
 *
 * Package is `org.videolan` (sub-packages match original modules) so that
 * existing imports across the Android codebase resolve without changes.
 */
plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    namespace = "org.videolan.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 17
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
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
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                // KMP DataStore — provides DataStore<Preferences> for all targets
                api("androidx.datastore:datastore-preferences-core:1.1.1")
                api("androidx.datastore:datastore-core-okio:1.1.1")
                api("com.squareup.okio:okio:3.9.0")
            }
        }

        // ── JVM intermediate (shared between android and jvm) ──
        val commonJvmMain by creating {
            dependsOn(commonMain.get())
        }

        androidMain {
            dependsOn(commonJvmMain)
            dependencies {
                implementation("androidx.datastore:datastore-preferences:1.1.1")
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
