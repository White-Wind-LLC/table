package ua.wwind.table.filter.data

import ua.wwind.table.strings.UiString

/** Supported filter comparison operators. */
public enum class FilterConstraint {
    EQUALS,
    NOT_EQUALS,
    BETWEEN,
    CONTAINS,
    IN,
    STARTS_WITH,
    ENDS_WITH,
    NOT_IN,
    GT,
    GTE,
    LT,
    LTE,
    IS_NULL,
    IS_NOT_NULL,
    ;

    /**
     * Map constraint to a UI string key.
     *
     * One exhaustive arm per constant, each a bare reference. The arms do not interact, so the
     * complexity count is the size of this enum rather than anything a reader has to follow —
     * `CyclomaticComplexMethod` is suppressed rather than fixed. Keeping the mapping here instead
     * of on the constants themselves keeps `filter.data` free of a `strings` dependency.
     */
    @Suppress("CyclomaticComplexMethod")
    public fun toUiString(): UiString =
        when (this) {
            EQUALS -> UiString.FilterConstraintEquals
            NOT_EQUALS -> UiString.FilterConstraintNotEquals
            BETWEEN -> UiString.FilterConstraintBetween
            CONTAINS -> UiString.FilterConstraintContains
            IN -> UiString.FilterConstraintIn
            STARTS_WITH -> UiString.FilterConstraintStartsWith
            ENDS_WITH -> UiString.FilterConstraintEndsWith
            NOT_IN -> UiString.FilterConstraintNotIn
            GT -> UiString.FilterConstraintGt
            GTE -> UiString.FilterConstraintGte
            LT -> UiString.FilterConstraintLt
            LTE -> UiString.FilterConstraintLte
            IS_NULL -> UiString.FilterConstraintIsNull
            IS_NOT_NULL -> UiString.FilterConstraintIsNotNull
        }
}

/** True when constraint checks null/empty semantics. */
public fun FilterConstraint.isNullCheck(): Boolean =
    this == FilterConstraint.IS_NULL || this == FilterConstraint.IS_NOT_NULL
