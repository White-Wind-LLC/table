package ua.wwind.table.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
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
import ua.wwind.table.tableColumns

/**
 * Simple Person model used in the sample.
 */
data class Person(
    val name: String,
    val age: Int,
    val active: Boolean,
)

/**
 * Columns enum is used as a stable column key and to wire conditional formatting to fields.
 */
enum class PersonColumn {
    NAME,
    AGE,
    ACTIVE,
    NAME_LENGTH,
    AGE_GROUP,
    AGE_TWICE,
    ACTIVE_INT,
    FIRST_LETTER,
    HAS_E_IN_NAME,
    NAME_REVERSED,
}

@OptIn(ExperimentalTableApi::class)
@Composable
fun SampleApp(modifier: Modifier = Modifier) {
    // Demo dataset
    val people = remember {
        listOf(
            Person("Alice", 24, true),
            Person("Bob", 31, false),
            Person("Carol", 29, true),
            Person("Dave", 40, true),
            Person("Eve", 22, false),
        )
    }

    // Define filter types per field (to drive the format dialog conditions)
    val filterTypes = remember {
        mapOf<PersonColumn, TableFilterType<*>>(
            PersonColumn.NAME to TableFilterType.TextTableFilter(),
            PersonColumn.AGE to TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 100,
            ),
            PersonColumn.ACTIVE to TableFilterType.BooleanTableFilter(),
            PersonColumn.NAME_LENGTH to TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 100,
            ),
            PersonColumn.AGE_GROUP to TableFilterType.TextTableFilter(),
            PersonColumn.AGE_TWICE to TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 200,
            ),
            PersonColumn.ACTIVE_INT to TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 1,
            ),
            PersonColumn.FIRST_LETTER to TableFilterType.TextTableFilter(),
            PersonColumn.HAS_E_IN_NAME to TableFilterType.BooleanTableFilter(),
            PersonColumn.NAME_REVERSED to TableFilterType.TextTableFilter(),
        )
    }

    // Column definitions with titles, cells and optional filters (for header UI)
    val columns: List<ColumnSpec<Person, PersonColumn>> = remember {
        tableColumns<Person, PersonColumn> {
            column(PersonColumn.NAME) {
                header("Name")
                title { "Name" }
                filter(TableFilterType.TextTableFilter())
                cell { item -> Text(item.name) }
            }
            column(PersonColumn.AGE) {
                header("Age")
                title { "Age" }
                filter(
                    TableFilterType.NumberTableFilter(
                        delegate = TableFilterType.NumberTableFilter.IntDelegate,
                        rangeOptions = 0 to 100,
                    ),
                )
                align(androidx.compose.ui.Alignment.End)
                cell { item ->
                    Text(
                        item.age.toString(),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            column(PersonColumn.ACTIVE) {
                header("Active")
                title { "Active" }
                filter(TableFilterType.BooleanTableFilter())
                cell { item -> Text(if (item.active) "Yes" else "No") }
            }
            column(PersonColumn.NAME_LENGTH) {
                header("Name length")
                title { "Name length" }
                filter(
                    TableFilterType.NumberTableFilter(
                        delegate = TableFilterType.NumberTableFilter.IntDelegate,
                        rangeOptions = 0 to 100,
                    ),
                )
                align(androidx.compose.ui.Alignment.End)
                cell { item ->
                    Text(item.name.length.toString(), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
            column(PersonColumn.AGE_GROUP) {
                header("Age group")
                title { "Age group" }
                filter(TableFilterType.TextTableFilter())
                cell { item ->
                    val group = when {
                        item.age < 25 -> "<25"
                        item.age < 35 -> "25-34"
                        else -> "35+"
                    }
                    Text(group)
                }
            }
            column(PersonColumn.AGE_TWICE) {
                header("Age x2")
                title { "Age x2" }
                filter(
                    TableFilterType.NumberTableFilter(
                        delegate = TableFilterType.NumberTableFilter.IntDelegate,
                        rangeOptions = 0 to 200,
                    ),
                )
                align(androidx.compose.ui.Alignment.End)
                cell { item ->
                    Text((item.age * 2).toString(), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
            column(PersonColumn.ACTIVE_INT) {
                header("Active (0/1)")
                title { "Active (0/1)" }
                filter(
                    TableFilterType.NumberTableFilter(
                        delegate = TableFilterType.NumberTableFilter.IntDelegate,
                        rangeOptions = 0 to 1,
                    ),
                )
                align(androidx.compose.ui.Alignment.End)
                cell { item ->
                    Text(
                        (if (item.active) 1 else 0).toString(),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            column(PersonColumn.FIRST_LETTER) {
                header("First letter")
                title { "First letter" }
                filter(TableFilterType.TextTableFilter())
                cell { item -> Text(item.name.firstOrNull()?.uppercase() ?: "") }
            }
            column(PersonColumn.HAS_E_IN_NAME) {
                header("Has 'e' in name")
                title { "Has 'e' in name" }
                filter(TableFilterType.BooleanTableFilter())
                cell { item -> Text(if (item.name.contains('e', ignoreCase = true)) "Yes" else "No") }
            }
            column(PersonColumn.NAME_REVERSED) {
                header("Name reversed")
                title { "Name reversed" }
                filter(TableFilterType.TextTableFilter())
                cell { item -> Text(item.name.reversed()) }
            }
        }
    }

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
        Column(modifier = Modifier.fillMaxSize()) {
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
                PersonColumn.NAME_LENGTH -> "Name length"
                PersonColumn.AGE_GROUP -> "Age group"
                PersonColumn.AGE_TWICE -> "Age x2"
                PersonColumn.ACTIVE_INT -> "Active (0/1)"
                PersonColumn.FIRST_LETTER -> "First letter"
                PersonColumn.HAS_E_IN_NAME -> "Has 'e' in name"
                PersonColumn.NAME_REVERSED -> "Name reversed"
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
            PersonColumn.NAME_LENGTH -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.AGE_GROUP -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.AGE_TWICE -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.ACTIVE_INT -> TableFilterState<Int>(constraint = null, values = null)
            PersonColumn.FIRST_LETTER -> TableFilterState<String>(constraint = null, values = null)
            PersonColumn.HAS_E_IN_NAME -> TableFilterState<Boolean>(constraint = FilterConstraint.EQUALS, values = null)
            PersonColumn.NAME_REVERSED -> TableFilterState<String>(constraint = null, values = null)
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

            PersonColumn.NAME_LENGTH -> {
                val value = person.name.length
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

            PersonColumn.AGE_TWICE -> {
                val value = person.age * 2
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

            PersonColumn.ACTIVE_INT -> {
                val value = if (person.active) 1 else 0
                val st = stateAny as TableFilterState<Int>
                val constraint = st.constraint ?: continue
                val ok = when (constraint) {
                    FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
                    FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
                    FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
                    FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
                    FilterConstraint.EQUALS -> value == (st.values?.getOrNull(0) ?: value)
                    FilterConstraint.NOT_EQUALS -> value != (st.values?.getOrNull(0) ?: value)
                    else -> true
                }
                if (!ok) return false
            }

            PersonColumn.FIRST_LETTER -> {
                val value = person.name.firstOrNull()?.uppercase() ?: ""
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

            PersonColumn.HAS_E_IN_NAME -> {
                val value = person.name.contains('e', ignoreCase = true)
                val st = stateAny as TableFilterState<Boolean>
                val constraint = st.constraint ?: continue
                val ok = when (constraint) {
                    FilterConstraint.EQUALS -> st.values?.firstOrNull()?.let { v -> value == v } ?: true
                    FilterConstraint.NOT_EQUALS -> st.values?.firstOrNull()?.let { v -> value != v } ?: true
                    else -> true
                }
                if (!ok) return false
            }

            PersonColumn.NAME_REVERSED -> {
                val value = person.name.reversed()
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
