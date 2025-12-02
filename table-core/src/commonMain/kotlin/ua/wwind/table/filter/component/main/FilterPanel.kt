package ua.wwind.table.filter.component.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.component.main.booleann.BooleanFilter
import ua.wwind.table.filter.component.main.custom.CustomFilter
import ua.wwind.table.filter.component.main.date.DateFilter
import ua.wwind.table.filter.component.main.enumm.EnumFilter
import ua.wwind.table.filter.component.main.number.NumberFilter
import ua.wwind.table.filter.component.main.text.TextFilter
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider

@Suppress("LongParameterList", "UNCHECKED_CAST")
@Composable
internal fun <T, E> FilterPanel(
    type: TableFilterType<T>?,
    state: TableFilterState<T>?,
    tableData: E,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<T>?) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(0.dp, 8.dp),
        modifier = Modifier.width(280.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (type) {
                is TableFilterType.TextTableFilter ->
                    TextFilter(
                        filter = type,
                        state =
                            state as? TableFilterState<String>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as? TableFilterState<T>) },
                    )

                is TableFilterType.NumberTableFilter<*> ->
                    NumberFilter<Number>(
                        filter = type as TableFilterType.NumberTableFilter<Number>,
                        state =
                            state as? TableFilterState<Number>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as? TableFilterState<T>) },
                    )

                is TableFilterType.BooleanTableFilter ->
                    BooleanFilter(
                        filter = type,
                        state =
                            state as? TableFilterState<Boolean>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as? TableFilterState<T>) },
                    )

                is TableFilterType.DateTableFilter ->
                    DateFilter(
                        filter = type,
                        state =
                            state as? TableFilterState<LocalDate>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as? TableFilterState<T>) },
                    )

                is TableFilterType.EnumTableFilter<*> ->
                    EnumFilter(
                        filter = type,
                        state =
                            state as? TableFilterState<*>
                                ?: TableFilterState<Any?>(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as? TableFilterState<T>) },
                    )

                is TableFilterType.CustomTableFilter<*, *> -> {
                    val customFilter = type as TableFilterType.CustomTableFilter<Any, E>
                    CustomFilter<Any, E>(
                        filter = customFilter,
                        state = state as? TableFilterState<Any>,
                        tableData = tableData,
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        onChange = { onChange(it as? TableFilterState<T>) },
                    )
                }

                TableFilterType.DisabledTableFilter -> {
                    // No-op
                }

                null -> error("Filter type cannot be null")
            }
        }
    }
}
