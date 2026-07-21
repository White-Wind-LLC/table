package ua.wwind.convention.kmp.target

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import ua.wwind.convention.util.computeValidatedNamespace

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
    // AGP 9 dropped KMP support from com.android.library; this is its multiplatform replacement.
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace =
            project.computeValidatedNamespace(
                explicitPropertyNames = listOf("androidNamespace"),
                basePropertyName = "baseNamespace",
                subjectLabel = "Android namespace",
            )
        compileSdk =
            libs
                .findVersion("android-compileSdk")
                .get()
                .requiredVersion
                .toInt()
        minSdk =
            libs
                .findVersion("android-minSdk")
                .get()
                .requiredVersion
                .toInt()

        // Required so Compose composeResources keep packaging into consumers' APKs (CMP-9547)
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
