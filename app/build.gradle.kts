import java.util.Properties

val releaseSigningPropertiesFile = file("/home/cliu/.config/ble-signal-analyzer/signing.properties")
if (!releaseSigningPropertiesFile.isFile) {
    throw GradleException(
        "BLE Signal Analyzer release signing requires the secure file at " +
            releaseSigningPropertiesFile.absolutePath,
    )
}

val releaseSigningProperties = Properties().apply {
    releaseSigningPropertiesFile.inputStream().use(::load)
}

fun requiredReleaseSigningProperty(name: String): String =
    releaseSigningProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: throw GradleException(
            "BLE Signal Analyzer release signing property '$name' is missing from " +
                releaseSigningPropertiesFile.absolutePath,
        )

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ble.signal.analyzer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ble.signal.analyzer"
        minSdk = 27
        targetSdk = 37
        versionCode = 4
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(requiredReleaseSigningProperty("storeFile"))
            storePassword = requiredReleaseSigningProperty("storePassword")
            keyAlias = requiredReleaseSigningProperty("keyAlias")
            keyPassword = requiredReleaseSigningProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
