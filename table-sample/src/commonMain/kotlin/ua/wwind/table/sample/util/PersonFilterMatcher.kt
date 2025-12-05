package ua.wwind.table.sample.util

import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
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
     */
    fun matchesPerson(
        person: Person,
        filters: Map<PersonColumn, TableFilterState<*>>,
    ): Boolean {
        for ((column, stateAny) in filters) {
            // If state has no constraint or values, skip this field (not restrictive)
            if (stateAny.constraint == null ||
                (
                    stateAny.values == null &&
                        stateAny.constraint != FilterConstraint.IS_NULL &&
                        stateAny.constraint != FilterConstraint.IS_NOT_NULL
                )
            ) {
                continue
            }

            val matches =
                when (column) {
                    PersonColumn.NAME -> matchesTextField(person.name, stateAny)
                    PersonColumn.AGE -> matchesIntField(person.age, stateAny)
                    PersonColumn.ACTIVE -> matchesBooleanField(person.active, stateAny)
                    PersonColumn.ID -> matchesIntField(person.id, stateAny)
                    PersonColumn.EMAIL -> matchesTextField(person.email, stateAny)
                    PersonColumn.CITY -> matchesTextField(person.city, stateAny)
                    PersonColumn.COUNTRY -> matchesTextField(person.country, stateAny)
                    PersonColumn.DEPARTMENT -> matchesTextField(person.department, stateAny)
                    PersonColumn.POSITION -> matchesPositionField(person.position, stateAny)
                    PersonColumn.SALARY -> matchesSalaryField(person.salary, stateAny)
                    PersonColumn.RATING -> matchesIntField(person.rating, stateAny)
                    PersonColumn.HIRE_DATE -> matchesDateField(person.hireDate, stateAny)
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
        value: String,
        state: TableFilterState<*>,
    ): Boolean {
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

    private fun matchesIntField(
        value: Int,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<Int>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.EQUALS -> value == (st.values?.getOrNull(0) ?: value)
            FilterConstraint.NOT_EQUALS -> value != (st.values?.getOrNull(0) ?: value)
            FilterConstraint.BETWEEN -> {
                val from = st.values?.getOrNull(0) ?: value
                val to = st.values?.getOrNull(1) ?: value
                value in from..to
            }

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
        return matchesIntField(value, state)
    }

    private fun matchesDateField(
        value: LocalDate,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<LocalDate>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.EQUALS -> value == (st.values?.getOrNull(0) ?: value)
            FilterConstraint.NOT_EQUALS -> value != (st.values?.getOrNull(0) ?: value)
            FilterConstraint.BETWEEN -> {
                val from = st.values?.getOrNull(0) ?: value
                val to = st.values?.getOrNull(1) ?: value
                from <= value && value <= to
            }

            else -> true
        }
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
