import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "pl.monter.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "pl.monter.elektryk"
        minSdk = 26
        targetSdk = 36
        // Numer przebiegu CI rośnie z każdym buildem – Google Play wymaga rosnącego versionCode,
        // a Android nie zainstaluje aktualizacji ze starszym numerem.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "1.0.$build"
        manifestPlaceholders["appLabel"] = "@string/app_name"
    }

    signingConfigs {
        // Stały klucz debug w repozytorium: każda kolejna wersja z CI instaluje się jako aktualizacja,
        // więc postępy zostają. (Losowy klucz z każdego buildu wymuszał odinstalowanie = utratę zapisu).
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Klucz przesyłania do Google Play – tylko z sekretów CI, nigdy w repozytorium.
        val uploadStore = System.getenv("MONTER_KEYSTORE")
        if (uploadStore != null && file(uploadStore).exists()) {
            create("upload") {
                storeFile = file(uploadStore)
                storePassword = System.getenv("MONTER_KEYSTORE_PASSWORD")
                keyAlias = "upload"
                keyPassword = System.getenv("MONTER_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Wersja testowa obok wersji ze sklepu – osobna ikona „Spięcie! DEV" i osobny zapis.
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "Spięcie! DEV"
        }
        release {
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.play.games)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}
