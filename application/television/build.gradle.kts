plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
}

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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    implementation(libs.colorpicker)
}
