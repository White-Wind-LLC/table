package ua.wwind.table.sample.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonEditState
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings

@OptIn(ExperimentalTableApi::class)
@Composable
fun MainTable(
    displayedPeople: List<Person>,
    state: TableState<PersonColumn>,
    editState: PersonEditState,
    columns: ImmutableList<ColumnSpec<Person, PersonColumn, PersonEditState>>,
    customization: TableCustomization<Person, PersonColumn>,
    onFiltersChanged: (Map<PersonColumn, TableFilterState<*>>) -> Unit,
    onSortChanged: (SortState<PersonColumn>?) -> Unit,
    onRowEditStart: (Person, Int) -> Unit,
    onRowEditComplete: (Int) -> Boolean,
    onEditCancelled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Observe filters and sort state changes
    LaunchedEffect(state) {
        snapshotFlow { state.filters.toMap() }.collect { filters -> onFiltersChanged(filters) }
    }

    LaunchedEffect(state) { snapshotFlow { state.sort }.collect { sort -> onSortChanged(sort) } }

    EditableTable(
        itemsCount = displayedPeople.size,
        itemAt = { index -> displayedPeople.getOrNull(index) },
        state = state,
        editState = editState,
        columns = columns,
        customization = customization,
        strings = DefaultStrings,
        rowKey = { item, index -> index },
        rowEmbedded = { _, person ->
            val visible = person.expandedMovement
            AnimatedVisibility(
                visible = visible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        thickness = state.dimensions.dividerThickness,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PersonMovementsSection(person = person)
                }
            }
        },
        onRowEditStart = onRowEditStart,
        onRowEditComplete = onRowEditComplete,
        onEditCancelled = onEditCancelled,
        modifier = modifier,
    )
}
