package ua.wwind.table.filter.component.fast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

private const val FAST_FILTER_ROW_HEIGHT = 48

@Composable
internal fun <T : Any, C> FastFiltersRow(
    tableWidth: Dp,
    leadingColumnWidth: Dp?,
    visibleColumns: ImmutableList<ColumnSpec<T, C>>,
    widthResolver: (C) -> Dp,
    rowContainerColor: Color,
    state: TableState<C>,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C>, TableFilterState<T>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        HorizontalDivider(
            modifier = Modifier.width(tableWidth),
            thickness = state.dimensions.dividerThickness,
        )
        LazyRow(
            modifier = modifier.height(FAST_FILTER_ROW_HEIGHT.dp).width(tableWidth),
            state = rememberLazyListState(),
            userScrollEnabled = false,
        ) {
            if (leadingColumnWidth != null) {
                item(key = "leading") {
                    Row {
                        Spacer(
                            modifier =
                                Modifier
                                    .width(leadingColumnWidth)
                                    .fillMaxHeight(),
                        )
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            thickness = state.dimensions.dividerThickness,
                        )
                    }
                }
            }

            items(items = visibleColumns, key = { item -> item.key as Any }) { spec ->
                val filterType = spec.filter as? TableFilterType<Any?>
                val autoFilterDebounce = state.settings.autoFilterDebounce
                Surface(
                    color = rowContainerColor,
                ) {
                    val width = widthResolver(spec.key) + state.dimensions.dividerThickness

                    Box(
                        modifier =
                            Modifier
                                .width(width)
                                .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (filterType) {
                            is TableFilterType.TextTableFilter ->
                                FastTextFilter(
                                    spec = spec,
                                    state = state.filters[spec.key] as? TableFilterState<String>
                                        ?: TableFilterState(constraint = null, values = null),
                                    autoFilterDebounce = autoFilterDebounce,
                                    strings = strings,
                                    onChange = onChange,
                                )

                            is TableFilterType.BooleanTableFilter ->
                                FastBooleanFilter(
                                    spec = spec,
                                    state = state.filters[spec.key] as? TableFilterState<Boolean>,
                                    autoFilterDebounce = autoFilterDebounce,
                                    strings = strings,
                                    onChange = onChange,
                                )

                            is TableFilterType.NumberTableFilter<*> ->
                                FastNumberFilter(
                                    spec = spec,
                                    state = state.filters[spec.key],
                                    autoFilterDebounce = autoFilterDebounce,
                                    strings = strings,
                                    onChange = onChange,
                                )

                            is TableFilterType.DateTableFilter ->
                                FastDateFilter(
                                    spec = spec,
                                    state = state.filters[spec.key] as? TableFilterState<LocalDate>,
                                    autoFilterDebounce = autoFilterDebounce,
                                    strings = strings,
                                    onChange = onChange,
                                )

                            is TableFilterType.EnumTableFilter<*> ->
                                FastEnumFilter(
                                    spec = spec,
                                    state = state.filters[spec.key],
                                    autoFilterDebounce = autoFilterDebounce,
                                    strings = strings,
                                    onChange = onChange,
                                )

                            null -> {
                                // No filter for this column
                            }
                        }
                        VerticalDivider(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            thickness = state.dimensions.dividerThickness,
                        )
                    }
                }
            }
        }
    }
}

