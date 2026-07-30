plugins {
    alias(libs.plugins.android.library)
}

val publicApiKey = providers.environmentVariable("K8S_SECRET_VLC_PLAYSTORE_PUBLIC_API_KEY")
    .orElse(providers.gradleProperty("vlc_playstore_public_api_key"))
    .orNull
    .orEmpty()

android {
    namespace = "org.videolan.vlc.donations"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        multiDexEnabled = true
        buildConfigField("String", "PUBLIC_API_KEY", "\"$publicApiKey\"")
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
        aidl = true
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":shared"))
    implementation(project(":application:tools"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
