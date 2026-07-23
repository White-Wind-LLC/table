plugins {
    id("ua.wwind.convention.kmp.library")
    id("ua.wwind.convention.kmp.target.all")
    id("ua.wwind.convention.compose")
    id("ua.wwind.convention.publishing")
    id("ua.wwind.convention.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Public API references types from table-core (e.g., StringProvider), so expose as API
            api(project(":table-core"))

            // UI helpers used internally by the formatting dialog/components
            implementation(libs.colorpicker)
            implementation(libs.reorderable)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.compose.ui.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
