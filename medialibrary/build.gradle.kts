plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

val rootExtra = rootProject.extra
val medialibraryVersion = rootExtra["medialibraryVersion"] as String
val toolchainNdkVersion = rootExtra["toolchainNdkVersion"] as String
val toolchainNdkPath = rootExtra["toolchainNdkPath"] as String?
val vlcMajorVersion = rootExtra["vlcMajorVersion"] as Int
val libvlcVersion = rootExtra["libvlcVersion"] as String

group = "org.videolan.android"
version = medialibraryVersion

android {
    namespace = "org.videolan.medialibrary"
    defaultConfig {
        ndkVersion = toolchainNdkVersion
        toolchainNdkPath?.takeIf { it.isNotBlank() }?.let { ndkPath = it }
    }
    sourceSets {
        named("main") {
            jniLibs.directories.add("jni/libs")
            manifest.srcFile("AndroidManifest.xml")
            val versionedSources = if (vlcMajorVersion == 4) listOf("src", "vlc4/src") else listOf("src", "vlc3/src")
            java.directories.apply { clear(); addAll(versionedSources) }
            resources.directories.apply { clear(); add("src") }
            aidl.directories.apply { clear(); add("src") }
            res.directories.apply { clear(); add("res") }
            assets.directories.apply { clear(); addAll(listOf("assets", "libcompat/libs/armeabi")) }
        }
        named("test") {
            java.directories.apply { clear(); add("test") }
        }
        named("androidTest") {
            java.directories.apply { clear(); add("androidTest") }
            assets.directories.add("assets/schemas")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles("proguard.cfg")
        }
        create("dev") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
        }
    }
    buildFeatures {
        aidl = true
    }
}

tasks.named<Delete>("clean") {
    delete("build", "jni/libs", "jni/obj")
}

dependencies {
    val localLibVlcPath = ":libvlcjni:libvlc"
    if (rootProject.findProject(localLibVlcPath) != null) {
        val localLibVlcDependency = dependencies.project(localLibVlcPath)
        add("releaseImplementation", localLibVlcDependency)
        add("devImplementation", localLibVlcDependency)
    } else {
        add("releaseImplementation", "org.videolan.android:libvlc-all:$libvlcVersion")
        add("devImplementation", "org.videolan.android:libvlc-all:$libvlcVersion")
    }
    debugImplementation("org.videolan.android:libvlc-all:$libvlcVersion")
    api(libs.androidx.core)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
