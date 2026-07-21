package ua.wwind.table.sample.app.components

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.FormatDialog
import ua.wwind.table.format.data.FormatDialogSettings
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.strings.DefaultStrings

/**
 * Forwards everything to `FormatDialog`, supplying one exhaustive `when` arm per column to label
 * it. The complexity count is the number of columns rather than branching a reader has to follow,
 * so `CyclomaticComplexMethod` is suppressed rather than fixed.
 */
@Suppress("CyclomaticComplexMethod")
@Composable
fun ConditionalFormattingDialog(
    showDialog: Boolean,
    rules: ImmutableList<TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>>,
    onRulesChanged: (
        ImmutableList<
            TableFormatRule<
                PersonColumn,
                Map<PersonColumn, TableFilterState<*>>,
            >,
        >,
    ) -> Unit,
    buildFormatFilterData: (
        TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
        (
            TableFormatRule<
                PersonColumn,
                Map<PersonColumn, TableFilterState<*>>,
            >,
        ) -> Unit,
    ) -> List<
        ua.wwind.table.format.FormatFilterData<PersonColumn>,
    >,
    onDismissRequest: () -> Unit,
) {
    FormatDialog(
        showDialog = showDialog,
        rules = rules,
        onRulesChanged = onRulesChanged,
        getNewRule = { id ->
            TableFormatRule.new<PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
                id,
                emptyMap(),
            )
        },
        getTitle = { field ->
            when (field) {
                PersonColumn.NAME -> "Name"
                PersonColumn.AGE -> "Age"
                PersonColumn.ACTIVE -> "Active"
                PersonColumn.ID -> "ID"
                PersonColumn.EMAIL -> "Email"
                PersonColumn.CITY -> "City"
                PersonColumn.COUNTRY -> "Country"
                PersonColumn.DEPARTMENT -> "Department"
                PersonColumn.POSITION -> "Position"
                PersonColumn.SALARY -> "Salary"
                PersonColumn.RATING -> "Rating"
                PersonColumn.HIRE_DATE -> "Hire Date"
                PersonColumn.NOTES -> "Notes"
                PersonColumn.AGE_GROUP -> "Age group"
                PersonColumn.EXPAND -> "Movements"
                PersonColumn.SELECTION -> "Selection"
            }
        },
        filters = buildFormatFilterData,
        entries = PersonColumn.entries.toImmutableList(),
        key = Unit,
        strings = DefaultStrings,
        onDismissRequest = onDismissRequest,
        settings = FormatDialogSettings(),
    )
}
