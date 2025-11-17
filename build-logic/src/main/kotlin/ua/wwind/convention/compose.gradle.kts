package ua.wwind.convention

import org.jetbrains.compose.ComposeExtension

// Needs to exist before the first usage of 'libs'
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.github.skydoves.compose.stability.analyzer")
}

val composeDeps = extensions.getByType<ComposeExtension>().dependencies

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(composeDeps.runtime)
                implementation(composeDeps.foundation)
                implementation(composeDeps.material3)
                implementation(composeDeps.materialIconsExtended)
                implementation(composeDeps.components.uiToolingPreview)
                implementation(libs.findLibrary("collections-immutable").get())
            }
        }

        androidMain {
            dependencies {
                implementation(libs.findLibrary("androidx.activity.compose").get())
                implementation(composeDeps.preview)
            }
        }
    }
}