package ua.wwind.table.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.paging.core.PagingMap
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

private fun columns() =
    tableColumns<String, String, Unit> {
        column("name", valueOf = { it }) {
            header("Name")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text(item) }
        }
    }

/** `items == null` means the first page is still loading, which is not the same as no data. */
@OptIn(ExperimentalTestApi::class)
class PagedEmptyStateTest {
    @Test
    fun `a table still loading its first page shows no empty state`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(items = null, state = state, columns = columns)
                }
            }
            onNodeWithText("Name").assertIsDisplayed()
            onNodeWithText("No data").assertDoesNotExist()
        }

    @Test
    fun `a loaded empty page says there is no data`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                val items =
                    remember {
                        PagingData(
                            data = PagingMap<String>(size = 0, values = persistentMapOf(), onGet = {}),
                            loadState = LoadState.Success,
                            retry = {},
                        )
                    }
                Box(Modifier.size(400.dp)) {
                    Table(items = items, state = state, columns = columns)
                }
            }
            onNodeWithText("No data").assertIsDisplayed()
        }
}
