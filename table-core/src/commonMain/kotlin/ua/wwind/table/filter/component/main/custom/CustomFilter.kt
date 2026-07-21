package ua.wwind.table.filter.component.main.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.CustomFilterPanelActions
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
        // Render user's custom filter content, which publishes its apply/clear behavior into the
        // handle. It composes ahead of the buttons below, so the handle is filled by the time
        // either can be pressed.
        val panelActions = remember { CustomFilterPanelActions() }
        filter.renderFilter.RenderPanel(
            currentState = state,
            tableData = tableData,
            panelActions = panelActions,
            onDismiss = onClose,
            onChange = onChange,
        )

        // Standard action buttons (Clear + optional Apply)
        FilterPanelActions(
            autoApplyFilters = autoApplyFilters,
            enabled = true,
            onApply = { panelActions.actions?.applyFilter() },
            onClear = { panelActions.actions?.clearFilter() },
            onClose = onClose,
            strings = strings,
        )
    }
}
