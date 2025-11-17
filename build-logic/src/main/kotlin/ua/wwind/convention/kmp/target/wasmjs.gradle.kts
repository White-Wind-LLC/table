@file:OptIn(ExperimentalWasmDsl::class)

package ua.wwind.convention.kmp.target

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        browser()
        binaries.library()
    }
}
