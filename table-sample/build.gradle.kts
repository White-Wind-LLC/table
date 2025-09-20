@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("ua.wwind.convention.kmp.android-app")
    id("ua.wwind.convention.kmp.desktop-app")
    id("ua.wwind.convention.compose")
    id("ua.wwind.convention.coroutines")
}

val baseNamespace: String by project
val androidApplicationId: String by project

val desktopPackageName: String by project

val jsOutputModuleName: String by project
val jsOutputFileName: String by project

val iosFrameworkBaseName: String by project

val buildKonfigPackage: String by project

kotlin {
    androidTarget()

    jvm()

    js {
        outputModuleName = jsOutputModuleName
        browser {
            commonWebpackConfig {
                outputFileName = jsOutputFileName
                // Disable webpack source maps to avoid Safari warnings about invalid sourcesContent
                sourceMaps = false
                devServer = devServer?.copy() ?: KotlinWebpackConfig.DevServer()
            }
            testTask {
                useKarma {
                    useChrome()
                    useChromeHeadless()
                    useFirefox()
                }
            }
        }
        // Also disable Kotlin/JS compiler source maps to prevent inlined data source maps
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap.set(false)
        }
        binaries.executable()
    }

    wasmJs {
        outputModuleName = jsOutputModuleName
        browser {
            commonWebpackConfig {
                outputFileName = jsOutputFileName
                // Disable webpack source maps to avoid Safari warnings about invalid sourcesContent
                sourceMaps = false
                devServer = devServer?.copy() ?: KotlinWebpackConfig.DevServer()
            }
            testTask {
                useKarma {
                    useChrome()
                    useChromeHeadless()
                    useFirefox()
                }
            }
        }
        // Also disable Kotlin/JS compiler source maps to prevent inlined data source maps
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap.set(false)
        }
        binaries.executable()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = iosFrameworkBaseName
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":table-core"))
            implementation(project(":table-format"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.annotations.common)
            implementation(libs.assertk)
            implementation(libs.coroutines.test)
            implementation(libs.turbine.turbine)

            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        jvmMain.dependencies {
            // implementation(libs.firebase.analytics)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.okio)
        }

        jsMain.dependencies {
            // implementation(libs.firebase.analytics)
            implementation(compose.html.core)
        }

        iosMain.dependencies {
            // implementation(libs.firebase.analytics)
        }

    }
}
