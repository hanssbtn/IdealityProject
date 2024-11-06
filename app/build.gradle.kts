plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    // Existing plugins
    id("com.google.gms.google-services")
    alias(libs.plugins.compose.compiler)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

kapt {
    correctErrorTypes = true
}

composeCompiler {
    includeSourceInformation = true
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("/app/src/main/java/resources/stability_config.conf")
}

android {
    namespace = "com.ideality.idealityproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ideality.idealityproject"
        minSdk = 29
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
    }
    dataBinding {
        enable = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}


dependencies {
    // Dagger / Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.android.compiler)

    // Google Fonts
    implementation(libs.androidx.ui.text.google.fonts)
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    // Jetpack Compose + Material3
    androidTestImplementation(composeBom)
    implementation(libs.androidx.material3)
    // Jetpack Compose Preview
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    // Jetpack Compose UI test
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Jetpack Compose Add full set of material icons
    implementation(libs.androidx.material.icons.extended)
    // Jetpack Compose Add window size utils
    implementation(libs.androidx.adaptive)
    // Jetpack Compose Integration with activities
    implementation(libs.androidx.activity.compose)
    // Jetpack Compose Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Jetpack Compose Integration with LiveData
    implementation(libs.androidx.runtime.livedata)
    // ConstraintLayout
    implementation(libs.androidx.constraintlayout.compose)

    // ARCore
    implementation(libs.core)
    // SceneView
    implementation(libs.sceneview)
    // ARSceneView
    implementation(libs.arsceneview)
    //

    // Google Services
    implementation(libs.google.services)

    // Firebase BOM
    //noinspection UseTomlInstead
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    // Firebase Core
    implementation(libs.firebase.core)
    // Firebase Auth
    implementation(libs.firebase.auth.ktx)
    // Firebase Admin (delete later)
    // FirebaseUI Auth
    implementation(libs.androidx.credentials.play.services.auth)
    // Firebase Firestore
    implementation(libs.firebase.firestore.ktx)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.navigation.testing)

    // AndroidX KTX
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.core.ktx)

    // Core libs
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
