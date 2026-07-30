package ua.wwind.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isGreaterThan
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ActiveFiltersHeaderTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header("A")
                width(500.dp, 500.dp)
                resizable(false)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
            }
            column("b", valueOf = { it }) {
                header("B")
                width(500.dp, 500.dp)
                resizable(false)
                cell { _, _ -> Text("cell-b") }
            }
        }

    @Test
    fun `an active filter chip stays on screen after the table is scrolled right`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            lateinit var horizontalState: ScrollState

            setContent {
                state =
                    rememberTableState(
                        columns = persistentListOf("a", "b"),
                        settings = TableSettings(showActiveFiltersHeader = true),
                    )
                horizontalState = rememberScrollState()
                Box(Modifier.size(600.dp, 400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                        horizontalState = horizontalState,
                    )
                }
            }

            waitForIdle()
            state.setFilter("a", TableFilterState(FilterConstraint.CONTAINS, listOf("x")))
            waitForIdle()
            onNodeWithText("A:", substring = true).assertIsDisplayed()

            horizontalState.dispatchRawDelta(10_000f)
            waitForIdle()

            assertThat(horizontalState.value).isGreaterThan(0)
            onNodeWithText("A:", substring = true).assertIsDisplayed()
        }

    @Test
    fun `no chips row is rendered while no filter is active`() =
        runComposeUiTest {
            setContent {
                val state =
                    rememberTableState(
                        columns = persistentListOf("a", "b"),
                        settings = TableSettings(showActiveFiltersHeader = true),
                    )
                Box(Modifier.size(600.dp, 400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                    )
                }
            }

            waitForIdle()
            onNodeWithText("Clear").assertDoesNotExist()
        }
}
