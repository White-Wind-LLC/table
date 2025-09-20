package ua.wwind.convention

// Needs to exist before the first usage of 'libs'
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Lifecycle ViewModel integration for Compose (KMP flavor)
                implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            }
        }
    }
}
