package ua.wwind.table.component.header

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.state.TableState
import ua.wwind.table.state.mapNotNullToImmutable

@Immutable
internal data class HeaderDerivedState<T : Any, C>(
    val visibleColumns: ImmutableList<ColumnSpec<T, C>>,
    val widthMap: Map<C, Dp>,
)

@Composable
internal fun <T : Any, C> rememberHeaderDerivedState(
    columns: ImmutableList<ColumnSpec<T, C>>,
    state: TableState<C>,
    dimensions: TableDimensions,
): HeaderDerivedState<T, C> {
    val keyToSpec = remember(columns) { columns.associateBy { it.key } }

    val visibleColumns by remember(columns, state.columnOrder) {
        derivedStateOf {
            state.columnOrder.mapNotNullToImmutable { key ->
                @Suppress("UNCHECKED_CAST")
                (keyToSpec[key] as ColumnSpec<T, C>?)?.takeIf { it.visible }
            }
        }
    }

    val widthMap by remember(columns, state.columnWidths, dimensions) {
        derivedStateOf {
            buildMap {
                columns.forEach { spec ->
                    val width = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
                    @Suppress("UNCHECKED_CAST")
                    put(spec.key as C, width)
                }
            }
        }
    }

    return remember(visibleColumns, widthMap) { HeaderDerivedState(visibleColumns, widthMap) }
}
