@file:Suppress("UnstableApiUsage")

package ua.wwind.convention

import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    // No versions here; provided by build-logic classpath
}

// Ensure applied only to the root project
if (project != rootProject) {
    error("Plugin 'ua.wwind.convention.quality' must be applied to the root project only")
}

// :table-sample-android is a "pure" Android entrypoint module (manifest only, no Kotlin sources).
// Detekt hooks into the Kotlin/Android plugin lifecycle and registers no `detekt` task there, so it
// is skipped entirely and excluded from the aggregate task below.
val hasKotlinSources: (Project) -> Boolean = { it.path != ":table-sample-android" }

subprojects {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    if (hasKotlinSources(project)) {
        pluginManager.apply("dev.detekt")
    }

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
    if (hasKotlinSources(project)) {
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            val customConfig = rootProject.file("config/detekt/detekt.yml")
            if (customConfig.exists()) {
                config.setFrom(files(customConfig))
            }
            basePath.set(rootDir)
        }
    }
}

// Aggregated quality task
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs Detekt and Ktlint checks on all subprojects."
    dependsOn(subprojects.filter(hasKotlinSources).map { "${it.path}:detekt" })
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
}
