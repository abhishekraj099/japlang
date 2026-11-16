import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The release key lives outside version control, in keystore.properties next to settings.gradle.kts
// (storeFile, storePassword, keyAlias, keyPassword). See docs/RELEASE.md for how to create it.
val keystore = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "com.example.jap"
    // Recordings in assets/audio are played straight from the APK, which needs them stored uncompressed.
    androidResources {
        noCompress += listOf("m4a", "mp3", "ogg", "opus")
    }
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // The id Google Play knows the app by; it cannot change after the first upload.
        applicationId = "com.manabi.learnjapanese"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // Older and vendor-modified installers still look for the v1 (JAR) signature.
            enableV1Signing = true
            enableV2Signing = true
        }
        if (keystore != null) {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            // Shrinks and optimises code and drops unused resources (R8); keep rules are in proguard-rules.pro.
            optimization {
                enable = true
                keepRules {
                    files.add(project.file("proguard-rules.pro"))
                }
            }
            // Signed with the release key when keystore.properties exists. Without it the build still works,
            // signed with the debug key, for sharing test copies; Google Play will not accept that one.
            signingConfig = if (keystore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.mlkit.digital.ink)
    implementation(libs.youtube.player)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    testImplementation(libs.junit)
    // The real org.json, so unit tests can load the content files (Android's copy is a stub off-device).
    testImplementation(libs.org.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}