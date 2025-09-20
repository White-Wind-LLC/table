@file:Suppress("UnstableApiUsage")

package ua.wwind.convention

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

plugins {
    // No versions here; provided by build-logic classpath
    id("org.jetbrains.kotlinx.kover")
}

// Ensure applied only to the root project
if (project != rootProject) {
    error("Plugin 'ua.wwind.convention.coverage' must be applied to the root project only")
}

// Apply Kover to all subprojects
subprojects {
    pluginManager.apply("org.jetbrains.kotlinx.kover")
}

// Configure Kover reporting and exclusions for typical non-production classes
extensions.configure(KoverProjectExtension::class.java) {
    reports {
        // Common filters for all report variants
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*.*Test*",
                    "*.*Fake*",
                    "*.*Mock*",
                    // Moko resources generated classes
                    "*.MR",
                    "*.MR$*",
                    // Compose previews or preview-only holders
                    "*.*Preview*"
                )
            }
        }
    }
}

// Add all subprojects to merged coverage for root reports
dependencies {
    subprojects.forEach { sub ->
        add("kover", project(sub.path))
    }
}

// Aggregated coverage task that produces merged HTML and XML reports
tasks.register("coverageReport") {
    group = "verification"
    description = "Generates merged code coverage reports (HTML and XML) for all subprojects."
    dependsOn("koverHtmlReport", "koverXmlReport")
}
