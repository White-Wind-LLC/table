plugins {
    id("ua.wwind.convention.kmp.library")
    id("ua.wwind.convention.kmp.target.all")
    id("ua.wwind.convention.compose")
    id("ua.wwind.convention.publishing")
    id("ua.wwind.convention.logging")
    id("ua.wwind.convention.test")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // DrawableResource is the public type of TableIcons, so consumers need it on their classpath.
            api(libs.compose.components.resources)
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

compose.resources {
    packageOfResClass = "ua.wwind.table.generated.resources"
    publicResClass = false
    generateResClass = always
}
