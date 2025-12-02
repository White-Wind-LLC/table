package ua.wwind.table.filter.component.main.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider

/**
 * Custom filter component that wraps user-provided custom filter UI with standard FilterPanelActions.
 * This component follows the same pattern as NumberFilter, TextFilter, etc.
 *
 * @param filter the custom filter type containing the renderer and state provider
 * @param state the current filter state from the table
 * @param tableData current table data for accessing context during filter rendering
 * @param onClose callback to close the filter panel
 * @param strings string provider for localization
 * @param autoApplyFilters whether filters should apply automatically or require Apply button
 * @param onChange callback to update the filter state
 */
@Suppress("UNCHECKED_CAST")
@Composable
internal fun <T : Any, E> CustomFilter(
    filter: TableFilterType.CustomTableFilter<T, E>,
    state: TableFilterState<T>?,
    tableData: E,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    onChange: (TableFilterState<T>?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Render user's custom filter content
        val actions =
            filter.renderFilter.RenderPanel(
                currentState = state,
                tableData = tableData,
                onDismiss = onClose,
                onChange = onChange,
            )

        // Standard action buttons (Clear + optional Apply)
        FilterPanelActions(
            autoApplyFilters = autoApplyFilters,
            enabled = true,
            onApply = { actions.applyFilter() },
            onClear = { actions.clearFilter() },
            onClose = onClose,
            strings = strings,
        )
    }
}
