package ua.wwind.table.strings

import androidx.compose.runtime.Composable

/**
 * Typed keys for all table UI strings.
 * Prefer extending this sealed class with objects per string key for exhaustiveness.
 */
public sealed class UiString {
    // Generic filter actions
    public object FilterClear : UiString()

    public object FilterApply : UiString()

    // Placeholders
    public object FilterSearchPlaceholder : UiString()

    public object FilterEnterNumberPlaceholder : UiString()

    public object FilterSelectOnePlaceholder : UiString()

    public object FilterSelectManyPlaceholder : UiString()

    public object FilterRangeFromPlaceholder : UiString()

    public object FilterRangeToPlaceholder : UiString()

    public object FilterRangeIconDescription : UiString()

    // Boolean titles
    public object BooleanTrueTitle : UiString()

    public object BooleanFalseTitle : UiString()

    // Filter constraint titles
    public object FilterConstraintEquals : UiString()

    public object FilterConstraintNotEquals : UiString()

    public object FilterConstraintBetween : UiString()

    public object FilterConstraintContains : UiString()

    public object FilterConstraintIn : UiString()

    public object FilterConstraintStartsWith : UiString()

    public object FilterConstraintEndsWith : UiString()

    public object FilterConstraintNotIn : UiString()

    public object FilterConstraintGt : UiString()

    public object FilterConstraintGte : UiString()

    public object FilterConstraintLt : UiString()

    public object FilterConstraintLte : UiString()

    public object FilterConstraintIsNull : UiString()

    public object FilterConstraintIsNotNull : UiString()

    // Format
    public object FormatRules : UiString()

    public object FormatDesignTab : UiString()

    public object FormatConditionTab : UiString()

    public object FormatFieldTab : UiString()

    public object FormatVerticalAlignmentTop : UiString()

    public object FormatVerticalAlignmentCenter : UiString()

    public object FormatVerticalAlignmentBottom : UiString()

    public object FormatHorizontalAlignmentStart : UiString()

    public object FormatHorizontalAlignmentCenter : UiString()

    public object FormatHorizontalAlignmentEnd : UiString()

    public object FormatTextStyleNormal : UiString()

    public object FormatTextStyleBold : UiString()

    public object FormatTextStyleItalic : UiString()

    public object FormatTextStyleUnderline : UiString()

    public object FormatTextStyleStrikethrough : UiString()

    public object FormatLabelVerticalAlignment : UiString()

    public object FormatLabelHorizontalAlignment : UiString()

    public object FormatLabelTypography : UiString()

    public object FormatContentColor : UiString()

    public object FormatBackgroundColor : UiString()

    public object FormatChooseColor : UiString()

    public object FormatResetColor : UiString()

    public object FormatAlwaysApply : UiString()
}

/**
 * Minimal string provider for table UI.
 */
public interface StringProvider {
    @Composable
    public fun get(key: UiString): String
}

/**
 * Default English strings for the table UI.
 */
public object DefaultStrings : StringProvider {
    @Composable
    public override fun get(key: UiString): String =
        when (key) {
            // Generic
            UiString.FilterClear -> "Clear"
            UiString.FilterApply -> "Apply"

            // Placeholders
            UiString.FilterSearchPlaceholder -> "Search..."
            UiString.FilterEnterNumberPlaceholder -> "Enter number..."
            UiString.FilterSelectOnePlaceholder -> "Select One"
            UiString.FilterSelectManyPlaceholder -> "Select Many"
            UiString.FilterRangeFromPlaceholder -> "From"
            UiString.FilterRangeToPlaceholder -> "To"
            UiString.FilterRangeIconDescription -> "Range"

            // Boolean
            UiString.BooleanTrueTitle -> "Yes"
            UiString.BooleanFalseTitle -> "No"

            // Constraints
            UiString.FilterConstraintEquals -> "Equals"
            UiString.FilterConstraintNotEquals -> "Not equals"
            UiString.FilterConstraintBetween -> "Between"
            UiString.FilterConstraintContains -> "Contains"
            UiString.FilterConstraintIn -> "In"
            UiString.FilterConstraintStartsWith -> "Starts with"
            UiString.FilterConstraintEndsWith -> "Ends with"
            UiString.FilterConstraintNotIn -> "Not in"
            UiString.FilterConstraintGt -> "Greater than"
            UiString.FilterConstraintGte -> "Greater than or equal"
            UiString.FilterConstraintLt -> "Less than"
            UiString.FilterConstraintLte -> "Less than or equal"
            UiString.FilterConstraintIsNull -> "Is null"
            UiString.FilterConstraintIsNotNull -> "Is not null"

            // Format
            UiString.FormatRules -> "Formatting rules"
            UiString.FormatDesignTab -> "Design"
            UiString.FormatConditionTab -> "Condition"
            UiString.FormatFieldTab -> "Fields to format"
            UiString.FormatVerticalAlignmentTop -> "Top"
            UiString.FormatVerticalAlignmentCenter -> "Center"
            UiString.FormatVerticalAlignmentBottom -> "Bottom"
            UiString.FormatHorizontalAlignmentStart -> "Start"
            UiString.FormatHorizontalAlignmentCenter -> "Center"
            UiString.FormatHorizontalAlignmentEnd -> "End"
            UiString.FormatLabelVerticalAlignment -> "Vertical alignment"
            UiString.FormatLabelHorizontalAlignment -> "Horizontal alignment"
            UiString.FormatLabelTypography -> "Text style"
            UiString.FormatTextStyleNormal -> "Normal"
            UiString.FormatTextStyleItalic -> "Italic"
            UiString.FormatTextStyleBold -> "Bold"
            UiString.FormatTextStyleUnderline -> "Underline"
            UiString.FormatTextStyleStrikethrough -> "Strikethrough"
            UiString.FormatContentColor -> "Content color"
            UiString.FormatBackgroundColor -> "Background color"
            UiString.FormatChooseColor -> "Choose color"
            UiString.FormatResetColor -> "Reset color"
            UiString.FormatAlwaysApply -> "Always"
        }
}
