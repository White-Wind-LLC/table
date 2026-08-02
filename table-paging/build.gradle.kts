plugins {
    id("ua.wwind.convention.kmp.library")
    id("ua.wwind.convention.kmp.target.all")
    id("ua.wwind.convention.compose")
    id("ua.wwind.convention.logging")
    id("ua.wwind.convention.publishing")
}

// `paging-core` ships without the Compose compiler plugin, so the compiler infers no stability for
// its types: `PagingData` counted as unstable, and strong skipping falls back to comparing an
// unstable parameter by instance. The pagers publish a fresh snapshot per state change, so the
// paged `Table` overloads recomposed on every emission even when the window they render was
// unchanged. Declaring the types stable puts `items` back on value comparison — see the file for
// why that claim is sound.
composeCompiler {
    stabilityConfigurationFiles.add(
        rootProject.layout.projectDirectory.file("compose_compiler_config.conf"),
    )
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":table-core"))
            implementation(libs.paging.core)
        }
    }
}
