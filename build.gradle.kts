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
    alias(libs.plugins.stability.analyzer) apply false
}

allprojects {
    // Apply Dokka to root and all subprojects so documentation can be generated across modules
    apply(plugin = "org.jetbrains.dokka")
}

// Aggregate the three public library modules into a single Dokka HTML site at build/dokka/html/.
// table-sample is the demo app (not public API) and is intentionally excluded.
// Uses string-based add() because the dokka plugin is applied via allprojects{} (not directly
// on root), so the typed dokka() accessor is not available in the root script.
dependencies {
    add("dokka", project(":table-core"))
    add("dokka", project(":table-paging"))
    add("dokka", project(":table-format"))
}
