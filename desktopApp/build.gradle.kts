plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${libs.versions.kotlinx.coroutines.get()}")
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.videolan.vlc.desktop.MainKt"
        nativeDistributions {
            packageName = "VLC Design Preview"
            packageVersion = "1.0.0"
            // Development bundle favors reliable inspection over installer size.
            includeAllModules = true
            macOS {
                bundleID = "org.videolan.vlc.designpreview"
            }
        }
    }
}

// Headless design review: ./gradlew :desktopApp:snapshots [-Pfilter=audio,player]
tasks.register<JavaExec>("snapshots") {
    group = "verification"
    description = "Renders shared screens to desktopApp/build/snapshots"
    val jvmCompilation = kotlin.jvm().compilations.getByName("main")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("org.videolan.vlc.desktop.SnapshotsKt")
    args(providers.gradleProperty("filter").getOrElse(""))
    systemProperty("snapshots.dir", layout.buildDirectory.dir("snapshots").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
    notCompatibleWithConfigurationCache("Development-only design review task")
}
