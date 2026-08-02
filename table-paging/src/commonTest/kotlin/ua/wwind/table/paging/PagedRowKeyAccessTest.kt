package ua.wwind.table.paging

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
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.paging.core.PagingMap
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

private data class Person(
    val id: Int,
)

private const val TOTAL = 1000

/** Every position is loaded, so an access can only come from the table asking for that row. */
private fun pagingData(onGet: (Int) -> Unit): PagingData<Person> =
    PagingData(
        data =
            PagingMap(
                size = TOTAL,
                values = (0 until TOTAL).associateWith { Person(it) }.toPersistentMap(),
                onGet = onGet,
            ),
        loadState = LoadState.Success,
        retry = {},
    )

private fun personColumns() =
    tableColumns<Person, String, Unit> {
        column("name", valueOf = { it.id }) {
            header("Name")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text("row-${item.id}") }
        }
    }

/**
 * `PagingMap.get` tells the pager where the viewport is, so only rendered rows may reach it — the
 * key map spans 130 rows against the handful on screen (issue #60). A custom `rowKey` still needs
 * its row, which the adapter takes from the map directly.
 */
@OptIn(ExperimentalTestApi::class)
class PagedRowKeyAccessTest {
    @Test
    fun `a custom rowKey does not report a viewport past the rendered rows`() =
        runComposeUiTest {
            val accessed = mutableSetOf<Int>()
            setContent {
                val columns = remember { personColumns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                val items = remember { pagingData { accessed += it } }
                Box(Modifier.size(400.dp, 320.dp)) {
                    Table(
                        items = items,
                        state = state,
                        columns = columns,
                        rowKey = { person, index -> person?.id ?: index },
                    )
                }
            }
            waitForIdle()
            assertThat(accessed).contains(0)
            assertThat(accessed.max()).isLessThan(60)
        }

    @Test
    fun `a custom rowKey still keys rendered rows by their item`() =
        runComposeUiTest {
            val keys = mutableMapOf<Int, Any>()
            setContent {
                val columns = remember { personColumns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                val items = remember { pagingData { } }
                Box(Modifier.size(400.dp, 320.dp)) {
                    Table(
                        items = items,
                        state = state,
                        columns = columns,
                        rowKey = { person, index ->
                            val key = person?.let { "id-${it.id}" } ?: "placeholder-$index"
                            keys[index] = key
                            key
                        },
                    )
                }
            }
            waitForIdle()
            assertThat(keys[0]).isEqualTo("id-0")
        }
}
