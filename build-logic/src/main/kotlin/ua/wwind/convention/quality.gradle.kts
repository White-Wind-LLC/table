@file:Suppress("UnstableApiUsage")

package ua.wwind.convention

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    // No versions here; provided by build-logic classpath
}

// Ensure applied only to the root project
if (project != rootProject) {
    error("Plugin 'ua.wwind.convention.quality' must be applied to the root project only")
}

subprojects {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    // Ktlint defaults
    extensions.configure<KtlintExtension> {
        android.set(false)
        verbose.set(true)
        outputToConsole.set(true)
        // Use a shared baseline file at the root (one baseline for the whole repo)
        baseline.set(rootProject.file(".ktlint-baseline.xml"))
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }

    // Detekt defaults
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        val customConfig = rootProject.file("config/detekt/detekt.yml")
        if (customConfig.exists()) {
            config.setFrom(files(customConfig))
        }
        basePath = rootDir.absolutePath
    }
}

// Aggregated quality task
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs Detekt and Ktlint checks on all subprojects."
    dependsOn(subprojects.map { "${it.path}:detekt" })
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
}
