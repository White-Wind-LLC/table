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
                implementation(libs.findLibrary("kermit").get())
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.findLibrary("slf4j-api").get())
                implementation(libs.findLibrary("slf4j-log4j12").get())
            }
        }
    }
}
