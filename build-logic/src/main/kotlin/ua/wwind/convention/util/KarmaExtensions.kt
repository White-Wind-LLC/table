package ua.wwind.convention.util

import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.io.File

// Kept in build logic rather than inlined in a build script: the callbacks are stored on the test
// task, and a lambda written in a `*.gradle.kts` file captures the script object, which the
// configuration cache cannot serialize.

/**
 * Configures a Kotlin/JS or Kotlin/Wasm browser target: webpack output name without source maps
 * (avoids Safari warnings about invalid sourcesContent), and Karma tests on Chrome and headless Chrome.
 *
 * No Firefox: its launcher would add npm packages that the library modules lack, and the committed
 * yarn locks must match builds with and without the samples.
 */
fun KotlinJsBrowserDsl.configureSampleBrowser(outputFileName: String) {
    commonWebpackConfig {
        this.outputFileName = outputFileName
        sourceMaps = false
        devServer = devServer?.copy() ?: KotlinWebpackConfig.DevServer()
    }
    testTask {
        useKarma {
            useChrome()
            useChromeHeadless()
        }
    }
}

/** Runs a library's Kotlin/JS browser tests in headless Chrome with the Karma config files in [configDirectory]. */
fun KotlinJsBrowserDsl.configureLibraryBrowserTests(configDirectory: File) {
    testTask {
        useKarma {
            useChromeHeadless()
            useConfigDirectory(configDirectory)
        }
    }
}
