@file:Suppress("UnstableApiUsage")
package ua.wwind.convention

plugins {
    // No external plugins required here
}

// Ensure applied only to the root project
if (project != rootProject) {
    error("Plugin 'ua.wwind.convention.root' must be applied to the root project only")
}

// Standard repositories for all subprojects (complements settings' repositories)
allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
    }
}

// Aggregation task to run 'check' on all subprojects
val checkAll = tasks.register("checkAll") {
    group = "verification"
    description = "Runs the 'check' task on all subprojects."
    // Lazy dependency to avoid task realization too early
    dependsOn(subprojects.map { "${it.path}:check" })
}
