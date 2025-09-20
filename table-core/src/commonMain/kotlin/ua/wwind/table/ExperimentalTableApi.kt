package ua.wwind.table

import kotlin.RequiresOptIn
import kotlin.annotation.Retention

@Suppress("ExperimentalAnnotationRetention")
@RequiresOptIn(
    message = "This table library API is experimental and may change without notice. Opt in with @OptIn(ExperimentalTableApi::class).",
    level = RequiresOptIn.Level.ERROR,
)
@MustBeDocumented
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalTableApi
