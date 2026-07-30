plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val rootExtra = rootProject.extra
val versionCode = rootExtra["versionCode"] as Int
val kotlinxVersion = rootExtra["kotlinx_version"] as String
val pagingVersion = rootExtra["pagingVersion"] as String
val lifecycleVersion = rootExtra["lifecycleVersion"] as String
val composeBomVersion = rootExtra["composeBomVersion"] as String
val roomVersion = rootExtra["roomVersion"] as String
val jdbcVersion = rootExtra["jdbcVersion"] as String
val retrofitVersion = rootExtra["retrofit"] as String
val moshiVersion = rootExtra["moshi"] as String
val junitVersion = rootExtra["junitVersion"] as String
val junitExtVersion = rootExtra["junitExtVersion"] as String
val espressoVersion = rootExtra["espressoVersion"] as String
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxVersion")
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("org.xerial:sqlite-jdbc:$jdbcVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    implementation("com.squareup.moshi:moshi-adapters:$moshiVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:4.2.1")
    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
}
