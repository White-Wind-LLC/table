package ua.wwind.convention.util

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Builds a Java/Kotlin package-like namespace derived from the Gradle project path and a base namespace.
 * Example: baseNamespace = "ua.wwind.wms" and project path = ":feature:profile:ui"
 * Result: "ua.wwind.wms.feature.profile.ui"
 */
fun Project.deriveNamespaceFromPath(basePropertyName: String = "baseNamespace"): String? {
    val base: String = providers.gradleProperty(basePropertyName).orNull
        ?.trim()
        ?.trimEnd('.')
        ?: return null

    val segments: List<String> = path
        .trimStart(':')
        .split(":")
        .filter { it.isNotBlank() }
        .map { segment ->
            // Only Java package-safe characters; replace others with underscore
            segment.replace(Regex("[^A-Za-z0-9_]"), "_")
        }

    val suffix: String = segments.joinToString(separator = ".")
    return if (suffix.isBlank()) base else "$base.$suffix"
}

/**
 * Computes a namespace using explicit override properties or falls back to [deriveNamespaceFromPath].
 * Validates the result against Java package rules. Throws if nothing is provided or invalid.
 */
fun Project.computeValidatedNamespace(
    explicitPropertyNames: List<String> = listOf("androidNamespace"),
    basePropertyName: String = "baseNamespace",
    subjectLabel: String = "namespace",
): String {
    val explicit: String? = explicitPropertyNames.asSequence()
        .mapNotNull { name -> providers.gradleProperty(name).orNull ?: findProperty(name)?.toString() }
        .firstOrNull()

    val derived: String? = deriveNamespaceFromPath(basePropertyName)
    val result: String = explicit ?: derived ?: throw GradleException(
        "Missing required $subjectLabel for module $path. " +
                "Provide '-P${explicitPropertyNames.first()}=your.package.name' or set a base with '-P$basePropertyName=your.base' to derive from project path."
    )

    val isValid: Boolean = result.matches(Regex("^[a-zA-Z][A-Za-z0-9_]*(\\.[a-zA-Z][A-Za-z0-9_]*)*$"))
    if (!isValid) {
        throw GradleException(
            "Invalid $subjectLabel '$result' in module $path. Specify a valid '-P${explicitPropertyNames.first()}' or adjust '-P$basePropertyName'."
        )
    }
    return result
}
