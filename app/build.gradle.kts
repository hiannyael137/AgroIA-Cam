// ════════════════════════════════════════════════
// REEMPLAZA: app/build.gradle.kts
// ════════════════════════════════════════════════
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace  = "com.hypv.agroiacam"
    compileSdk = 35

    defaultConfig {
        applicationId             = "com.hypv.agroiacam"
        minSdk                    = 24
        targetSdk                 = 35
        versionCode               = 1
        versionName               = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions { jvmTarget = "11" }

    buildFeatures {
        viewBinding = true   // XML ViewBinding — NO Compose
    }

    androidResources {
        noCompress += "tflite"   // TFLite no se comprime
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE"
        }
    }
}

dependencies {
    // UI base
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // Lifecycle + ViewModel
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Coroutines
    implementation(libs.coroutines.android)

    // HTTP — Node-RED
    implementation(libs.okhttp)

    // TFLite — Teachable Machine
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}