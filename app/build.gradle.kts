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
        manifestPlaceholders["allowAutoBackup"] =
            providers.gradleProperty("pilmil.allowAutoBackup").getOrElse("false")
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
                // Mot de passe de clé distinct si fourni, sinon celui du keystore (cas courant).
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Jamais de repli sur la clé de debug : un APK release mal signé serait inutilisable pour les mises à jour.
            // L'absence de keystore est refusée plus bas, au moment où une tâche release est demandée.
            signingConfig = signingConfigs.findByName("release")
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

// Le debug et les tests n'ont pas besoin de keystore : on n'échoue que si une tâche release est demandée.
gradle.taskGraph.whenReady {
    val releaseTasks = setOf("assembleRelease", "bundleRelease", "packageRelease", "validateSigningRelease")
    if (allTasks.any { it.project == project && it.name in releaseTasks }) {
        val keystore = System.getenv("RELEASE_KEYSTORE")
        if (keystore.isNullOrBlank() || !file(keystore).exists()) {
            throw GradleException(
                "Signature release impossible : RELEASE_KEYSTORE est absent ou ne pointe pas vers un fichier existant " +
                    "(valeur : ${keystore ?: "non définie"}). Définissez RELEASE_KEYSTORE, RELEASE_STORE_PASSWORD, " +
                    "RELEASE_KEY_ALIAS (et RELEASE_KEY_PASSWORD si elle diffère du mot de passe du keystore)."
            )
        }
        val missing = listOf("RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS").filter { System.getenv(it).isNullOrBlank() }
        if (missing.isNotEmpty()) {
            throw GradleException("Signature release impossible : variable(s) manquante(s) : ${missing.joinToString()}.")
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("fr.pilmil.core:core-platform:0.1.0")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // CameraX : viseur intelligent avec analyse de cadrage temps-réel (Apache 2.0).
    val cameraVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // Inférence locale du diagnostic photo (Apache 2.0), sans accès réseau.
    implementation("com.google.ai.edge.litert:litert:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
}
