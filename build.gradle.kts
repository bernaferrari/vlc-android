import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import java.util.Properties
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.api.tasks.javadoc.Javadoc
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.version.catalog.update)
}

allprojects {
    tasks.withType<Javadoc>().configureEach {
        // Ignores errors from mavenAndroidJavadocs.
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("encoding", "UTF-8")
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    // Java 17 for all Android modules — matches Kotlin JVM targets. Applying this when each
    // plugin arrives avoids mutable afterEvaluate configuration.
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

// The Kotlin/Wasm plugin builds its transient npm workspace under build/wasm. Declare the Yarn
// version in that generated root package so Yarn does not inherit a parent package-manager field.
tasks.withType<RootPackageJsonTask>().configureEach {
    doLast {
        val packageJson = rootPackageJsonFile.get().asFile
        val contents = packageJson.readText(Charsets.UTF_8)
        if (!contents.contains("\"packageManager\"")) {
            val beforeClosingBrace = contents.substring(0, contents.lastIndexOf('}')).trim()
            val separator = if (beforeClosingBrace.endsWith('{')) "\n" else ",\n"
            packageJson.writeText(
                "${beforeClosingBrace}${separator}  \"packageManager\": \"yarn@1.22.22\"\n}\n",
                Charsets.UTF_8,
            )
        }
    }
}

versionCatalogUpdate {
    keep {
        keepUnusedVersions = true
    }
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val vlcMajorVersion = gradle.extra["vlcMajorVersion"] as Int
val versionCode = 3_070_100
val versionName = if (vlcMajorVersion == 3) "3.7.1" else "4.0.0-preview - $versionCode"

extra.apply {
    set("appId", "org.videolan.vlc")
    set("versionCode", versionCode)
    set("vlcMajorVersion", vlcMajorVersion)
    set("versionName", versionName)
    set("libvlcVersion", if (vlcMajorVersion == 3) "3.7.1" else "4.0.0-eap25")
    set("medialibraryVersion", "0.13.18" + if (vlcMajorVersion == 3) "" else "-vlc4")
    set("toolchainNdkVersion", localProperties.getProperty("android.ndkFullVersion") ?: if (vlcMajorVersion == 3) "21.4.7075529" else "28.2.13676358")
    set("toolchainNdkPath", localProperties.getProperty("android.ndkPath"))
}
