package ua.wwind.table

import kotlin.RequiresOptIn
import kotlin.annotation.Retention

/**
 * Marked the table API while it was still settling. Nothing in the library carries it as of 2.0.0 —
 * the API is stable and needs no opt-in.
 *
 * The annotation itself is kept for one major release so that an existing
 * `@OptIn(ExperimentalTableApi::class)` still compiles; delete those opt-ins and this marker goes
 * away with the next major.
 */
@Deprecated(
    "The table API is stable as of 2.0.0 and requires no opt-in. " +
        "Remove the @OptIn(ExperimentalTableApi::class) annotation from the call site. " +
        "This marker will be removed in the next major release.",
    level = DeprecationLevel.WARNING,
)
@Suppress("ExperimentalAnnotationRetention")
@RequiresOptIn(
    message =
        "This table library API is experimental and may change without notice. " +
            "Opt in with @OptIn(ExperimentalTableApi::class).",
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
