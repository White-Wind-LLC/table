package ua.wwind.table.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import ua.wwind.table.component.body.LocalEditCellContext
import ua.wwind.table.component.body.LocalEditCellFocusRequester
import ua.wwind.table.state.LocalTableState

/**
 * Modifier extension that integrates an editable cell component with the table's focus management system.
 *
 * This modifier performs two key functions:
 * 1. Applies the table's [LocalEditCellFocusRequester] to enable programmatic focus control
 *    (e.g., when navigating between cells with Tab/Enter or when starting edit mode)
 * 2. Synchronizes the component's focused state with [TableState.selectedCell], so clicking
 *    on the field updates the table's selection state and triggers cell visibility/scrolling
 *
 * This ensures seamless integration with the table's cell selection and keyboard navigation system.
 * [TableCellTextField] already applies this modifier internally, so you only need to use it
 * explicitly with custom input components.
 *
 * Example usage with custom TextField:
 * ```kotlin
 * editCell { person, tableData, onComplete ->
 *     var text by remember(person) { mutableStateOf(person.name) }
 *
 *     TextField(
 *         value = text,
 *         onValueChange = { text = it },
 *         modifier = Modifier
 *             .syncEditCellFocus() // Integrates with table focus management
 *             .fillMaxSize()
 *     )
 * }
 * ```
 *
 * @return A [Modifier] that integrates the component with table's focus management system.
 */
@Composable
public fun Modifier.syncEditCellFocus(): Modifier {
    val focusRequester = LocalEditCellFocusRequester.current
    val editCellContext = LocalEditCellContext.current
    val tableState = LocalTableState.current

    return if (focusRequester != null && editCellContext != null) {
        this
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                // Update selectedCell when this component receives focus
                if (focusState.isFocused) {
                    tableState.selectCellUnchecked(
                        editCellContext.rowIndex,
                        editCellContext.columnAsAny,
                    )
                }
            }
    } else {
        this
    }
}
