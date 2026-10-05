package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ColumnHideTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header("A")
                width(100.dp, 100.dp)
                resizable(false)
                cell { item, _ -> Text(item) }
            }
            column("b", valueOf = { it }) {
                header("B")
                width(100.dp, 100.dp)
                resizable(false)
                cell { _, _ -> Text("cell-b") }
            }
            column("c", valueOf = { it }) {
                header("C")
                width(100.dp, 100.dp)
                resizable(false)
                visible(false)
                cell { _, _ -> Text("cell-c") }
            }
        }

    @Test
    fun `a hidden column leaves the header and the table width`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                state = rememberTableState(columns = persistentListOf("a", "b", "c"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = 1, itemAt = { "row" }, state = state, columns = columns)
                }
            }
            waitForIdle()
            runOnIdle { state.columns.hide("b") }
            waitForIdle()

            onNodeWithText("B").assertDoesNotExist()
            assertThat(state.tableWidth).isEqualTo(100.dp + state.dimensions.dividerThickness)
        }

    @Test
    fun `showAll does not reveal a column the spec hides`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                state = rememberTableState(columns = persistentListOf("a", "b", "c"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = 1, itemAt = { "row" }, state = state, columns = columns)
                }
            }
            runOnIdle {
                state.columns.hide("b")
                state.columns.showAll()
            }
            waitForIdle()

            onNodeWithText("B").assertExists()
            onNodeWithText("C").assertDoesNotExist()
        }
}
