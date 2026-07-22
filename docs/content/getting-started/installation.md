# Installation

Add repository (usually `mavenCentral`) and include the modules you need:

```kotlin
dependencies {
    implementation("ua.wwind.table-kmp:table-core:2.1.0")
    // optional
    implementation("ua.wwind.table-kmp:table-format:2.1.0")
    implementation("ua.wwind.table-kmp:table-paging:2.1.0")
}
```

The project uses `kotlinx-collections-immutable` for all table/state collections to ensure predictable, thread-safe
state management and efficient Compose recomposition:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:<latest-version>")
}
```

The table API is stable — no opt-in annotation is required. Upgrading from 1.x? See the
[2.0 migration guide](migration-2.0.md).
