package ua.wwind.table.sample.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.oikvpqya.compose.fastscroller.HorizontalScrollbar
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.defaultScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonTableData
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTableApi::class)
@Composable
fun MainTable(
    state: TableState<PersonColumn>,
    tableData: PersonTableData,
    columns: ImmutableList<ColumnSpec<Person, PersonColumn, PersonTableData>>,
    customization: TableCustomization<Person, PersonColumn>,
    onFiltersChanged: (Map<PersonColumn, TableFilterState<*>>) -> Unit,
    onSortChanged: (SortState<PersonColumn>?) -> Unit,
    onRowMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onMovementRowMove: (person: Person, fromIndex: Int, toIndex: Int) -> Unit,
    onRowEditStart: (Person, Int) -> Unit,
    onRowEditComplete: (Int) -> Boolean,
    onEditCancelled: (Int) -> Unit,
    useCompactMode: Boolean = false,
    enableRowReorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Observe filters and sort state changes
    LaunchedEffect(state) {
        snapshotFlow { state.filters.toMap() }.collect { filters -> onFiltersChanged(filters) }
    }

    LaunchedEffect(state) { snapshotFlow { state.sort }.collect { sort -> onSortChanged(sort) } }
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()
    var showVerticalScrollbar by remember { mutableStateOf(false) }
    var showHorizontalScrollbar by remember { mutableStateOf(false) }

    LaunchedEffect(verticalState.isScrollInProgress) {
        if (verticalState.isScrollInProgress) {
            showVerticalScrollbar = true
        } else {
            delay(1.seconds)
            if (!verticalState.isScrollInProgress) {
                showVerticalScrollbar = false
            }
        }
    }

    LaunchedEffect(horizontalState.isScrollInProgress) {
        if (horizontalState.isScrollInProgress) {
            showHorizontalScrollbar = true
        } else {
            delay(1.seconds)
            if (!horizontalState.isScrollInProgress) {
                showHorizontalScrollbar = false
            }
        }
    }

    Box(modifier = modifier) {
        EditableTable(
            itemsCount = tableData.displayedPeople.size,
            itemAt = { index -> tableData.displayedPeople.getOrNull(index) },
            state = state,
            tableData = tableData,
            columns = columns,
            customization = customization,
            strings = DefaultStrings,
            verticalState = verticalState,
            horizontalState = horizontalState,
            rowKey = { person, index -> person?.id?.toString() ?: "_$index" },
            rowEmbedded = { _, person ->
                val visible = person.expandedMovement
                if (visible) {
                    HorizontalDivider(
                        thickness = state.dimensions.dividerThickness,
                        modifier = Modifier.width(state.tableWidth),
                    )
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    PersonMovementsSection(
                        person = person,
                        useCompactMode = useCompactMode,
                        enableRowReorder = enableRowReorder,
                        onRowMove = { from, to ->
                            onMovementRowMove(person, from, to)
                        },
                    )
                }
            },
            onRowMove = onRowMove,
            onRowEditStart = onRowEditStart,
            onRowEditComplete = onRowEditComplete,
            onEditCancelled = onEditCancelled,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(
            visible = showVerticalScrollbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = verticalState),
                style = defaultScrollbarStyle(),
            )
        }

        AnimatedVisibility(
            visible = showHorizontalScrollbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            HorizontalScrollbar(
                modifier = Modifier.fillMaxWidth(),
                adapter = rememberScrollbarAdapter(scrollState = horizontalState),
                style = defaultScrollbarStyle(),
            )
        }
    }
}
