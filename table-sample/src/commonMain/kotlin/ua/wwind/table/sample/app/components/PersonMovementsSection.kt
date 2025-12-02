package ua.wwind.table.sample.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.sample.column.createMovementColumns
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonMovementColumn
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings

@OptIn(ExperimentalTableApi::class)
@Composable
fun PersonMovementsSection(
    person: Person,
    useCompactMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val columns =
        remember(useCompactMode) {
            createMovementColumns(useCompactMode)
        }
    val movementSettings =
        remember {
            TableSettings(
                isDragEnabled = false,
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
        )
    }
}
