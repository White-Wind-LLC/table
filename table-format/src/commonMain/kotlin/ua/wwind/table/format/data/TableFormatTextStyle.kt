package ua.wwind.table.format.data

import ua.wwind.table.strings.UiString

public enum class TableFormatTextStyle(
    public val uiString: UiString,
) {
    NORMAL(UiString.FormatTextStyleNormal),
    BOLD(UiString.FormatTextStyleBold),
    ITALIC(UiString.FormatTextStyleItalic),
    UNDERLINE(UiString.FormatTextStyleUnderline),
    STRIKETHROUGH(UiString.FormatTextStyleStrikethrough),
}
