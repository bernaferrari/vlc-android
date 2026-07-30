plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

val rootExtra = rootProject.extra
val appCompatVersion = rootExtra["appCompatVersion"] as String
val androidxCoreVersion = rootExtra["androidxCoreVersion"] as String
val roomVersion = rootExtra["roomVersion"] as String
val jdbcVersion = rootExtra["jdbcVersion"] as String
val kotlinxVersion = rootExtra["kotlinx_version"] as String
val junitVersion = rootExtra["junitVersion"] as String
val junitExtVersion = rootExtra["junitExtVersion"] as String
val espressoVersion = rootExtra["espressoVersion"] as String

android {
    namespace = "org.videolan.vlc.mediadb"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("dev") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
        }
        create("vlcBundle") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
    }
    packaging {
        jniLibs.pickFirsts += "**/*.so"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:$appCompatVersion")
    implementation("androidx.core:core-ktx:$androidxCoreVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("org.xerial:sqlite-jdbc:$jdbcVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation(project(":application:tools"))
    implementation(project(":application:resources"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxVersion")
    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
}
