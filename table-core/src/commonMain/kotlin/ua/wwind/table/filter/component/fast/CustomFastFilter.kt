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
 * @param onChange callback to update the filter state
 */
@Suppress("UNCHECKED_CAST")
@Composable
internal fun <T : Any> CustomFastFilter(
    filter: TableFilterType.CustomTableFilter<T>,
    state: TableFilterState<T>?,
    onChange: (TableFilterState<T>?) -> Unit,
) {
    // Delegate to user's custom fast filter implementation
    filter.renderFilter.RenderFastFilter(
        currentState = state,
        onChange = onChange,
    )
}
