package ua.wwind.table.sample.util

import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.FormatFilterData
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.filter.filterTypes

/**
 * Builds the per-column [FormatFilterData] the conditional formatting dialog edits.
 *
 * Stateless: it reads the rule it is handed plus the static column metadata and never touches the
 * ViewModel, so it lives with the other Person helpers rather than on `SampleViewModel`.
 */
object PersonFormatFilterData {
    /** Build the `FormatFilterData` list for [rule]; edits are published through [onApply]. */
    fun build(
        rule: TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
        onApply: (TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>) -> Unit,
    ): List<FormatFilterData<PersonColumn>> =
        PersonColumn.entries.map { column ->
            val type = filterTypes.getValue(column)
            val current: TableFilterState<*>? = rule.filter[column]
            val defaultState: TableFilterState<*> = PersonFilterStateFactory.createDefaultState(column)

            FormatFilterData(
                field = column,
                filterType = type,
                filterState = current ?: defaultState,
                onChange = { newState ->
                    val newMap = rule.filter.toMutableMap().apply { put(column, newState) }
                    onApply(rule.copy(filter = newMap))
                },
            )
        }
}
