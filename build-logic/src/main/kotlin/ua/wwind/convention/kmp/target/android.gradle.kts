package ua.wwind.convention.kmp.target

import ua.wwind.convention.util.computeValidatedNamespace

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    androidTarget()
}

android {
    compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
    val androidNamespace: String = project.computeValidatedNamespace(
        explicitPropertyNames = listOf("androidNamespace"),
        basePropertyName = "baseNamespace",
        subjectLabel = "Android namespace",
    )
    namespace = androidNamespace

    defaultConfig {
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
    }

    sourceSets.getByName("main").apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/composeResources")
        res.srcDirs("src/commonMain/composeResources", "src/androidMain/res")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
