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
     * Apply sorting to a list of persons based on the sort state.
     */
    fun sortPeople(
        people: List<Person>,
        sort: SortState<PersonColumn>?,
    ): List<Person> {
        if (sort == null) {
            return people
        }

        val sortedList =
            when (sort.column) {
                PersonColumn.NAME -> people.sortedBy { it.name.lowercase() }
                PersonColumn.AGE -> people.sortedBy { it.age }
                PersonColumn.ACTIVE -> people.sortedBy { it.active }
                PersonColumn.ID -> people.sortedBy { it.id }
                PersonColumn.EMAIL -> people.sortedBy { it.email.lowercase() }
                PersonColumn.CITY -> people.sortedBy { it.city.lowercase() }
                PersonColumn.COUNTRY -> people.sortedBy { it.country.lowercase() }
                PersonColumn.DEPARTMENT -> people.sortedBy { it.department.lowercase() }
                PersonColumn.POSITION -> people.sortedBy { it.position.name }
                PersonColumn.SALARY -> people.sortedBy { it.salary }
                PersonColumn.RATING -> people.sortedBy { it.rating }
                PersonColumn.HIRE_DATE -> people.sortedBy { it.hireDate }
                PersonColumn.NOTES -> people.sortedBy { it.notes.lowercase() }
                PersonColumn.AGE_GROUP -> {
                    people.sortedBy {
                        when {
                            it.age < 25 -> 0
                            it.age < 35 -> 1
                            else -> 2
                        }
                    }
                }

                else -> people
            }

        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
