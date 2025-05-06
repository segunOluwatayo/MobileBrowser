plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("org.jetbrains.kotlin.kapt")
}


android {
    namespace = "com.example.mobilebrowser"
    compileSdk = 35
    aaptOptions {
        noCompress += listOf("tflite", "bloom")
    }

    defaultConfig {
        applicationId = "com.example.mobilebrowser"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation (libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.testing)
//    implementation(libs.litert) {
//        // Kotlin-DSL style
//        exclude(group = "org.tensorflow", module = "tensorflow-lite")
//        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
//    }
//    implementation(libs.litert.support.api) {
//        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
//    }
    implementation(libs.tensorflow.lite.metadata)
    implementation (libs.tensorflow.lite.support)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.geckoview)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation (libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation (libs.androidx.security.crypto)
    implementation (libs.retrofit2.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.logging.interceptor)
    implementation (libs.androidx.hilt.work)
    implementation (libs.androidx.biometric)
    implementation(libs.androidx.work.runtime.ktx.v271)



    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
//    ksp(libs.hilt.compiler)
    kapt (libs.androidx.hilt.compiler)
    kapt (libs.hilt.android.compiler)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
}

