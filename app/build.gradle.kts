plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "fr.pilmil.sillon"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.pilmil.sillon"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("SILLON_VERSION_CODE")?.toIntOrNull() ?: 10000
        versionName = System.getenv("SILLON_VERSION_NAME") ?: "1.0.0-dev"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("sillon-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        val keystore = System.getenv("RELEASE_KEYSTORE")
        if (keystore != null && file(keystore).exists()) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    // Le modèle de diagnostic photo est lu directement depuis l'APK (mappé en mémoire) : pas de compression.
    androidResources {
        noCompress += "tflite"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    // Inférence locale du diagnostic photo (Apache 2.0), sans accès réseau.
    implementation("com.google.ai.edge.litert:litert:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
}
