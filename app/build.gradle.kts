plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Plugin do Allure para gerar o relatÃ³rio
    id("io.qameta.allure") version "2.12.0"
    id("com.google.gms.google-services")
}

import java.util.Properties

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val fipeBaseUrl = (localProps.getProperty("FIPE_BASE_URL") ?: "https://parallelum.com.br/fipe/")
    .trim()
    .ifEmpty { "https://parallelum.com.br/fipe/" }

fun propOrEnv(key: String): String? {
    return System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: localProps.getProperty(key)?.takeIf { it.isNotBlank() }
}

val releaseStoreFile = propOrEnv("RELEASE_STORE_FILE")
val releaseStorePassword = propOrEnv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = propOrEnv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = propOrEnv("RELEASE_KEY_PASSWORD") ?: releaseStorePassword
val ciVersionCode = System.getenv("CI_VERSION_CODE")?.toIntOrNull()
val isReleaseSigningReady = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "br.com.gui.carlembrete"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.gui.carlembrete"
        minSdk = 26
        targetSdk = 35
        versionCode = ciVersionCode ?: 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "FIPE_BASE_URL", "\"$fipeBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            if (isReleaseSigningReady) {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (isReleaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                println("Release signing is not configured; generating unsigned release artifacts.")
            }
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/INDEX.LIST"
            )
        }
    }
    sourceSets {
        getByName("main") {
            res.srcDir("src/main/res-carlogos")
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // BOM do Compose
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Ãcones
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("io.coil-kt:coil-compose:2.5.0")

    // --- TESTES E RELATÃ“RIOS ---
    testImplementation(libs.junit)
    // DependÃªncia do Allure para entender os testes
    testImplementation("io.qameta.allure:allure-junit4:2.29.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // CÃ¢mera (CameraX)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.guava:guava:31.1-android")

    // InteligÃªncia Artificial (Google ML Kit - OCR)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("org.jsoup:jsoup:1.15.4")

    // PermissÃµes
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("com.google.zxing:core:3.5.1")
    // Imagens remotas
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.1.0")

    // Google Drive (App Folder) backup
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.api-client:google-api-client-gson:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230815-2.0.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20240328-2.0.0")
    implementation("com.google.http-client:google-http-client-android:1.43.3")
    implementation("com.google.code.gson:gson:2.11.0")

    // WorkManager (agendamento de backup)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}

