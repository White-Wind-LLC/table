package ua.wwind.table.filter.data

import ua.wwind.table.strings.UiString

public enum class BooleanType {
    TRUE,
    FALSE,
}

public fun BooleanType.toUiString(): UiString =
    when (this) {
        BooleanType.TRUE -> UiString.BooleanTrueTitle
        BooleanType.FALSE -> UiString.BooleanFalseTitle
    }
