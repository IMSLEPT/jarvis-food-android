plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.jarvis.food"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jarvis.food"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    // Chiave di firma fissa: gli aggiornamenti si installano sopra la versione precedente.
    // Progetto personale in repo privato; per il Play Store servirà una chiave dedicata.
    signingConfigs {
        create("release") {
            storeFile = file("jarvisfood.jks")
            storePassword = "jarvisfood-release"
            keyAlias = "jarvisfood"
            keyPassword = "jarvisfood-release"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Il modello TFLite va letto non compresso dagli asset.
    androidResources {
        noCompress += "tflite"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Scanner barcode di Google Play Services (UI pronta, nessun permesso fotocamera).
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // AI offline: classificatore alimentare TFLite on-device.
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
}
