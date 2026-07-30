plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

val rootExtra = rootProject.extra
val remoteAccessVersion = rootExtra["remoteAccessVersion"] as String
val ktorVersion = rootExtra["ktorVersion"] as String
val kotlinxVersion = rootExtra["kotlinx_version"] as String
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
version = remoteAccessVersion

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
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
