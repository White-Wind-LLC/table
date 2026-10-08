package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private data class Planet(
    val name: String,
    val kind: String,
)

private val planets =
    listOf(
        Planet("Mercury", "rocky"),
        Planet("Venus", "rocky"),
        Planet("Jupiter", "giant"),
    )

private fun planetColumns() =
    tableColumns<Planet, String, Unit> {
        column("name", valueOf = { it.name }) {
            header("Name")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text(item.name) }
        }
        column("kind", valueOf = { it.kind }) {
            header("Kind")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text("kind: ${item.kind}") }
            groupHeader { value -> Text("group: $value") }
        }
    }

private fun hasCollectionInfo(
    rowCount: Int,
    columnCount: Int,
) = SemanticsMatcher("CollectionInfo($rowCount, $columnCount)") { node ->
    node.config.getOrNull(SemanticsProperties.CollectionInfo).let {
        it != null && it.rowCount == rowCount && it.columnCount == columnCount
    }
}

// CollectionItemInfo has no equals, so compare its fields.
private fun hasCollectionItemInfo(
    rowIndex: Int,
    columnIndex: Int,
) = SemanticsMatcher("CollectionItemInfo(row $rowIndex, column $columnIndex)") { node ->
    node.config.getOrNull(SemanticsProperties.CollectionItemInfo).let {
        it != null && it.rowIndex == rowIndex && it.columnIndex == columnIndex && it.rowSpan == 1 &&
            it.columnSpan == 1
    }
}

/** What screen readers learn about the table's structure: grid size, cell positions, headings, selection (#77). */
@OptIn(ExperimentalTestApi::class)
class TableSemanticsTest {
    @Test
    fun `the body reports its rows and visible columns`() =
        runComposeUiTest {
            setContent {
                val columns = remember { planetColumns() }
                val state = rememberTableState(columns = persistentListOf("name", "kind"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = planets.size, itemAt = { planets[it] }, state = state, columns = columns)
                }
            }
            assertThat(onAllNodes(hasCollectionInfo(rowCount = 3, columnCount = 2)).fetchSemanticsNodes().size)
                .isEqualTo(1)
        }

    @Test
    fun `an embedded body reports its rows and visible columns`() =
        runComposeUiTest {
            setContent {
                val columns = remember { planetColumns() }
                val state = rememberTableState(columns = persistentListOf("name", "kind"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = planets.size,
                        itemAt = { planets[it] },
                        state = state,
                        columns = columns,
                        embedded = true,
                    )
                }
            }
            assertThat(onAllNodes(hasCollectionInfo(rowCount = 3, columnCount = 2)).fetchSemanticsNodes().size)
                .isEqualTo(1)
        }

    @Test
    fun `each cell reports its row and column`() =
        runComposeUiTest {
            setContent {
                val columns = remember { planetColumns() }
                val state = rememberTableState(columns = persistentListOf("name", "kind"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = planets.size, itemAt = { planets[it] }, state = state, columns = columns)
                }
            }
            onNodeWithText("Mercury").assert(hasCollectionItemInfo(rowIndex = 0, columnIndex = 0))
            onNodeWithText("kind: giant").assert(hasCollectionItemInfo(rowIndex = 2, columnIndex = 1))
        }

    @Test
    fun `the selected cell is announced as selected`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { planetColumns() }
                state =
                    rememberTableState(
                        columns = persistentListOf("name", "kind"),
                        settings = TableSettings(selectionMode = SelectionMode.Single),
                    )
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = planets.size, itemAt = { planets[it] }, state = state, columns = columns)
                }
            }
            onNodeWithText("Venus").assert(!isSelected())

            runOnIdle { state.selection.selectCell(1, "name") }

            onNodeWithText("Venus").assert(isSelected())
            onNodeWithText("Mercury").assert(!isSelected())
        }

    @Test
    fun `the selected row is announced as selected`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { planetColumns() }
                state =
                    rememberTableState(
                        columns = persistentListOf("name", "kind"),
                        settings = TableSettings(selectionMode = SelectionMode.Single),
                    )
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = planets.size, itemAt = { planets[it] }, state = state, columns = columns)
                }
            }
            runOnIdle { state.selection.toggleRow(2) }

            val selectedRows =
                onAllNodes(isSelected()).fetchSemanticsNodes().filter { row ->
                    row.config.getOrNull(SemanticsProperties.CollectionItemInfo) == null
                }
            assertThat(selectedRows.size).isEqualTo(1)
        }

    @Test
    fun `column headers are headings`() =
        runComposeUiTest {
            setContent {
                val columns = remember { planetColumns() }
                val state = rememberTableState(columns = persistentListOf("name", "kind"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = planets.size, itemAt = { planets[it] }, state = state, columns = columns)
                }
            }
            assertThat(onAllNodes(isHeading() and hasText("Name")).fetchSemanticsNodes().size).isEqualTo(1)
            assertThat(onAllNodes(isHeading() and hasText("Kind")).fetchSemanticsNodes().size).isEqualTo(1)
        }

    @Test
    fun `each group header is one heading without the sticky copy`() =
        runComposeUiTest {
            setContent {
                val columns = remember { planetColumns() }
                val state = rememberTableState(columns = persistentListOf("name", "kind"))
                remember { state.groupBy("kind") }
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = planets.size, itemAt = { planets[it] }, state = state, columns = columns)
                }
            }
            waitForIdle()
            assertThat(onAllNodes(isHeading() and hasText("group: rocky")).fetchSemanticsNodes().size).isEqualTo(1)
            assertThat(onAllNodes(isHeading() and hasText("group: giant")).fetchSemanticsNodes().size).isEqualTo(1)
        }

    @Test
    fun `rows measured offscreen are not read twice`() =
        runComposeUiTest {
            val rows = List(40) { Planet("planet-$it", "rocky") }
            setContent {
                val columns = remember { planetColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("name", "kind"),
                        settings = TableSettings(rowHeightMode = RowHeightMode.Dynamic),
                    )
                Box(Modifier.size(400.dp, 200.dp)) {
                    Table(itemsCount = rows.size, itemAt = { rows[it] }, state = state, columns = columns)
                }
            }
            waitForIdle()
            rows.forEach { row ->
                assertThat(onAllNodesWithText(row.name).fetchSemanticsNodes().size).isLessThanOrEqualTo(1)
            }
        }
}
