plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
}

val rootExtra = rootProject.extra
val composeBomVersion = rootExtra["composeBomVersion"] as String
val testCore = rootExtra["testCore"] as String
val junitExtVersion = rootExtra["junitExtVersion"] as String

android {
    namespace = "org.videolan.television"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":application:vlc-android"))
    api(project(":application:moviepedia"))
    val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("androidx.test:core:$testCore")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    implementation("com.jaredrummler:colorpicker:1.1.0")
}
