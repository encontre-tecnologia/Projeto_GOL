plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Plugin do Allure para gerar o relatório
    id("io.qameta.allure") version "2.12.0"
}

android {
    namespace = "br.com.gui.carlembrete"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.gui.carlembrete"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // BOM do Compose
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Ícones
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    // --- TESTES E RELATÓRIOS ---
    testImplementation(libs.junit)
    // Dependência do Allure para entender os testes
    testImplementation("io.qameta.allure:allure-junit4:2.29.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Câmera (CameraX)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Inteligência Artificial (Google ML Kit - OCR)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // Permissões
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("com.google.zxing:core:3.5.1")
}

