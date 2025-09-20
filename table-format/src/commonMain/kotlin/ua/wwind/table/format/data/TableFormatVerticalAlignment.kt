package ua.wwind.table.format.data

import ua.wwind.table.strings.UiString

public enum class TableFormatVerticalAlignment(
    public val uiString: UiString,
) {
    TOP(UiString.FormatVerticalAlignmentTop),
    CENTER(UiString.FormatVerticalAlignmentCenter),
    BOTTOM(UiString.FormatVerticalAlignmentBottom),
}
