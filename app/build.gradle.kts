import java.util.Properties

val releaseSigningPropertiesFile =
    file("${System.getProperty("user.home")}/.config/ble-signal-analyzer/signing.properties")

val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.inputStream().use(::load)
    }
}

val expectedReleaseKeyAlias = "ble_signal_upload"
val releaseSigningKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val releaseSigningConfigured =
    releaseSigningPropertiesFile.isFile &&
        releaseSigningKeys.all { releaseSigningProperties.getProperty(it)?.isNotBlank() == true }

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
        versionCode = 5
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requiredReleaseSigningProperty("storeFile"))
                storePassword = requiredReleaseSigningProperty("storePassword")
                keyAlias = requiredReleaseSigningProperty("keyAlias")
                keyPassword = requiredReleaseSigningProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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

tasks.register("verifyReleaseSigning") {
    inputs.property("releaseSigningConfigured", releaseSigningConfigured)
    inputs.property("releaseSigningPropertiesPath", releaseSigningPropertiesFile.absolutePath)
    inputs.property("releaseKeyAlias", releaseSigningProperties.getProperty("keyAlias").orEmpty())
    inputs.property("expectedReleaseKeyAlias", expectedReleaseKeyAlias)
    doLast {
        val configured = inputs.properties["releaseSigningConfigured"] as Boolean
        val propertiesPath = inputs.properties["releaseSigningPropertiesPath"] as String
        val alias = inputs.properties["releaseKeyAlias"] as String
        val expectedAlias = inputs.properties["expectedReleaseKeyAlias"] as String
        check(configured) {
            "BLE Signal Analyzer release signing requires a complete secure file at $propertiesPath"
        }
        check(alias == expectedAlias) {
            "BLE Signal Analyzer release signing alias must be $expectedAlias, found $alias"
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn("verifyReleaseSigning")
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
