package ua.wwind.table.format.component

import ua.wwind.table.strings.UiString

public enum class RuleTab(
    public val uiString: UiString,
) {
    DESIGN(UiString.FormatDesignTab),
    CONDITION(UiString.FormatConditionTab),
    FIELD(UiString.FormatFieldTab),
}
