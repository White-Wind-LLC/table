package ua.wwind.convention

// Needs to exist before the first usage of 'libs'
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.findLibrary("compose-runtime").get())
                implementation(libs.findLibrary("compose-foundation").get())
                implementation(libs.findLibrary("compose-material3").get())
                implementation(libs.findLibrary("compose-material-icons-extended").get())
                implementation(libs.findLibrary("compose-ui-tooling-preview").get())
                implementation(libs.findLibrary("collections-immutable").get())
            }
        }

        androidMain {
            dependencies {
                implementation(libs.findLibrary("androidx-activity-compose").get())
                implementation(libs.findLibrary("compose-ui-tooling").get())
            }
        }
    }
}
