// ════════════════════════════════════════════════
// ESTE ARCHIVO VA EN:
//   Z:\agroicam\app\build.gradle.kts
//   (dentro de /app, NO en la raíz)
// ════════════════════════════════════════════════
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    // Sin esto TFLite falla silenciosamente
    androidResources {
        noCompress += "tflite"
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
    // Compose — BOM controla todas las versiones
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)   // iconos extras (Eco, History, etc.)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation + Lifecycle + Activity
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)

    // Coroutines (para llamadas de red en background)
    implementation(libs.coroutines.android)

    // TFLite — Teachable Machine
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}