@file:Suppress("UnstableApiUsage")

package ua.wwind.convention

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension

// Needs to exist before the first usage of 'libs'
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

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

val ktlintVersion: String = libs.findVersion("ktlint").get().requiredVersion

// ktlint reads .editorconfig itself, but Spotless caches formatter state per-step: passing the
// settings explicitly keeps the formatter reproducible even if a stray .editorconfig appears in a
// subdirectory. Keep this map in sync with the repo-root .editorconfig.
val ktlintEditorConfig: Map<String, String> =
    mapOf(
        "ktlint_code_style" to "ktlint_official",
        "max_line_length" to "120",
        "ij_kotlin_name_count_to_use_star_import" to "999",
        "ij_kotlin_name_count_to_use_star_import_for_members" to "999",
        "ij_kotlin_packages_to_use_import_on_demand" to "",
        // @Composable functions are PascalCase by convention.
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    )

fun SpotlessExtension.configureKotlin() {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintEditorConfig)
    }
}

fun SpotlessExtension.configureGradleKotlinDsl(targets: List<String>) {
    kotlinGradle {
        target(targets)
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
}

// Root project owns the build scripts that live outside any subproject (root + included build-logic).
pluginManager.apply("com.diffplug.spotless")
extensions.configure<SpotlessExtension> {
    configureKotlin()
    configureGradleKotlinDsl(
        listOf(
            "*.gradle.kts",
            "gradle/*.gradle.kts",
            "build-logic/**/*.gradle.kts",
        ),
    )
}

subprojects {
    pluginManager.apply("com.diffplug.spotless")
    if (hasKotlinSources(project)) {
        pluginManager.apply("dev.detekt")
    }

    extensions.configure<SpotlessExtension> {
        configureKotlin()
        configureGradleKotlinDsl(listOf("*.gradle.kts", "gradle/*.gradle.kts"))
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
            // Baselines stay at detekt's own defaults: one file per source set,
            // <module>/detekt-baseline-<sourceSet>.xml. A missing file is ignored, so source sets
            // without accumulated debt need no baseline at all. Regenerate via `detektBaselines`.
        }

        // In a KMP project dev.detekt puts the actual sources on per-source-set tasks
        // (detektCommonMainSourceSet, detektJvmMainSourceSet, …) and leaves the aggregate `detekt`
        // task with NO-SOURCE. Without this wiring `qualityCheck` would pass while analysing
        // nothing at all. The `detekt<Compilation><Target>` variants (type resolution) are left out
        // on purpose: they require a full Kotlin compile, and no rule we enable needs types.
        tasks.named("detekt") {
            dependsOn(tasks.withType<Detekt>().matching { it.name.endsWith("SourceSet") })
        }
    }
}

// Regenerates every per-source-set baseline (<module>/detekt-baseline-<sourceSet>.xml). The bare
// `detektBaseline` task is not usable here: like `detekt`, it holds no sources in a KMP project and
// expects an explicit --baseline path.
tasks.register("detektBaselines") {
    group = "verification"
    description = "Regenerates the detekt baseline of every source set in every subproject."
    dependsOn(
        subprojects.filter(hasKotlinSources).map { subproject ->
            subproject.tasks.withType<DetektCreateBaselineTask>().matching { it.name.endsWith("SourceSet") }
        },
    )
}

// Aggregated quality task
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs Detekt and Spotless checks on the root project and all subprojects."
    dependsOn(subprojects.filter(hasKotlinSources).map { "${it.path}:detekt" })
    dependsOn(subprojects.map { "${it.path}:spotlessCheck" })
    dependsOn(":spotlessCheck")
}
