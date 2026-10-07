package ua.wwind.table.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.paging.core.PagingMap
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import ua.wwind.table.tableColumns
import kotlin.test.Test

private fun bodyColumns() =
    tableColumns<String, String, Unit> {
        column("name", valueOf = { it }) {
            header("Name")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text(item) }
        }
    }

private fun noRows(
    loadState: LoadState,
    retry: (Int) -> Unit = {},
) = PagingData(
    data = PagingMap<String>(size = 0, values = persistentMapOf(), onGet = {}),
    loadState = loadState,
    retry = retry,
)

/** The body while the paged table has no rows: loading, failed, or empty. */
@OptIn(ExperimentalTestApi::class)
class PagedBodyLoadStatesTest {
    @Composable
    private fun PagedTable(
        items: PagingData<String>?,
        errorContent: (@Composable PagedTableErrorScope.() -> Unit)? = null,
        strings: StringProvider = DefaultStrings,
    ) {
        val columns = remember { bodyColumns() }
        val state = rememberTableState(columns = persistentListOf("name"))
        Box(Modifier.size(400.dp)) {
            Table(
                items = items,
                state = state,
                columns = columns,
                errorContent = errorContent ?: PagedTableDefaults.ErrorContent,
                strings = strings,
            )
        }
    }

    @Test
    fun `no snapshot yet shows the loading content`() =
        runComposeUiTest {
            setContent { PagedTable(items = null) }
            onNodeWithContentDescription("Loading").assertIsDisplayed()
            onNodeWithText("No data").assertDoesNotExist()
        }

    @Test
    fun `an empty list that is still loading shows the loading content`() =
        runComposeUiTest {
            setContent { PagedTable(items = remember { noRows(LoadState.Loading) }) }
            onNodeWithContentDescription("Loading").assertIsDisplayed()
            onNodeWithText("No data").assertDoesNotExist()
        }

    @Test
    fun `a failed first load shows the error and retries its key`() =
        runComposeUiTest {
            val retried = mutableListOf<Int>()
            setContent {
                PagedTable(
                    items =
                        remember {
                            noRows(LoadState.Error(IllegalStateException("boom"), key = 40)) { retried += it }
                        },
                )
            }
            onNodeWithText("Couldn't load data").assertIsDisplayed()
            onNodeWithText("Name").assertIsDisplayed()
            onNodeWithText("Retry").performClick()
            runOnIdle { assertThat(retried).containsExactly(40) }
        }

    @Test
    fun `a custom error slot sees the failure`() =
        runComposeUiTest {
            setContent {
                PagedTable(
                    items = remember { noRows(LoadState.Error(IllegalStateException("boom"), key = 0)) },
                    errorContent = { Text("failed with ${error.message}") },
                )
            }
            onNodeWithText("failed with boom").assertIsDisplayed()
            onNodeWithText("Couldn't load data").assertDoesNotExist()
        }

    @Test
    fun `the default error content uses the table string provider`() =
        runComposeUiTest {
            val ukrainian =
                object : StringProvider {
                    @Composable
                    override fun get(key: UiString): String =
                        when (key) {
                            UiString.PagingRetry -> "Повторити"
                            else -> DefaultStrings.get(key)
                        }
                }
            setContent {
                PagedTable(
                    items = remember { noRows(LoadState.Error(IllegalStateException("boom"), key = 0)) },
                    strings = ukrainian,
                )
            }
            onNodeWithText("Повторити").assertIsDisplayed()
        }

    @Test
    fun `a settled empty list says there is no data`() =
        runComposeUiTest {
            setContent { PagedTable(items = remember { noRows(LoadState.Success) }) }
            onNodeWithText("No data").assertIsDisplayed()
            onNodeWithContentDescription("Loading").assertDoesNotExist()
        }
}
