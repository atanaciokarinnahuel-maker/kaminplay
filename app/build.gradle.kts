import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Firebase is optional at build time. Without app/google-services.json the APK still
// builds and the login screen reports that Firebase is not connected yet.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Google sign-in is tied to the SHA-1 of the signing key, so CI signs every build
// (debug included) with one shared keystore supplied through environment variables.
val keystorePath: String? = System.getenv("KAMIN_KEYSTORE_FILE")
val hasSharedKeystore = keystorePath != null && file(keystorePath).exists()

android {
    namespace = "com.karin.kaminplay"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.karin.kaminplay"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasSharedKeystore) {
            create("shared") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KAMIN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KAMIN_KEY_ALIAS")
                keyPassword = System.getenv("KAMIN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (hasSharedKeystore) signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            if (hasSharedKeystore) signingConfig = signingConfigs.getByName("shared")
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
}
