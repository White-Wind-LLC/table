package ua.wwind.convention.kmp

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("multiplatform")
}

// Configure project coordinates from centralized properties/catalog
val baseNamespace: String = providers.gradleProperty("baseNamespace").get()
// Allow overriding the Maven group independently of the base namespace (Android namespace derivation)
val mavenGroup: String? = providers.gradleProperty("mavenGroup").orNull
group = mavenGroup ?: baseNamespace

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val versionName: String = libs.findVersion("version-name").get().requiredVersion
version = versionName

kotlin {
    // Default JVM toolchain to 17 for Android-facing and general JVM targets.
    // Use JVM 21 only where explicitly required (e.g., if a specific module/tooling demands it).
    jvmToolchain(17)

    applyDefaultHierarchyTemplate()

    // Centralized Kotlin compiler flags and opt-ins
    val warningsAsErrors: String? by project
    compilerOptions {
        allWarningsAsErrors.set(warningsAsErrors.toBoolean())
    }
}

// Ensure all Kotlin JVM compilations target JVM 17 bytecode by default
tasks.withType(KotlinJvmCompile::class.java).configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
