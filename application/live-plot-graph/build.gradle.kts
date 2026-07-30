plugins {
    alias(libs.plugins.android.library)
}

val rootExtra = rootProject.extra
val appCompatVersion = rootExtra["appCompatVersion"] as String
val androidxCoreVersion = rootExtra["androidxCoreVersion"] as String
val constraintLayoutVersion = rootExtra["constraintLayoutVersion"] as String
val junitVersion = rootExtra["junitVersion"] as String
val junitExtVersion = rootExtra["junitExtVersion"] as String
val espressoVersion = rootExtra["espressoVersion"] as String

android {
    namespace = "org.videolan.liveplotgraph"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}")
    implementation("androidx.appcompat:appcompat:$appCompatVersion")
    implementation("androidx.core:core-ktx:$androidxCoreVersion")
    implementation(project(":application:tools"))
    implementation("androidx.constraintlayout:constraintlayout:$constraintLayoutVersion")
    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
}
