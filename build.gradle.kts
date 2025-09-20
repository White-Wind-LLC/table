plugins {
    id("ua.wwind.convention.root")
    id("ua.wwind.convention.quality")
    id("ua.wwind.convention.coverage")
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.buildKonfig).apply(false)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hot.reload).apply(false)
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    // Apply Dokka to root and all subprojects so documentation can be generated across modules
    apply(plugin = "org.jetbrains.dokka")
}
