plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

val remoteClientSourceDir = layout.projectDirectory.dir("remoteaccess").asFile
val remoteAccessPackage = remoteClientSourceDir.resolve("package.json")
val remoteAccessRevision = if (remoteAccessPackage.exists()) {
    providers.exec {
        workingDir(remoteClientSourceDir)
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
} else {
    "unknown"
}
val bundledRemoteAccessVersion = runCatching {
    remoteAccessPackage.useLines { lines ->
        lines.firstOrNull { "\"version\"" in it }?.split('"')?.get(3)
    }
}.getOrNull() ?: "unknown"

group = "org.videolan.android"
version = libs.versions.vlc.remote.access.get()

val webCopy = tasks.register<Copy>("webCopy") {
    from("remoteaccess/dist")
    into("assets/dist")
    onlyIf { file("remoteaccess/dist").exists() }
}

android {
    namespace = "org.videolan.vlc.remoteaccessclient"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        resValue("string", "build_remote_access_revision", remoteAccessRevision)
        resValue("string", "remote_access_version", bundledRemoteAccessVersion)
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures.resValues = true
    sourceSets {
        named("main") {
            assets.directories.add("assets")
        }
    }
}

tasks.named("preBuild") { dependsOn(webCopy) }

dependencies {
    api(project(":shared"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
