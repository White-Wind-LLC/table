package ua.wwind.table.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

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

    public object FilterOptionsSearchPlaceholder : UiString()

    // Multi-select actions
    public object FilterSelectAll : UiString()

    public object FilterSelectNone : UiString()

    // Field labels
    public object FilterConditionLabel : UiString()

    public object FilterValueLabel : UiString()

    public object FilterDateLabel : UiString()

    // Inline input errors
    public object FilterErrorInvalidNumber : UiString()

    public object FilterErrorRangeIncomplete : UiString()

    public object FilterErrorRangeInverted : UiString()

    // Date picker
    public object DatePickerSelectDate : UiString()

    public object DatePickerConfirm : UiString()

    public object DatePickerCancel : UiString()

    public object DatePickerClear : UiString()

    // Boolean titles
    public object BooleanTrueTitle : UiString()

    public object BooleanFalseTitle : UiString()

    public object BooleanAnyTitle : UiString()

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

    public object FormatDeleteRuleTitle : UiString()

    public object FormatDeleteRuleConfirm : UiString()

    public object FormatDeleteRuleCancel : UiString()

    // Grouping menu
    public object GroupBy : UiString()

    public object Ungroup : UiString()

    // Column menu
    public object ColumnMenuSortAscending : UiString()

    public object ColumnMenuSortDescending : UiString()

    public object ColumnMenuClearSort : UiString()

    public object ColumnMenuOpenFilter : UiString()

    public object ColumnMenuClearFilter : UiString()

    public object ColumnMenuPinLeft : UiString()

    public object ColumnMenuPinRight : UiString()

    public object ColumnMenuUnpin : UiString()

    public object ColumnMenuMoveLeft : UiString()

    public object ColumnMenuMoveRight : UiString()

    public object ColumnMenuAutoFit : UiString()

    public object ColumnMenuResetWidth : UiString()

    public object ColumnMenuHide : UiString()

    public object ColumnMenuShowHidden : UiString()

    public object ColumnMenuOptions : UiString()

    public object ColumnMenuReasonRowReorder : UiString()

    public object ColumnMenuReasonRowBlocks : UiString()

    public object ColumnMenuReasonFirst : UiString()

    public object ColumnMenuReasonLast : UiString()

    public object ColumnMenuReasonPinnedEdge : UiString()

    public object ColumnMenuReasonDefaultWidth : UiString()

    public object ColumnMenuReasonNothingToFit : UiString()

    public object ColumnMenuReasonLastVisible : UiString()

    public object ColumnMenuReasonLastUnpinned : UiString()

    // Tooltip actions
    public object TooltipDismiss : UiString()

    // Empty state
    public object EmptyNoData : UiString()

    public object EmptyNoResults : UiString()

    public object EmptyClearFilters : UiString()

    // Paged load states
    public object PagingLoading : UiString()

    public object PagingLoadError : UiString()

    public object PagingLoadMoreError : UiString()

    public object PagingRetry : UiString()
}

/**
 * Minimal string provider for table UI.
 */
@Stable
public interface StringProvider {
    @Composable
    public fun get(key: UiString): String
}

/**
 * Default English strings for the table UI.
 */
public object DefaultStrings : StringProvider {
    /**
     * One exhaustive arm per [UiString] key, each a string literal. The arms do not interact, so
     * the complexity count is the number of strings the table has rather than branching a reader
     * has to follow — `CyclomaticComplexMethod` is suppressed rather than fixed. Splitting the
     * table by category would only hide which keys are covered from the compiler.
     */
    @Suppress("CyclomaticComplexMethod")
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

            UiString.FilterOptionsSearchPlaceholder -> "Search options…"

            // Multi-select actions
            UiString.FilterSelectAll -> "Select all"

            UiString.FilterSelectNone -> "None"

            // Field labels
            UiString.FilterConditionLabel -> "Condition"

            UiString.FilterValueLabel -> "Value"

            UiString.FilterDateLabel -> "Date"

            // Inline input errors
            UiString.FilterErrorInvalidNumber -> "Enter a valid number"

            UiString.FilterErrorRangeIncomplete -> "Enter both values"

            UiString.FilterErrorRangeInverted -> "From must not be greater than To"

            // Date picker
            UiString.DatePickerSelectDate -> "Select Date"

            UiString.DatePickerConfirm -> "Confirm"

            UiString.DatePickerCancel -> "Cancel"

            UiString.DatePickerClear -> "Clear"

            // Boolean
            UiString.BooleanTrueTitle -> "Yes"

            UiString.BooleanFalseTitle -> "No"

            UiString.BooleanAnyTitle -> "Any"

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

            UiString.FormatDeleteRuleTitle -> "Delete this rule?"

            UiString.FormatDeleteRuleConfirm -> "Delete"

            UiString.FormatDeleteRuleCancel -> "Cancel"

            // Grouping menu
            UiString.GroupBy -> "Group by"

            UiString.Ungroup -> "Ungroup"

            // Column menu
            UiString.ColumnMenuSortAscending -> "Sort ascending"

            UiString.ColumnMenuSortDescending -> "Sort descending"

            UiString.ColumnMenuClearSort -> "Clear sort"

            UiString.ColumnMenuOpenFilter -> "Filter…"

            UiString.ColumnMenuClearFilter -> "Clear filter"

            UiString.ColumnMenuPinLeft -> "Pin left"

            UiString.ColumnMenuPinRight -> "Pin right"

            UiString.ColumnMenuUnpin -> "Unpin"

            UiString.ColumnMenuMoveLeft -> "Move left"

            UiString.ColumnMenuMoveRight -> "Move right"

            UiString.ColumnMenuAutoFit -> "Auto-fit width"

            UiString.ColumnMenuResetWidth -> "Reset width"

            UiString.ColumnMenuHide -> "Hide column"

            UiString.ColumnMenuShowHidden -> "Show hidden columns"

            UiString.ColumnMenuOptions -> "Column options"

            UiString.ColumnMenuReasonRowReorder -> "Unavailable while rows can be reordered"

            UiString.ColumnMenuReasonRowBlocks -> "Unavailable while row blocks are shown"

            UiString.ColumnMenuReasonFirst -> "Already first"

            UiString.ColumnMenuReasonLast -> "Already last"

            UiString.ColumnMenuReasonPinnedEdge -> "Already at the pinned edge"

            UiString.ColumnMenuReasonDefaultWidth -> "Already at default width"

            UiString.ColumnMenuReasonNothingToFit -> "No content to fit yet"

            UiString.ColumnMenuReasonLastVisible -> "The last visible column can't be hidden"

            UiString.ColumnMenuReasonLastUnpinned -> "At least one column must stay unpinned"

            // Tooltip actions
            UiString.TooltipDismiss -> "Dismiss"

            // Empty state
            UiString.EmptyNoData -> "No data"

            UiString.EmptyNoResults -> "No results match the current filters"

            UiString.EmptyClearFilters -> "Clear filters"

            // Paged load states
            UiString.PagingLoading -> "Loading"

            UiString.PagingLoadError -> "Couldn't load data"

            UiString.PagingLoadMoreError -> "Couldn't load some rows"

            UiString.PagingRetry -> "Retry"
        }
}
