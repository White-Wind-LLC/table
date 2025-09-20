package ua.wwind.table.format.data

import ua.wwind.table.strings.UiString

public enum class TableFormatHorizontalAlignment(
    public val uiString: UiString,
) {
    START(UiString.FormatHorizontalAlignmentStart),
    CENTER(UiString.FormatHorizontalAlignmentCenter),
    END(UiString.FormatHorizontalAlignmentEnd),
}
