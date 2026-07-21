package ua.wwind.table.sample.util

import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.isNullCheck
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.filter.NumericRangeFilterState
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.Position

/**
 * Utility class for filtering Person objects based on filter constraints.
 */
object PersonFilterMatcher {
    /**
     * Evaluate whether the given person matches the filter map.
     * Supports Text, Number(Int), Boolean, LocalDate filter types.
     *
     * One exhaustive arm per column, each naming the matcher for that field's type. The complexity
     * count is the number of columns rather than branching a reader has to follow, so
     * `CyclomaticComplexMethod` is suppressed rather than fixed — the alternative is a lookup map
     * that hides the missing-column check from the compiler.
     */
    @Suppress("CyclomaticComplexMethod")
    fun matchesPerson(
        person: Person,
        filters: Map<PersonColumn, TableFilterState<*>>,
    ): Boolean {
        for ((column, stateAny) in filters) {
            // If state has no constraint or values, skip this field (not restrictive)
            val constraint = stateAny.constraint
            if (constraint == null ||
                (
                    stateAny.values == null &&
                        !constraint.isNullCheck()
                )
            ) {
                continue
            }

            val matches =
                when (column) {
                    PersonColumn.NAME -> matchesTextField(person.name, stateAny)
                    PersonColumn.AGE -> matchesComparableField(person.age, stateAny)
                    PersonColumn.ACTIVE -> matchesBooleanField(person.active, stateAny)
                    PersonColumn.ID -> matchesComparableField(person.id, stateAny)
                    PersonColumn.EMAIL -> matchesTextField(person.email, stateAny)
                    PersonColumn.CITY -> matchesTextField(person.city, stateAny)
                    PersonColumn.COUNTRY -> matchesTextField(person.country, stateAny)
                    PersonColumn.DEPARTMENT -> matchesTextField(person.department, stateAny)
                    PersonColumn.POSITION -> matchesPositionField(person.position, stateAny)
                    PersonColumn.SALARY -> matchesSalaryField(person.salary, stateAny)
                    PersonColumn.RATING -> matchesComparableField(person.rating, stateAny)
                    PersonColumn.HIRE_DATE -> matchesComparableField(person.hireDate, stateAny)
                    PersonColumn.NOTES -> matchesTextField(person.notes, stateAny)
                    PersonColumn.AGE_GROUP -> matchesAgeGroupField(person.age, stateAny)
                    PersonColumn.EXPAND -> true
                    PersonColumn.SELECTION -> true
                }

            if (!matches) return false
        }
        return true
    }

    private fun matchesTextField(
        value: String?,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<String>
        val query = st.values?.firstOrNull().orEmpty()
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.CONTAINS -> value?.contains(query, ignoreCase = true) == true
            FilterConstraint.STARTS_WITH -> value?.startsWith(query, ignoreCase = true) == true
            FilterConstraint.ENDS_WITH -> value?.endsWith(query, ignoreCase = true) == true
            FilterConstraint.EQUALS -> value?.equals(query, ignoreCase = true) == true
            FilterConstraint.NOT_EQUALS -> value?.equals(query, ignoreCase = true) != true
            FilterConstraint.IS_NULL -> value.isNullOrEmpty()
            FilterConstraint.IS_NOT_NULL -> !value.isNullOrEmpty()
            else -> true
        }
    }

    /**
     * Ordering constraints over any comparable field. Int and LocalDate share this: the constraint
     * set is the same and both are [Comparable], so the two used to be the same `when` written twice.
     *
     * A missing bound falls back to [value] itself, which makes the comparison trivially true — an
     * incomplete range filters nothing rather than everything.
     */
    private fun <T : Comparable<T>> matchesComparableField(
        value: T,
        state: TableFilterState<*>,
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        val st = state as TableFilterState<T>
        val constraint = st.constraint ?: return true
        val first = st.values?.getOrNull(0) ?: value

        return when (constraint) {
            FilterConstraint.GT -> value > first
            FilterConstraint.GTE -> value >= first
            FilterConstraint.LT -> value < first
            FilterConstraint.LTE -> value <= first
            FilterConstraint.EQUALS -> value == first
            FilterConstraint.NOT_EQUALS -> value != first
            FilterConstraint.BETWEEN -> value in first..(st.values?.getOrNull(1) ?: value)
            else -> true
        }
    }

    private fun matchesBooleanField(
        value: Boolean,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<Boolean>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.EQUALS -> st.values?.firstOrNull()?.let { v -> value == v } ?: true
            FilterConstraint.NOT_EQUALS -> st.values?.firstOrNull()?.let { v -> value != v } ?: true
            else -> true
        }
    }

    private fun matchesPositionField(
        value: Position,
        state: TableFilterState<*>,
    ): Boolean {
        val constraint = state.constraint ?: return true

        @Suppress("UNCHECKED_CAST")
        val selectedValues = (state.values as? List<Position>) ?: emptyList()

        return when (constraint) {
            FilterConstraint.IN -> selectedValues.isEmpty() || selectedValues.contains(value)
            FilterConstraint.NOT_IN -> selectedValues.isEmpty() || !selectedValues.contains(value)
            FilterConstraint.EQUALS -> selectedValues.firstOrNull() == value
            FilterConstraint.NOT_EQUALS -> selectedValues.firstOrNull() != value
            else -> true
        }
    }

    private fun matchesSalaryField(
        value: Int,
        state: TableFilterState<*>,
    ): Boolean {
        // Check if using custom NumericRangeFilter
        if (state.values?.firstOrNull() is NumericRangeFilterState) {
            val customState = state.values?.firstOrNull() as? NumericRangeFilterState
            return customState?.let { value in it.min..it.max } ?: true
        }

        // Standard number filter
        return matchesComparableField(value, state)
    }

    private fun matchesAgeGroupField(
        age: Int,
        state: TableFilterState<*>,
    ): Boolean {
        val value =
            when {
                age < 25 -> "<25"
                age < 35 -> "25-34"
                else -> "35+"
            }

        val st = state as TableFilterState<String>
        val query = st.values?.firstOrNull().orEmpty()
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
            else -> true
        }
    }
}
