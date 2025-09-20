package ua.wwind.table

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
@Suppress("LongParameterList")
public fun <T : Any, C> TableActiveFilters(
    columns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    strings: StringProvider = DefaultStrings,
    modifier: Modifier = Modifier,
    includeClearAllChip: Boolean = true,
) {
    val keyToSpec: Map<C, ColumnSpec<T, C>> = remember(columns) { columns.associateBy { it.key } }
    val activeFilters: List<Pair<C, TableFilterState<*>>> =
        state.filters.filter { (_, st) -> st.values?.isEmpty() == false }.toList()
    if (activeFilters.isEmpty()) return

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (includeClearAllChip) {
            InputChip(
                selected = true,
                onClick = {
                    activeFilters.forEach { (col, _) ->
                        val filterType = keyToSpec[col]?.filter as? TableFilterType<*>
                        val defaultConstraint = filterType?.constraints?.firstOrNull() ?: FilterConstraint.EQUALS
                        state.setFilter(col, TableFilterState<Any?>(defaultConstraint, null))
                    }
                },
                label = { Text(strings.get(UiString.FilterClear)) },
                trailingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Close,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        activeFilters.forEach { (col, st) ->
            val spec = keyToSpec[col]
            val title = spec?.title?.invoke() ?: col.toString()
            val text = spec?.filter?.let { ft -> buildFilterChipTextUnsafe(ft, st, strings) }
            if (text != null) {
                InputChip(
                    selected = true,
                    onClick = {
                        val defaultConstraint = spec.filter.constraints.firstOrNull() ?: FilterConstraint.EQUALS
                        state.setFilter(col, TableFilterState<Any?>(defaultConstraint, null))
                    },
                    label = { Text("$title: $text") },
                    trailingIcon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.Close,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
@Suppress("UNCHECKED_CAST", "CyclomaticComplexMethod", "ReturnCount")
private fun buildFilterChipTextUnsafe(
    filterType: TableFilterType<*>,
    state: TableFilterState<*>,
    strings: StringProvider,
): String? {
    return when (filterType) {
        is TableFilterType.TextTableFilter -> {
            val s = state as? TableFilterState<String> ?: return null
            val constraint = s.constraint ?: return null
            val value = s.values?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            "${strings.get(constraint.toUiString())} \"$value\""
        }

        is TableFilterType.NumberTableFilter<*> -> {
            val s = state as? TableFilterState<Number> ?: return null
            val constraint = s.constraint ?: return null
            val list = s.values ?: emptyList()
            if (constraint == FilterConstraint.BETWEEN && list.size >= 2) {
                val from = list[0]
                val to = list[1]
                "${strings.get(constraint.toUiString())} $from - $to"
            } else {
                val single = list.firstOrNull() ?: return null
                "${strings.get(constraint.toUiString())} $single"
            }
        }

        is TableFilterType.BooleanTableFilter -> {
            val s = state as? TableFilterState<Boolean> ?: return null
            val value = s.values?.firstOrNull() ?: return null
            val valueTitle =
                if (value) strings.get(UiString.BooleanTrueTitle) else strings.get(UiString.BooleanFalseTitle)
            "${strings.get(FilterConstraint.EQUALS.toUiString())} $valueTitle"
        }

        is TableFilterType.DateTableFilter -> {
            val s = state as? TableFilterState<kotlinx.datetime.LocalDate> ?: return null
            val constraint = s.constraint ?: return null
            val value = s.values?.firstOrNull() ?: return null
            "${strings.get(constraint.toUiString())} $value"
        }

        is TableFilterType.EnumTableFilter<*> -> {
            val s = state as? TableFilterState<*> ?: return null
            val constraint = s.constraint ?: return null
            val list = s.values ?: return null
            when (constraint) {
                FilterConstraint.EQUALS -> {
                    val single = (list.singleOrNull() as? Enum<*>) ?: return null
                    val title = (filterType.getTitle as @Composable (Enum<*>) -> String).invoke(single)
                    "${strings.get(constraint.toUiString())} $title"
                }

                FilterConstraint.IN, FilterConstraint.NOT_IN -> {
                    if (list.isEmpty()) return null
                    val titles =
                        list
                            .mapNotNull { it as? Enum<*> }
                            .map { (filterType.getTitle as @Composable (Enum<*>) -> String).invoke(it) }
                    val joined = titles.joinToString(", ")
                    "${strings.get(constraint.toUiString())} $joined"
                }

                else -> null
            }
        }
    }
}
