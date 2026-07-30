plugins {
    alias(libs.plugins.android.library)
}

val rootExtra = rootProject.extra
val appCompatVersion = rootExtra["appCompatVersion"] as String
val lifecycleVersion = rootExtra["lifecycleVersion"] as String
val androidxPreferencesVersion = rootExtra["androidxPreferencesVersion"] as String
val androidxLeanbackVersion = rootExtra["androidxLeanbackVersion"] as String
val kotlinxVersion = rootExtra["kotlinx_version"] as String
val androidxCoreVersion = rootExtra["androidxCoreVersion"] as String
val junitVersion = rootExtra["junitVersion"] as String
val testRunner = rootExtra["testRunner"] as String
val espressoVersion = rootExtra["espressoVersion"] as String

android {
    namespace = "videolan.org.commontools"
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("vlcBundle") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
    }
    buildFeatures.buildConfig = true
}

dependencies {
    api(project(":shared"))
    api("androidx.appcompat:appcompat:$appCompatVersion")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    implementation("androidx.preference:preference-ktx:$androidxPreferencesVersion")
    api("androidx.tvprovider:tvprovider:$androidxLeanbackVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:4.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxVersion")
    implementation("androidx.core:core-ktx:$androidxCoreVersion")
    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test:runner:$testRunner")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
}
