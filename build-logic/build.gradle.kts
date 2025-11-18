plugins {
    `version-catalog`
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

dependencies {
    // Gradle API for better IDE support
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    implementation(libs.gradle.kotlin.multiplatform)
    implementation(libs.gradle.android)
    implementation(libs.gradle.android.library)
    implementation(libs.gradle.compose)
    implementation(libs.gradle.kotlin.compose.compiler)
    implementation(libs.gradle.stability.analyzer)
    implementation(libs.gradle.kotlin.allopen)
    implementation(libs.gradle.buildkonfig)
    // Quality & Coverage plugins available to precompiled scripts
    implementation(libs.gradle.detekt)
    implementation(libs.gradle.ktlint)
    implementation(libs.gradle.kover)
    implementation(libs.gradle.dokka)
    implementation(libs.gradle.maven.publish)
}

// Configure Kotlin compiler for better IDE support
kotlin {
    jvmToolchain(17)
}
