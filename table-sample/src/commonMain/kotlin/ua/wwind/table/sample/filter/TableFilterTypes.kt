package ua.wwind.table.sample.filter

import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.model.Position

/** Define filter types per field to drive the format dialog conditions. */
fun createFilterTypes(): Map<PersonColumn, TableFilterType<*>> =
    mapOf(
        // Real Person fields
        PersonColumn.NAME to TableFilterType.TextTableFilter(),
        PersonColumn.AGE to
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 100,
            ),
        PersonColumn.ACTIVE to TableFilterType.BooleanTableFilter(),
        PersonColumn.ID to
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 1 to 1000,
            ),
        PersonColumn.EMAIL to TableFilterType.TextTableFilter(),
        PersonColumn.CITY to TableFilterType.TextTableFilter(),
        PersonColumn.COUNTRY to TableFilterType.TextTableFilter(),
        PersonColumn.DEPARTMENT to TableFilterType.TextTableFilter(),
        PersonColumn.POSITION to
            TableFilterType.EnumTableFilter(
                options = Position.entries.toImmutableList(),
                getTitle = { it.displayName },
            ),
        PersonColumn.SALARY to
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 200000,
            ),
        PersonColumn.RATING to
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 1 to 5,
            ),
        PersonColumn.HIRE_DATE to TableFilterType.DateTableFilter(),
        PersonColumn.NOTES to TableFilterType.TextTableFilter(),
        // Computed fields
        PersonColumn.AGE_GROUP to TableFilterType.TextTableFilter(),
        PersonColumn.EXPAND to TableFilterType.DisabledTableFilter,
        PersonColumn.SELECTION to TableFilterType.DisabledTableFilter,
    )
