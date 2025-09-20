package ua.wwind.convention.kmp

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Access to version catalog without importing types
val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

compose.desktop {
    application {
        // Allow overriding by -PdesktopMainClass, defaulting to MainKt
        val mainClassProp: String? = providers.gradleProperty("desktopMainClass").orNull
        val mainClassName: String = mainClassProp?.takeIf { it.isNotBlank() } ?: "MainKt"
        this.mainClass = mainClassName

        nativeDistributions {
            // Package name can be provided via -PdesktopPackageName; fallback to project name
            val pkgName: String = providers.gradleProperty("desktopPackageName").orNull
                ?: project.name
            this.packageName = pkgName
            this.packageVersion = libs.findVersion("version-name").get().requiredVersion

            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
            )
        }
    }
}
