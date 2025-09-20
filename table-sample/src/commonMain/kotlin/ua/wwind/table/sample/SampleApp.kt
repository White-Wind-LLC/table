package ua.wwind.table.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.format.FormatDialog
import ua.wwind.table.format.FormatDialogSettings
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.rememberCustomization
import ua.wwind.table.format.FormatFilterData
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings

@OptIn(ExperimentalTableApi::class)
@Composable
fun SampleApp(modifier: Modifier = Modifier) {
    // Demo dataset
    val people = remember { createDemoData() }

    // Define filter types per field (to drive the format dialog conditions)
    val filterTypes = remember { createFilterTypes() }

    // Column definitions with titles, cells and optional filters (for header UI)
    val columns = remember { createTableColumns() }

    val state = rememberTableState(columns = PersonColumn.entries)

    // Conditional formatting rules (editable via dialog)
    var rules by remember {
        mutableStateOf(
            emptyList<TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>>()
        )
    }
    var showFormatDialog by remember { mutableStateOf(false) }

    // Build customization based on rules + matching logic
    val customization: TableCustomization<Person, PersonColumn> = rememberCustomization(
        rules = rules,
        key = rules, // recompose on rules change
        matches = { person, ruleFilters -> matchesPerson(person, ruleFilters, filterTypes) },
    )

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Toolbar with a single button that opens the conditional formatting dialog
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { showFormatDialog = true }) {
                    Text("Conditional formatting")
                }
            }
            HorizontalDivider()

            // The table
            Table(
                itemsCount = people.size,
                itemAt = { index -> people.getOrNull(index) },
                state = state,
                columns = columns,
                customization = customization,
                strings = DefaultStrings,
            )
        }
    }

    // Formatting dialog
    FormatDialog(
        showDialog = showFormatDialog,
        rules = rules,
        onRulesChanged = { rules = it },
        getNewRule = { id ->
            TableFormatRule.new<PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
                id,
                emptyMap()
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
                PersonColumn.SALARY -> "Salary"
                PersonColumn.RATING -> "Rating"
                PersonColumn.AGE_GROUP -> "Age group"
            }
        },
        filters = { rule, onApply ->
            buildFormatFilterData(
                rule = rule,
                filterTypes = filterTypes,
                onApply = onApply,
            )
        },
        entries = PersonColumn.entries,
        key = Unit,
        strings = DefaultStrings,
        onDismissRequest = { showFormatDialog = false },
        settings = FormatDialogSettings(),
    )
}

/**
 * Build `FormatFilterData` list for the dialog from current rule state.
 */
private fun buildFormatFilterData(
    rule: TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
    filterTypes: Map<PersonColumn, TableFilterType<*>>,
    onApply: (TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>) -> Unit,
): List<FormatFilterData<PersonColumn>> {
    return PersonColumn.entries.map { column ->
        val type = filterTypes.getValue(column)
        val current: TableFilterState<*>? = rule.filter[column]
        val defaultState: TableFilterState<*> = when (column) {
            PersonColumn.NAME -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.AGE -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.ACTIVE -> TableFilterState<Boolean>(constraint = FilterConstraint.EQUALS, values = null)
            PersonColumn.ID -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.EMAIL -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.CITY -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.COUNTRY -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.DEPARTMENT -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.SALARY -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.RATING -> TableFilterState<Int>(constraint = null, values = null)
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
}

/**
 * Evaluate whether the given person matches the rule's filter map.
 * Supports Text, Number(Int) and Boolean filter types used in the sample.
 */
private fun matchesPerson(
    person: Person,
    ruleFilters: Map<PersonColumn, TableFilterState<*>>,
    filterTypes: Map<PersonColumn, TableFilterType<*>>,
): Boolean {
    for ((column, stateAny) in ruleFilters) {
        val type = filterTypes[column] ?: continue
        // If state has no constraint or values, skip this field (not restrictive)
        if (stateAny.constraint == null || (stateAny.values == null && stateAny.constraint != FilterConstraint.IS_NULL && stateAny.constraint != FilterConstraint.IS_NOT_NULL)) continue

        when (column) {
            PersonColumn.NAME -> {
                val value = person.name
                val st = stateAny as TableFilterState<String>
                val query = st.values?.firstOrNull().orEmpty()
                val constraint = st.constraint ?: continue
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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
                val ok = when (constraint) {
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

            PersonColumn.AGE_GROUP -> {
                val value = when {
                    person.age < 25 -> "<25"
                    person.age < 35 -> "25-34"
                    else -> "35+"
                }
                val st = stateAny as TableFilterState<String>
                val query = st.values?.firstOrNull().orEmpty()
                val constraint = st.constraint ?: continue
                val ok = when (constraint) {
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
