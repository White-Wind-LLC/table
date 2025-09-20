package ua.wwind.convention.kmp.target

plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        browser()
        binaries.library()
    }
}
