package ua.wwind.table.filter.component

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
import ua.wwind.table.filter.BooleanFilter
import ua.wwind.table.filter.DateFilter
import ua.wwind.table.filter.EnumFilter
import ua.wwind.table.filter.NumberFilter
import ua.wwind.table.filter.TextFilter
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider

@Suppress("LongParameterList", "UNCHECKED_CAST")
@Composable
internal fun <T> FilterPanel(
    type: TableFilterType<T>?,
    state: TableFilterState<T>?,
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val filter = type) {
                is TableFilterType.TextTableFilter ->
                    TextFilter(
                        filter = filter,
                        state =
                            state as? TableFilterState<String>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as TableFilterState<T>) },
                    )

                is TableFilterType.NumberTableFilter<*> ->
                    NumberFilter<Number>(
                        filter = filter as TableFilterType.NumberTableFilter<Number>,
                        state =
                            state as? TableFilterState<Number>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as TableFilterState<T>) },
                    )

                is TableFilterType.BooleanTableFilter ->
                    BooleanFilter(
                        filter = filter,
                        state =
                            state as? TableFilterState<Boolean>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as TableFilterState<T>) },
                    )

                is TableFilterType.DateTableFilter ->
                    DateFilter(
                        filter = filter,
                        state =
                            state as? TableFilterState<kotlinx.datetime.LocalDate>
                                ?: TableFilterState(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as TableFilterState<T>) },
                    )

                is TableFilterType.EnumTableFilter<*> ->
                    EnumFilter(
                        filter = filter,
                        state =
                            state as? TableFilterState<*>
                                ?: TableFilterState<Any?>(constraint = null, values = null),
                        onClose = onDismissRequest,
                        strings = strings,
                        autoApplyFilters = autoApplyFilters,
                        autoFilterDebounce = autoFilterDebounce,
                        onChange = { onChange(it as TableFilterState<T>) },
                    )

                null -> error("Filter type cannot be null")
            }
        }
    }
}
