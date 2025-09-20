package ua.wwind.convention.kmp

import com.android.build.api.dsl.ManagedVirtualDevice
import ua.wwind.convention.util.computeValidatedNamespace

// Access to version catalog
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
    id("com.android.application")
}

kotlin { androidTarget() }

val androidApplicationId: String by project

android {
    // SDK versions from version catalog
    compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()

    // Namespace derived or provided explicitly via -PandroidNamespace or -PbaseNamespace
    val androidNamespace: String = project.computeValidatedNamespace(
        explicitPropertyNames = listOf("androidNamespace"),
        basePropertyName = "baseNamespace",
        subjectLabel = "Android namespace",
    )
    namespace = androidNamespace

    defaultConfig {
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
        targetSdk = libs.findVersion("android-targetSdk").get().requiredVersion.toInt()

        // Application ID may be provided explicitly via -PandroidApplicationId, otherwise fall back to namespace
        applicationId = androidApplicationId

        versionCode = libs.findVersion("android-version-code").get().requiredVersion.toInt()
        versionName = libs.findVersion("version-name").get().requiredVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Align source sets with MPP layout
    sourceSets.getByName("main").apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/composeResources")
        res.srcDirs("src/commonMain/composeResources", "src/androidMain/res")
    }

    buildTypes {
        maybeCreate("release").apply {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Safe default; projects can override with proper signing
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Managed devices for instrumentation tests (can be overridden per-module)
    @Suppress("UnstableApiUsage")
    testOptions {
        managedDevices.allDevices {
            maybeCreate<ManagedVirtualDevice>("pixel5").apply {
                device = "Pixel 5"
                apiLevel = libs.findVersion("android-targetSdk").get().requiredVersion.toInt()
                systemImageSource = "aosp"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Enable Compose build features for Android app modules
    buildFeatures {
        compose = true
    }

    // Avoid language split in bundles by default
    bundle {
        language {
            enableSplit = false
        }
    }
}
