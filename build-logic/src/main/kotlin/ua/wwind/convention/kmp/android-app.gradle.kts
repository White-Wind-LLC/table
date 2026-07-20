package ua.wwind.convention.kmp

import com.android.build.api.dsl.ManagedVirtualDevice
import ua.wwind.convention.util.computeValidatedNamespace

// Access to version catalog
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Pure Android application module. AGP 9 removed Kotlin Multiplatform compatibility from
// com.android.application, so the shared multiplatform code lives in a separate module applying
// 'ua.wwind.convention.kmp.target.android' and is consumed from here as a regular dependency.
// Kotlin support is built into AGP 9, so no Kotlin plugin is applied.
plugins {
    id("com.android.application")
}

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

    // Standard Android layout (the MPP layout belongs to the multiplatform module)
    sourceSets.getByName("main").apply {
        manifest.srcFile("src/main/AndroidManifest.xml")
        res.srcDirs("src/main/res")
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

    // Avoid language split in bundles by default
    bundle {
        language {
            enableSplit = false
        }
    }
}
