plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.voicelock"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.voicelock"
        minSdk = 25
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    androidResources {
        // Store the CAM++ model uncompressed so the AssetManager can read it without inflating ~28 MB.
        noCompress += "onnx"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Base Kit dependencies
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.timber)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    // Sherpa-ONNX speaker verification (AAR from https://github.com/k2-fsa/sherpa-onnx/releases)
    implementation(files("libs/sherpa-onnx-1.13.8.aar"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
