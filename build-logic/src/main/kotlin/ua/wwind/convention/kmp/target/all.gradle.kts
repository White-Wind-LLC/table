package ua.wwind.convention.kmp.target

// Apply platform plugins conditionally based on Gradle properties (default: enabled)
// Supported properties:
// - enableAndroid
// - enableIos
// - enableJvm
// - enableJs
// - enableWasmJs

val isEnabled: (String) -> Boolean = { propName ->
    // If property is not set, return true (enabled by default)
    (providers.gradleProperty(propName).orNull)?.toBoolean() ?: true
}

if (isEnabled("enableJvm")) {
    pluginManager.apply("ua.wwind.convention.kmp.target.jvm")
}

if (isEnabled("enableJs")) {
    pluginManager.apply("ua.wwind.convention.kmp.target.js")
}

if (isEnabled("enableWasmJs")) {
    pluginManager.apply("ua.wwind.convention.kmp.target.wasmjs")
}

if (isEnabled("enableAndroid")) {
    pluginManager.apply("ua.wwind.convention.kmp.target.android")
}

if (isEnabled("enableIos")) {
    pluginManager.apply("ua.wwind.convention.kmp.target.ios")
}
