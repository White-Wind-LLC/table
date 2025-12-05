package ua.wwind.table.sample.util

import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.model.Position

/**
 * Factory for creating default filter states for Person columns.
 */
object PersonFilterStateFactory {
    /**
     * Create a default filter state for the given column.
     */
    fun createDefaultState(column: PersonColumn): TableFilterState<*> =
        when (column) {
            PersonColumn.NAME -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.AGE -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.ACTIVE ->
                TableFilterState<Boolean>(
                    constraint = FilterConstraint.EQUALS,
                    values = null,
                )

            PersonColumn.ID -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.EMAIL -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.CITY -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.COUNTRY -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.DEPARTMENT -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.POSITION ->
                TableFilterState<List<Position>>(
                    constraint = null,
                    values = null,
                )

            PersonColumn.SALARY -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.RATING -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.HIRE_DATE ->
                TableFilterState<LocalDate>(
                    constraint = null,
                    values = null,
                )

            PersonColumn.NOTES -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.AGE_GROUP -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.EXPAND ->
                TableFilterState<Boolean>(
                    constraint = FilterConstraint.EQUALS,
                    values = null,
                )

            PersonColumn.SELECTION -> {
                TableFilterState<Int>(
                    constraint = null,
                    values = null,
                )
            }
        }
}
