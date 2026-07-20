// Thin Android application wrapper around the multiplatform :table-sample module.
// AGP 9 no longer allows com.android.application in the same module as a Kotlin Multiplatform
// target, so this module holds only the manifest; MainActivity lives in table-sample/androidMain.
plugins {
    id("ua.wwind.convention.kmp.android-app")
}

dependencies {
    implementation(project(":table-sample"))
}
