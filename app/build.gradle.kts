plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.jarvis.food"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jarvis.food"
        minSdk = 26
        targetSdk = 35
        // Numero build di GitHub Actions: ogni APK nuovo si installa sopra il precedente.
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = run
        versionName = "1.0.$run"
        // Solo ARM (tutti gli smartphone moderni): APK più leggero. x86_64 solo per i test su emulatore.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Chiave di firma fissa: gli aggiornamenti si installano sopra la versione precedente.
    // Progetto personale in repo privato; per il Play Store servirà una chiave dedicata.
    signingConfigs {
        create("release") {
            storeFile = file("jarvisfood.jks")
            // Il keystore è cifrato nel repo (app/jarvisfood.jks.enc) e viene decifrato in CI con il secret SIGNING_PASS.
            storePassword = "jYIJF4M2dyW6QwBl0nLW38rj"
            keyAlias = "jarvisfood"
            keyPassword = "jYIJF4M2dyW6QwBl0nLW38rj"
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Il modello TFLite va letto non compresso dagli asset.
    androidResources {
        noCompress += "tflite"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
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

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
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

    // Smartwatch tramite Health Connect + sincronizzazione periodica
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Chatbot Jarvis: LLM open source (Gemma 4, Apache-2.0) eseguito sul telefono con LiteRT-LM di Google.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.1")

    // OCR degli scontrini sul telefono (modello incluso, funziona offline)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
