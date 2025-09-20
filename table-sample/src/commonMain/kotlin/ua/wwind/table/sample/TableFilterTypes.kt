package ua.wwind.table.sample

import ua.wwind.table.filter.data.TableFilterType

/**
 * Define filter types per field to drive the format dialog conditions.
 */
fun createFilterTypes(): Map<PersonColumn, TableFilterType<*>> {
    return mapOf(
        // Real Person fields
        PersonColumn.NAME to TableFilterType.TextTableFilter(),
        PersonColumn.AGE to TableFilterType.NumberTableFilter(
            delegate = TableFilterType.NumberTableFilter.IntDelegate,
            rangeOptions = 0 to 100,
        ),
        PersonColumn.ACTIVE to TableFilterType.BooleanTableFilter(),
        PersonColumn.ID to TableFilterType.NumberTableFilter(
            delegate = TableFilterType.NumberTableFilter.IntDelegate,
            rangeOptions = 1 to 1000,
        ),
        PersonColumn.EMAIL to TableFilterType.TextTableFilter(),
        PersonColumn.CITY to TableFilterType.TextTableFilter(),
        PersonColumn.COUNTRY to TableFilterType.TextTableFilter(),
        PersonColumn.DEPARTMENT to TableFilterType.TextTableFilter(),
        PersonColumn.SALARY to TableFilterType.NumberTableFilter(
            delegate = TableFilterType.NumberTableFilter.IntDelegate,
            rangeOptions = 0 to 200000,
        ),
        PersonColumn.RATING to TableFilterType.NumberTableFilter(
            delegate = TableFilterType.NumberTableFilter.IntDelegate,
            rangeOptions = 1 to 5,
        ),

        // Computed fields
        PersonColumn.AGE_GROUP to TableFilterType.TextTableFilter(),
    )
}