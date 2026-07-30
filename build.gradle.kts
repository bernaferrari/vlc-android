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
    set("android_plugin_version", libs.versions.android.gradle.plugin.get())
    set("kotlin_version", libs.versions.kotlin.get())
    set("kotlinx_version", libs.versions.kotlinx.coroutines.get())
    set("appId", "org.videolan.vlc")
    set("versionCode", versionCode)
    set("vlcMajorVersion", vlcMajorVersion)
    set("versionName", versionName)
    set("remoteAccessVersion", "0.17.0")
    set("libvlcVersion", if (vlcMajorVersion == 3) "3.7.1" else "4.0.0-eap25")
    set("medialibraryVersion", "0.13.18" + if (vlcMajorVersion == 3) "" else "-vlc4")
    set("toolchainNdkVersion", localProperties.getProperty("android.ndkFullVersion") ?: if (vlcMajorVersion == 3) "21.4.7075529" else "28.2.13676358")
    set("toolchainNdkPath", localProperties.getProperty("android.ndkPath"))
    set("targetSdkVersion", 36)
    set("compileSdkVersion", 37)
    set("desugarLib", "2.0.4")
    set("androidxCoreVersion", "1.12.0")
    set("appCompatVersion", "1.6.1")
    set("lifecycleVersion", "2.5.1")
    set("androidxPreferencesVersion", "1.2.1")
    set("androidxActivityVersion", "1.8.2")
    set("androidxFragmentVersion", "1.6.2")
    set("androidxAnnotationVersion", "1.7.1")
    set("androidxLeanbackVersion", "1.0.0")
    set("androidxMaterialVersion", "1.11.0")
    set("constraintLayoutVersion", "2.1.4")
    set("windowVersion", "1.1.0")
    set("mediaVersion", "1.6.0")
    set("carVersion", "1.7.0-beta03")
    set("jdbcVersion", "3.36.0")
    set("viewPager2Version", "1.0.0")
    set("archVersion", "2.2.0")
    set("roomVersion", "2.6.1")
    set("pagingVersion", "3.2.1")
    set("composeBomVersion", "2024.10.00")
    set("glanceVersion", "1.1.1")
    set("junitVersion", "4.13.2")
    set("junitExtVersion", "1.1.5")
    set("mockito", "2.25.0")
    set("retrofit", "2.7.1")
    set("moshi", "1.8.0")
    set("ktorVersion", "2.3.7")
    set("espressoVersion", "3.5.1")
    set("livedataTest", "1.2.0")
    set("robolectric", "4.16.1")
    set("mockk", "1.14.9")
    set("testRunner", "1.4.0")
    set("orchestrator", "1.4.2")
    set("testCore", "1.5.0")
    set("publishScriptPath", "../../buildsystem/publish.gradle")
}
