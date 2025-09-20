includeBuild("build-logic")

val rootProjectName: String = providers.gradleProperty("rootProjectName").get()

rootProject.name = rootProjectName

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("table-core")
include("table-format")
include("table-paging")

// Conditionally include samples to avoid configuring/running its tasks in CI/publish
val excludeSamples: Boolean = gradle.startParameter.projectProperties["excludeSamples"]?.toBoolean() ?: false
if (!excludeSamples) {
    include("table-sample")
}
