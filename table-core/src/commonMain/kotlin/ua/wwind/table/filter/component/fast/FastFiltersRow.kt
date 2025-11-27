package ua.wwind.table.filter.component.fast

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.calculateFixedColumnState
import ua.wwind.table.strings.StringProvider

private const val FAST_FILTER_ROW_HEIGHT = 40

@Composable
internal fun <T : Any, C, E> FastFiltersRow(
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    widthResolver: (C) -> Dp,
    rowContainerColor: Color,
    state: TableState<C>,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C, E>, TableFilterState<T>?) -> Unit,
    modifier: Modifier = Modifier,
    horizontalState: ScrollState,
) {
    val settings = state.settings

    Column {
        HorizontalDivider(
            modifier = Modifier.width(state.tableWidth),
            thickness = state.dimensions.dividerThickness,
        )
        LazyRow(
            modifier = modifier.height(FAST_FILTER_ROW_HEIGHT.dp).width(state.tableWidth),
            state = rememberLazyListState(),
            userScrollEnabled = false,
        ) {
            items(items = visibleColumns, key = { item -> item.key as Any }) { spec ->
                val index = visibleColumns.indexOf(spec)
                val filterType = spec.filter as? TableFilterType<Any?>
                val autoFilterDebounce = state.settings.autoFilterDebounce

                val fixedState =
                    calculateFixedColumnState(
                        columnIndex = index,
                        totalVisibleColumns = visibleColumns.size,
                        fixedColumnsCount = settings.fixedColumnsCount,
                        fixedColumnsSide = settings.fixedColumnsSide,
                        horizontalState = horizontalState,
                    )

                val surfaceColor =
                    if (fixedState.isFixed) {
                        if (rowContainerColor != Unspecified) {
                            rowContainerColor.copy(alpha = 1f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    } else {
                        rowContainerColor
                    }

                Surface(
                    color = surfaceColor,
                    modifier =
                        Modifier
                            .zIndex(fixedState.zIndex)
                            .graphicsLayer {
                                this.translationX = fixedState.translationX
                            },
                ) {
                    val width = widthResolver(spec.key)

                    Row {
                        if (fixedState.isFirstRightFixed) {
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                thickness = state.dimensions.fixedColumnDividerThickness,
                            )
                        }
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
                                        state =
                                            state.filters[spec.key] as? TableFilterState<String>
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

                                is TableFilterType.CustomTableFilter<*> -> {
                                    val customFilter = filterType as TableFilterType.CustomTableFilter<Any>
                                    CustomFastFilter<Any>(
                                        filter = customFilter,
                                        state = state.filters[spec.key] as? TableFilterState<Any>,
                                        onChange = { newState ->
                                            onChange(spec, newState as? TableFilterState<T>)
                                        },
                                    )
                                }

                                TableFilterType.DisabledTableFilter, null -> {
                                    // No filter for this column
                                }
                            }
                        }
                        if (!fixedState.isLastBeforeRightFixed) {
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                thickness =
                                    if (fixedState.isLastLeftFixed) {
                                        state.dimensions.fixedColumnDividerThickness
                                    } else {
                                        state.dimensions.dividerThickness
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
