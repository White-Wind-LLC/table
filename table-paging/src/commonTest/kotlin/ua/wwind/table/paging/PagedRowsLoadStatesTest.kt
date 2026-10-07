package ua.wwind.table.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isLessThanOrEqualTo
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.paging.core.PagingMap
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

private fun rowsColumns() =
    tableColumns<String, String, Unit> {
        column("name", valueOf = { it }) {
            header("Name")
            width(360.dp, 360.dp)
            resizable(false)
            cell { item, _ -> Text(item) }
        }
    }

private val loadedValues = persistentMapOf(0 to "a", 1 to "b", 2 to "c")

private fun rows(
    loadState: LoadState,
    loaded: Boolean = true,
    retry: (Int) -> Unit = {},
) = PagingData(
    data = PagingMap(size = 3, values = if (loaded) loadedValues else persistentMapOf(), onGet = {}),
    loadState = loadState,
    retry = retry,
)

private val failure = LoadState.Error(IllegalStateException("boom"), key = 20)

/** Load states while rows are on screen: a delayed progress indicator and an error bar. */
@OptIn(ExperimentalTestApi::class)
class PagedRowsLoadStatesTest {
    @Composable
    private fun PagedTable(
        items: PagingData<String>?,
        loadingIndicator: (@Composable () -> Unit)? = PagedTableDefaults.LoadingIndicator,
        errorBar: (@Composable PagedTableErrorScope.() -> Unit)? = PagedTableDefaults.ErrorBar,
    ) {
        val columns = remember { rowsColumns() }
        val state = rememberTableState(columns = persistentListOf("name"))
        Box(Modifier.size(400.dp)) {
            Table(
                items = items,
                state = state,
                columns = columns,
                loadingIndicator = loadingIndicator,
                errorBar = errorBar,
            )
        }
    }

    @Test
    fun `the indicator appears only after loading has lasted the delay`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var items by mutableStateOf(rows(LoadState.Loading))
            setContent { PagedTable(items = items, loadingIndicator = { Text("indicator") }) }
            mainClock.advanceTimeBy(200)
            onNodeWithText("indicator").assertDoesNotExist()
            mainClock.advanceTimeBy(300)
            onNodeWithText("indicator").assertIsDisplayed()

            items = rows(LoadState.Success)
            mainClock.advanceTimeByFrame()
            onNodeWithText("indicator").assertDoesNotExist()
        }

    @Test
    fun `a short loading pulse never shows the indicator`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var items by mutableStateOf(rows(LoadState.Loading))
            setContent { PagedTable(items = items, loadingIndicator = { Text("indicator") }) }
            mainClock.advanceTimeBy(200)
            items = rows(LoadState.Success)
            mainClock.advanceTimeBy(500)
            onNodeWithText("indicator").assertDoesNotExist()
        }

    @Test
    fun `a refresh over placeholders shows the indicator and no empty state`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { PagedTable(items = remember { rows(LoadState.Loading, loaded = false) }) }
            mainClock.advanceTimeBy(500)
            onNodeWithContentDescription("Loading").assertIsDisplayed()
            onNodeWithText("No data").assertDoesNotExist()
        }

    @Test
    fun `a failed load over rows shows the error bar and retries its key`() =
        runComposeUiTest {
            val retried = mutableListOf<Int>()
            setContent { PagedTable(items = remember { rows(failure) { retried += it } }) }
            onNodeWithText("a").assertIsDisplayed()
            onNodeWithText("Couldn't load some rows").assertIsDisplayed()
            onNodeWithText("Retry").performClick()
            runOnIdle { assertThat(retried).containsExactly(20) }
        }

    @Test
    fun `the error bar goes away as soon as loading resumes`() =
        runComposeUiTest {
            var items by mutableStateOf(rows(failure))
            setContent { PagedTable(items = items) }
            onNodeWithText("Couldn't load some rows").assertIsDisplayed()
            runOnIdle { items = rows(LoadState.Loading) }
            onNodeWithText("Couldn't load some rows").assertDoesNotExist()
        }

    @Test
    fun `null slots turn the indicator and the error bar off`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var items by mutableStateOf(rows(LoadState.Loading))
            setContent { PagedTable(items = items, loadingIndicator = null, errorBar = null) }
            mainClock.advanceTimeBy(500)
            onNodeWithContentDescription("Loading").assertDoesNotExist()
            items = rows(failure)
            mainClock.advanceTimeByFrame()
            onNodeWithText("Couldn't load some rows").assertDoesNotExist()
            onNodeWithText("a").assertIsDisplayed()
        }

    @Test
    fun `the error bar message stays within two lines in a narrow table`() =
        runComposeUiTest {
            setContent {
                val columns =
                    remember {
                        tableColumns<String, String, Unit> {
                            column("name", valueOf = { it }) {
                                header("Name")
                                width(150.dp, 150.dp)
                                resizable(false)
                                cell { item, _ -> Text(item) }
                            }
                        }
                    }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(width = 160.dp, height = 400.dp)) {
                    Table(items = remember { rows(failure) }, state = state, columns = columns)
                }
            }
            val message = onNodeWithText("Couldn't load some rows", useUnmergedTree = true).getBoundsInRoot()
            // Two bodyMedium lines are about 40 dp; per-glyph wrapping in the leftover width is far taller.
            assertThat(message.bottom - message.top).isLessThanOrEqualTo(48.dp)
        }
}
