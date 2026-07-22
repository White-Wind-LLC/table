package ua.wwind.table.sample.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.RowBlockMove
import ua.wwind.table.RowBlocks
import ua.wwind.table.RowWithinBlockMove
import ua.wwind.table.Table
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.draggableHandle
import ua.wwind.table.sample.column.createMovementColumns
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonMovement
import ua.wwind.table.sample.model.PersonMovementColumn
import ua.wwind.table.sample.model.movementBlockId
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings

@Composable
fun PersonMovementsSection(
    person: Person,
    useCompactMode: Boolean = false,
    enableRowReorder: Boolean = false,
    enableRowBlocks: Boolean = false,
    onRowMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onBlockMove: (RowBlockMove) -> Unit = {},
    onRowWithinBlockMove: (RowWithinBlockMove) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val columns =
        remember(useCompactMode, enableRowReorder) {
            createMovementColumns(useCompactMode, enableRowReorder = enableRowReorder)
        }
    val movementSettings =
        remember(enableRowReorder) {
            TableSettings(
                rowReorderEnabled = enableRowReorder,
                autoApplyFilters = false,
                showFastFilters = false,
                autoFilterDebounce = 0,
                stripedRows = false,
                showActiveFiltersHeader = false,
                selectionMode = SelectionMode.None,
                rowHeightMode = RowHeightMode.Dynamic,
                enableDragToScroll = false,
                showFooter = true,
            )
        }
    val movementState =
        rememberTableState(
            columns = PersonMovementColumn.entries.toImmutableList(),
            settings = movementSettings,
            dimensions = TableDefaults.compactDimensions(),
        )

    // The block declaration must stay one instance (identity equality) while the commit callback
    // tracks recompositions — each pass captures the current person — so the remembered config
    // reads the callback through rememberUpdatedState instead of being rebuilt around it.
    val currentOnBlockMove = rememberUpdatedState(onBlockMove)
    val currentOnRowWithinBlockMove = rememberUpdatedState(onRowWithinBlockMove)
    // Mirrors the reorder column in createMovementColumns.
    val handleColumnWidth = if (useCompactMode) 36.dp else 48.dp
    // Embedded blocks demo: adjacent same-year movements band and drag as one unit (from the header
    // handle), while each row reorders within its year block — driving the no-LazyColumn path of the
    // managed drag and its nested within-block column.
    val movementBlocks =
        remember(enableRowBlocks, handleColumnWidth) {
            if (!enableRowBlocks) {
                null
            } else {
                RowBlocks<PersonMovement>(
                    blockOf = { it.movementBlockId },
                    onCommit = { move -> currentOnBlockMove.value(move) },
                    onRowReorderWithinBlock = { move -> currentOnRowWithinBlockMove.value(move) },
                    blockHeader = { blockId, _ ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            // Same width as the row-handle column, so the handles line up.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.width(handleColumnWidth),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "Drag year $blockId",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp).draggableHandle(),
                                )
                            }
                            Text(
                                text = "Year $blockId",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            }
        }

    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 8.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "HR movements",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Table(
            itemsCount = person.movements.size,
            itemAt = { index -> person.movements.getOrNull(index) },
            state = movementState,
            tableData = person,
            columns = columns,
            strings = DefaultStrings,
            rowKey = { item, index -> item?.date ?: index },
            modifier = Modifier.padding(top = 8.dp),
            embedded = true,
            onRowMove = onRowMove,
            rowBlocks = movementBlocks,
        )
    }
}
