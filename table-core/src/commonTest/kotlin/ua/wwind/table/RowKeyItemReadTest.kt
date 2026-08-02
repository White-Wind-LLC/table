package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private data class KeyedRow(
    val id: Int,
)

private fun keyedColumns() =
    tableColumns<KeyedRow, String, Unit> {
        column("name", valueOf = { it.id }) {
            header("Name")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text("row-${item.id}") }
        }
    }

/**
 * A row read must mean "this row is on screen": Compose asks for keys over 130 rows from the top of
 * the list, and a source that loads on access reads every one of those as a viewport (issue #60).
 *
 * The viewport holds ~6 of the 1000 rows; the bound absorbs what Compose lays out past it.
 */
@OptIn(ExperimentalTestApi::class)
class RowKeyItemReadTest {
    @Test
    fun `the default rowKey resolves no row beyond the viewport`() =
        runComposeUiTest {
            val reads = mutableSetOf<Int>()
            setContent {
                val columns = remember { keyedColumns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp, 320.dp)) {
                    Table(
                        itemsCount = 1000,
                        itemAt = { index ->
                            reads += index
                            KeyedRow(index)
                        },
                        state = state,
                        columns = columns,
                    )
                }
            }
            waitForIdle()
            assertThat(reads).contains(0)
            assertThat(reads.max()).isLessThan(60)
        }

    @Test
    fun `a rowKeyAt resolves no row beyond the viewport`() =
        runComposeUiTest {
            val reads = mutableSetOf<Int>()
            setContent {
                val columns = remember { keyedColumns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp, 320.dp)) {
                    Table(
                        itemsCount = 1000,
                        itemAt = { index ->
                            reads += index
                            KeyedRow(index)
                        },
                        state = state,
                        columns = columns,
                        rowKeyAt = { index -> "key-$index" },
                    )
                }
            }
            waitForIdle()
            assertThat(reads).contains(0)
            assertThat(reads.max()).isLessThan(60)
        }

    @Test
    fun `a plain rowKey still receives the item of a rendered row`() =
        runComposeUiTest {
            val keyed = mutableMapOf<Int, KeyedRow?>()
            setContent {
                val columns = remember { keyedColumns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp, 320.dp)) {
                    Table(
                        itemsCount = 1000,
                        itemAt = { index -> KeyedRow(index) },
                        state = state,
                        columns = columns,
                        rowKey = { item, index ->
                            keyed[index] = item
                            item?.id ?: index
                        },
                    )
                }
            }
            waitForIdle()
            assertThat(keyed[0]).isNotNull()
        }
}
