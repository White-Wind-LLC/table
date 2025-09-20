package ua.wwind.convention

// Needs to exist before the first usage of 'libs'
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
}

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("coroutines-test").get())
                implementation(libs.findLibrary("turbine-turbine").get())
                implementation(libs.findLibrary("assertk").get())
            }
        }
    }
}
