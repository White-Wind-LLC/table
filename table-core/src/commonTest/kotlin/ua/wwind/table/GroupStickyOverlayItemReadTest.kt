package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private data class GroupedRow(
    val id: Int,
    val group: String,
)

private fun groupedColumns() =
    tableColumns<GroupedRow, String, Unit> {
        column("group", valueOf = { it.group }) {
            header("Group")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text(item.group) }
            footer { Text("total") }
        }
        column("id", valueOf = { it.id }) {
            header("Id")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text("row-${item.id}") }
        }
    }

/**
 * The sticky group header reads the first visible row and the one after it; neither read may go
 * past the last row, where a `List.get` loader throws and a paged one reports a phantom row (#115).
 */
@OptIn(ExperimentalTestApi::class)
class GroupStickyOverlayItemReadTest {
    @Test
    fun `a grouped table with one row reads no index past it`() =
        runComposeUiTest {
            val rows = listOf(GroupedRow(0, "a"))
            val reads = mutableSetOf<Int>()
            setContent {
                val columns = remember { groupedColumns() }
                val state = rememberTableState(columns = persistentListOf("group", "id"))
                remember { state.groupBy("group") }
                Box(Modifier.size(400.dp, 320.dp)) {
                    Table(
                        itemsCount = rows.size,
                        itemAt = { index ->
                            reads += index
                            rows[index]
                        },
                        state = state,
                        columns = columns,
                    )
                }
            }
            waitForIdle()
            assertThat(reads).isEqualTo(setOf(0))
        }

    @Test
    fun `the last row scrolled to the top reads no index past it`() =
        runComposeUiTest {
            val count = 50
            val reads = mutableSetOf<Int>()
            val listState = LazyListState()
            setContent {
                val columns = remember { groupedColumns() }
                val state = rememberTableState(columns = persistentListOf("group", "id"))
                remember { state.groupBy("group") }
                // Room for the header and about one row, so the last row can reach the top.
                Box(Modifier.size(400.dp, 90.dp)) {
                    Table(
                        itemsCount = count,
                        itemAt = { index ->
                            reads += index
                            GroupedRow(index, "g${index / 10}")
                        },
                        state = state,
                        columns = columns,
                        verticalState = listState,
                    )
                }
            }
            waitForIdle()
            runOnIdle { listState.requestScrollToItem(count - 1) }
            waitForIdle()
            assertThat(listState.firstVisibleItemIndex).isEqualTo(count - 1)
            assertThat(reads).contains(count - 1)
            assertThat(reads.max()).isLessThan(count)
        }

    @Test
    fun `a scrolling footer at the top reads no index past the last row`() =
        runComposeUiTest {
            val count = 3
            val reads = mutableSetOf<Int>()
            val listState = LazyListState()
            setContent {
                val columns = remember { groupedColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("group", "id"),
                        settings = TableSettings(showFooter = true, footerPinned = false),
                    )
                remember { state.groupBy("group") }
                Box(Modifier.size(400.dp, 90.dp)) {
                    Table(
                        itemsCount = count,
                        itemAt = { index ->
                            reads += index
                            GroupedRow(index, "a")
                        },
                        state = state,
                        columns = columns,
                        verticalState = listState,
                    )
                }
            }
            waitForIdle()
            runOnIdle { listState.requestScrollToItem(count) }
            waitForIdle()
            assertThat(listState.firstVisibleItemIndex).isEqualTo(count)
            assertThat(reads.max()).isLessThan(count)
        }
}
