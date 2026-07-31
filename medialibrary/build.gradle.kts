plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

val rootExtra = rootProject.extra
val medialibraryVersion = rootExtra["medialibraryVersion"] as String
val toolchainNdkVersion = rootExtra["toolchainNdkVersion"] as String
val toolchainNdkPath = rootExtra["toolchainNdkPath"] as String?
val libvlcVersion = rootExtra["libvlcVersion"] as String

// The Java callback bridge is maintained in this module, while the published medialibrary
// artifact still provides the prebuilt native mla binaries. Keeping the native payload separate
// lets every Android variant use the patched Java bridge without checking large binaries into git.
val nativeMedialibrary = configurations.create("nativeMedialibrary") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val nativeMedialibraryOutput = layout.buildDirectory.dir("generated/medialibrary-native-libs").get().asFile
val extractNativeMedialibrary = tasks.register<Sync>("extractNativeMedialibrary") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(nativeMedialibrary.elements.map { archives -> archives.map(::zipTree) }) {
        include("jni/**/*.so")
        eachFile { path = path.removePrefix("jni/") }
        includeEmptyDirs = false
    }
    into(nativeMedialibraryOutput)
}

dependencies {
    add(nativeMedialibrary.name, "org.videolan.android:medialibrary-all:$medialibraryVersion")
}

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
            jniLibs.directories.add(nativeMedialibraryOutput.path)
            manifest.srcFile("AndroidManifest.xml")
            java.directories.apply { clear(); addAll(listOf("src", "vlc4/src")) }
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

tasks.named("preBuild") {
    dependsOn(extractNativeMedialibrary)
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
