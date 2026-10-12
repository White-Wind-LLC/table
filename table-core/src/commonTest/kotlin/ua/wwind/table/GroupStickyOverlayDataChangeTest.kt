package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private data class SortedRow(
    val id: Int,
    val group: String,
)

private val initialRows = persistentListOf(SortedRow(0, "b"), SortedRow(1, "a"))
private val reorderedRows = persistentListOf(SortedRow(1, "a"), SortedRow(0, "b"))

@Composable
private fun GroupedTable(
    rows: ImmutableList<SortedRow>,
    columns: ImmutableList<ColumnSpec<SortedRow, String, Unit>>,
) {
    val state = rememberTableState(columns = persistentListOf("group"))
    remember { state.groupBy("group") }
    Box(Modifier.size(400.dp, 320.dp)) {
        Table(
            itemsCount = rows.size,
            itemAt = { index -> rows[index] },
            state = state,
            columns = columns,
        )
    }
}

@OptIn(ExperimentalTestApi::class)
class GroupStickyOverlayDataChangeTest {
    @Test
    fun `the sticky group header follows a reorder that keeps the row count and heights`() =
        runComposeUiTest {
            var rows by mutableStateOf(initialRows)
            val shownHeaders = mutableListOf<String>()
            setContent {
                val columns =
                    remember {
                        tableColumns<SortedRow, String, Unit> {
                            column("group", valueOf = { it.group }) {
                                header("Group")
                                width(120.dp, 120.dp)
                                resizable(false)
                                cell { item, _ -> Text(item.group) }
                                groupHeader { value ->
                                    DisposableEffect(value) {
                                        shownHeaders += value as String
                                        onDispose { shownHeaders -= value }
                                    }
                                    Text("group-$value")
                                }
                            }
                        }
                    }
                GroupedTable(rows, columns)
            }
            waitForIdle()
            assertThat(shownHeaders.sorted()).isEqualTo(listOf("a", "b", "b"))

            runOnIdle { rows = reorderedRows }
            waitForIdle()

            assertThat(shownHeaders.sorted()).isEqualTo(listOf("a", "a", "b"))
        }

    @Test
    fun `the default sticky group header renders the new first row after a reorder`() =
        runComposeUiTest {
            var rows by mutableStateOf(initialRows)
            val shownCells = mutableListOf<String>()
            setContent {
                val columns =
                    remember {
                        tableColumns<SortedRow, String, Unit> {
                            column("group", valueOf = { it.group }) {
                                header("Group")
                                width(120.dp, 120.dp)
                                resizable(false)
                                cell { item, _ ->
                                    DisposableEffect(item.id) {
                                        shownCells += "row-${item.id}"
                                        onDispose { shownCells -= "row-${item.id}" }
                                    }
                                    Text(item.group)
                                }
                            }
                        }
                    }
                GroupedTable(rows, columns)
            }
            waitForIdle()
            assertThat(shownCells.sorted()).isEqualTo(listOf("row-0", "row-0", "row-0", "row-1", "row-1"))

            runOnIdle { rows = reorderedRows }
            waitForIdle()

            assertThat(shownCells.sorted()).isEqualTo(listOf("row-0", "row-0", "row-1", "row-1", "row-1"))
        }
}
