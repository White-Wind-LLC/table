package ua.wwind.convention.util

import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

/**
 * Configures a Kotlin/JS or Kotlin/Wasm browser target: webpack output name without source maps
 * (avoids Safari warnings about invalid sourcesContent), and Karma tests on Chrome, headless Chrome
 * and Firefox.
 *
 * Kept in build logic rather than inlined in a build script: the callbacks are stored on the test
 * task, and a lambda written in a `*.gradle.kts` file captures the script object, which the
 * configuration cache cannot serialize.
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
            useFirefox()
        }
    }
}
