package ua.wwind.table.filter.component.fast

import androidx.compose.runtime.Composable
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType

/**
 * Custom fast filter component that delegates rendering to user-provided custom filter.
 * This component follows the same pattern as FastTextFilter, FastNumberFilter, etc.
 *
 * @param filter the custom filter type containing the renderer
 * @param state the current filter state from the table
 * @param tableData current table data for accessing context during filter rendering
 * @param onChange callback to update the filter state
 */
@Suppress("UNCHECKED_CAST")
@Composable
internal fun <T : Any, E> CustomFastFilter(
    filter: TableFilterType.CustomTableFilter<T, E>,
    state: TableFilterState<T>?,
    tableData: E,
    onChange: (TableFilterState<T>?) -> Unit,
) {
    // Delegate to user's custom fast filter implementation
    filter.renderFilter.RenderFastFilter(
        currentState = state,
        tableData = tableData,
        onChange = onChange,
    )
}
