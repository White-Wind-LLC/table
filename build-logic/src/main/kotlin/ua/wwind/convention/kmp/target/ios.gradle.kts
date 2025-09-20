package ua.wwind.convention.kmp.target

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
}

// Determine whether to include `iosX64` (Intel simulator) target.
// Default: disable on Apple Silicon to avoid unnecessary fat framework tasks and issues.
private val includeIosX64: Boolean by lazy {
    val prop = providers.gradleProperty("enableIosX64").orNull
    val isAppleSilicon = OperatingSystem.current().isMacOsX && System.getProperty("os.arch") == "aarch64"
    prop?.toBoolean() ?: !isAppleSilicon
}

kotlin {
    val iosTargets = buildList {
        if (includeIosX64) add(iosX64())
        add(iosArm64())
        add(iosSimulatorArm64())
    }

    val xcFramework = XCFramework()

    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = project.name
                .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
                .replace("-", "_")
            isStatic = true

            // Package into XCFramework (recommended for iOS)
            xcFramework.add(this)
        }
    }
}

// Disable legacy fat framework tasks to prevent accidental execution
// (e.g., `linkDebugFrameworkIosFat`) which can fail if some slices are not present.
tasks.matching { it.name.contains("IosFat") }.configureEach {
    enabled = false
}
