plugins {
    id("ua.wwind.convention.kmp.library")
    id("ua.wwind.convention.kmp.target.all")
    id("ua.wwind.convention.compose")
    id("ua.wwind.convention.logging")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":table-core"))
            implementation(libs.paging.core)
        }
    }
}
