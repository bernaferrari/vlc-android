plugins {
    alias(libs.plugins.android.library)
}

val rootExtra = rootProject.extra
val androidxCoreVersion = rootExtra["androidxCoreVersion"] as String
val appCompatVersion = rootExtra["appCompatVersion"] as String
val junitVersion = rootExtra["junitVersion"] as String
val junitExtVersion = rootExtra["junitExtVersion"] as String
val espressoVersion = rootExtra["espressoVersion"] as String
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
    implementation("androidx.core:core-ktx:$androidxCoreVersion")
    implementation("androidx.appcompat:appcompat:$appCompatVersion")
    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
}
