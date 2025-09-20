plugins {
    id("ua.wwind.convention.kmp.library")
    id("ua.wwind.convention.kmp.target.all")
    id("ua.wwind.convention.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.dnd)
            implementation(libs.reorderable)
            implementation(libs.kotlinx.datetime)
        }
    }
}
