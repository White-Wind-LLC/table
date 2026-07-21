package ua.wwind.table.sample.util

import ua.wwind.table.data.SortOrder
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.model.Person
import ua.wwind.table.state.SortState

/**
 * Utility class for sorting Person lists.
 */
object PersonSorter {
    /**
     * The ordering rule as a comparator rather than a sorted list, because the within-blocks sort
     * demo feeds it to `sortedWithinRowBlocks` while the free-sort path plain-sorts with it.
     * Returns null when [sort] is absent or the column has no ordering.
     */
    fun comparatorFor(sort: SortState<PersonColumn>?): Comparator<Person>? {
        if (sort == null) return null

        val comparator: Comparator<Person> =
            when (sort.column) {
                PersonColumn.NAME -> {
                    compareBy { it.name.lowercase() }
                }

                PersonColumn.AGE -> {
                    compareBy { it.age }
                }

                PersonColumn.ACTIVE -> {
                    compareBy { it.active }
                }

                PersonColumn.ID -> {
                    compareBy { it.id }
                }

                PersonColumn.EMAIL -> {
                    compareBy { it.email.orEmpty().lowercase() }
                }

                PersonColumn.CITY -> {
                    compareBy { it.city.lowercase() }
                }

                PersonColumn.COUNTRY -> {
                    compareBy { it.country.lowercase() }
                }

                PersonColumn.DEPARTMENT -> {
                    compareBy { it.department.lowercase() }
                }

                PersonColumn.POSITION -> {
                    compareBy { it.position.name }
                }

                PersonColumn.SALARY -> {
                    compareBy { it.salary }
                }

                PersonColumn.RATING -> {
                    compareBy { it.rating }
                }

                PersonColumn.HIRE_DATE -> {
                    compareBy { it.hireDate }
                }

                PersonColumn.NOTES -> {
                    compareBy { it.notes.lowercase() }
                }

                PersonColumn.AGE_GROUP -> {
                    compareBy {
                        when {
                            it.age < 25 -> 0
                            it.age < 35 -> 1
                            else -> 2
                        }
                    }
                }

                else -> {
                    return null
                }
            }

        return if (sort.order == SortOrder.DESCENDING) {
            comparator.reversed()
        } else {
            comparator
        }
    }
}
