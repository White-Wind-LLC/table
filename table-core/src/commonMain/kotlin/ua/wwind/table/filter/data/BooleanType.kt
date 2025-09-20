package ua.wwind.table.filter.data

import ua.wwind.table.strings.UiString

/** Boolean values used by the Boolean filter UI. */
public enum class BooleanType {
    TRUE,
    FALSE,
}

/** Map [BooleanType] to a UI string key. */
public fun BooleanType.toUiString(): UiString =
    when (this) {
        BooleanType.TRUE -> UiString.BooleanTrueTitle
        BooleanType.FALSE -> UiString.BooleanFalseTitle
    }
