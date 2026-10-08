package ua.wwind.convention.kmp.target

import ua.wwind.convention.util.configureLibraryBrowserTests

plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        browser { configureLibraryBrowserTests(rootDir.resolve("config/karma.config.d/js")) }
        binaries.library()
        // Compose bundles Skiko for browser tests only through an executable's webpack build (CMP-4906).
        binaries.executable()
    }
}
