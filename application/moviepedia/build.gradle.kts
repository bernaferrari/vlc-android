plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val rootExtra = rootProject.extra
val versionCode = rootExtra["versionCode"] as Int
val moviepediaUrl = providers.gradleProperty("moviepedia_api_url").orNull ?: "https://localhost/"

android {
    namespace = "org.videolan.moviepedia"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("int", "VLC_VERSION_CODE", versionCode.toString())
        buildConfigField("String", "MOVIEPEDIA_API_URL", "\"$moviepediaUrl\"")
        multiDexEnabled = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") { isJniDebuggable = true }
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
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":application:tools"))
    implementation(project(":application:vlc-android"))
    implementation(project(":application:compose"))
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    ksp(libs.sqlite.jdbc)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.adapters)
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
