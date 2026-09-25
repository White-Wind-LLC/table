package ua.wwind.convention.kmp.target

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.library()
        // Compose bundles Skiko for browser tests only through an executable's webpack build (CMP-4906).
        binaries.executable()
    }
}
