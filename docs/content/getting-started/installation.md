# Installation

Add repository (usually `mavenCentral`) and include the modules you need:

```kotlin
dependencies {
    implementation("ua.wwind.table-kmp:table-core:1.9.0")
    // optional
    implementation("ua.wwind.table-kmp:table-format:1.9.0")
    implementation("ua.wwind.table-kmp:table-paging:1.9.0")
}
```

The project uses `kotlinx-collections-immutable` for all table/state collections to ensure predictable, thread-safe
state management and efficient Compose recomposition:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:<latest-version>")
}
```

Opt‑in to experimental API on call sites that use the table:

```kotlin
@OptIn(ExperimentalTableApi::class)
@Composable
fun MyScreen() { /* ... */
}
```
