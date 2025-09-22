package ua.wwind.table.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.FormatFilterData
import ua.wwind.table.format.data.TableCellStyleConfig
import ua.wwind.table.format.data.TableFormatRule

@OptIn(ExperimentalTableApi::class)
class SampleViewModel {
    // Demo dataset
    val people = createDemoData()

    // Define filter types per field (to drive the format dialog conditions)
    val filterTypes = createFilterTypes()

    // Column definitions with titles, cells and optional filters (for header UI)
    val columns = createTableColumns()

    // Conditional formatting rules (editable via dialog)
    var rules by mutableStateOf<List<TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>>>(emptyList())
        private set

    // Dialog visibility
    var showFormatDialog by mutableStateOf(false)
        private set

    /**
     * Toggle dialog visibility
     */
    fun toggleFormatDialog(show: Boolean) {
        showFormatDialog = show
    }

    /**
     * Update formatting rules
     */
    fun updateRules(newRules: List<TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>>) {
        rules = newRules
    }

    /**
     * Build `FormatFilterData` list for the dialog from current rule state.
     */
    fun buildFormatFilterData(
        rule: TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
        onApply: (TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>) -> Unit,
    ): List<FormatFilterData<PersonColumn>> =
        PersonColumn.entries.map { column ->
            val type = filterTypes.getValue(column)
            val current: TableFilterState<*>? = rule.filter[column]
            val defaultState: TableFilterState<*> =
                when (column) {
                    PersonColumn.NAME -> TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.AGE -> TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.ACTIVE -> TableFilterState<Boolean>(
                        constraint = FilterConstraint.EQUALS,
                        values = null
                    )

                    PersonColumn.ID -> TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.EMAIL -> TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.CITY -> TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.COUNTRY -> TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.DEPARTMENT -> TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.SALARY -> TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.RATING -> TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.NOTES -> TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.AGE_GROUP -> TableFilterState<String>(constraint = null, values = null)
                }
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

    /**
     * Evaluate whether the given person matches the rule's filter map.
     * Supports Text, Number(Int) and Boolean filter types used in the sample.
     */
    fun matchesPerson(
        person: Person,
        ruleFilters: Map<PersonColumn, TableFilterState<*>>,
    ): Boolean {
        for ((column, stateAny) in ruleFilters) {
            val type = filterTypes[column] ?: continue
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

            when (column) {
                PersonColumn.NAME -> {
                    val value = person.name
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }

                PersonColumn.AGE -> {
                    val value = person.age
                    val st = stateAny as TableFilterState<Int>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
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
                    if (!ok) return false
                }

                PersonColumn.ACTIVE -> {
                    val value = person.active
                    val st = stateAny as TableFilterState<Boolean>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.EQUALS -> st.values?.firstOrNull()?.let { v -> value == v } ?: true
                            FilterConstraint.NOT_EQUALS -> st.values?.firstOrNull()?.let { v -> value != v } ?: true
                            else -> true
                        }
                    if (!ok) return false
                }

                PersonColumn.ID -> {
                    val value = person.id
                    val st = stateAny as TableFilterState<Int>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
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
                    if (!ok) return false
                }

                PersonColumn.EMAIL -> {
                    val value = person.email
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }

                PersonColumn.CITY -> {
                    val value = person.city
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }

                PersonColumn.COUNTRY -> {
                    val value = person.country
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }

                PersonColumn.DEPARTMENT -> {
                    val value = person.department
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }

                PersonColumn.SALARY -> {
                    val value = person.salary
                    val st = stateAny as TableFilterState<Int>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
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
                    if (!ok) return false
                }

                PersonColumn.RATING -> {
                    val value = person.rating
                    val st = stateAny as TableFilterState<Int>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
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
                    if (!ok) return false
                }

                PersonColumn.NOTES -> {
                    val value = person.notes
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }

                PersonColumn.AGE_GROUP -> {
                    val value =
                        when {
                            person.age < 25 -> "<25"
                            person.age < 35 -> "25-34"
                            else -> "35+"
                        }
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
            }
        }
        return true
    }

    /**
     * Clear resources when the ViewModel is no longer needed
     */
    fun clear() {
        // Add cleanup logic here if needed
    }

    init {
        // Default conditional formatting: if RATING >= 4, set content color to gold for the Rating column
        val ratingFilter: Map<PersonColumn, TableFilterState<*>> =
            mapOf(
                PersonColumn.RATING to TableFilterState(
                    constraint = FilterConstraint.GTE,
                    values = listOf(4),
                ),
            )
        val ratingRule =
            TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
                id = 1L,
                enabled = true,
                base = false,
                columns = listOf(PersonColumn.RATING),
                cellStyle = TableCellStyleConfig(
                    contentColor = 0xFFFFD700.toInt(), // Gold
                ),
                filter = ratingFilter,
            )
        // Default conditional formatting: if ACTIVE = false, set content color to gray for the whole row
        val activeFilter: Map<PersonColumn, TableFilterState<*>> =
            mapOf(
                PersonColumn.ACTIVE to TableFilterState(
                    constraint = FilterConstraint.EQUALS,
                    values = listOf(false),
                ),
            )
        val activeRule =
            TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
                id = 2L,
                enabled = true,
                base = false,
                columns = emptyList(),
                cellStyle = TableCellStyleConfig(
                    contentColor = Color.LightGray.toArgb(),
                ),
                filter = activeFilter,
            )
        rules = listOf(ratingRule, activeRule)
    }
}
