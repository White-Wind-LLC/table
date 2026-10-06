package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.Table
import ua.wwind.table.component.ColumnMenuBuilder
import ua.wwind.table.component.ColumnMenuDefaults
import ua.wwind.table.component.ColumnMenuItem
import ua.wwind.table.component.ColumnMenuItemId
import ua.wwind.table.component.ColumnMenuSection
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isNonMobile
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ColumnHeaderMenuTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                sortable()
                cell { item, _ -> Text(item) }
            }
            column("copy", valueOf = { it }) {
                header("Copy")
                cell { item, _ -> Text("$item copy") }
            }
        }

    private fun desktopOnlyTest(block: suspend ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            if (getPlatform().isNonMobile()) block()
        }

    private fun ComposeUiTest.showTable(
        columnMenu: ColumnMenuBuilder<String> = ColumnMenuDefaults.builder(),
        settings: TableSettings = TableSettings(),
    ): () -> TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state = rememberTableState(columns = persistentListOf("name", "copy"), settings = settings)
            Box(Modifier.size(400.dp, 300.dp)) {
                Table(itemsCount = 2, itemAt = { "row-$it" }, state = state, columns = columns, columnMenu = columnMenu)
            }
        }
        waitForIdle()
        return { state }
    }

    private fun ComposeUiTest.rightClickHeader(text: String) {
        onAllNodesWithText(text).onLast().performMouseInput { rightClick() }
        waitForIdle()
    }

    private val headerWithActions = SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)

    @Test
    fun `right click opens the menu and an item runs its action`() =
        desktopOnlyTest {
            val state = showTable()
            rightClickHeader("Name")

            onNodeWithText("Sort descending").performClick()
            waitForIdle()

            assertThat(state().sort).isEqualTo(SortState("name", SortOrder.DESCENDING))
            onNodeWithText("Sort descending").assertDoesNotExist()
        }

    @Test
    fun `a disabled item shows its reason`() =
        desktopOnlyTest {
            showTable()
            rightClickHeader("Name")

            onNodeWithText("Move left", substring = true).assertIsNotEnabled()
            onNodeWithText("Already first", substring = true).assertExists()
        }

    @Test
    fun `header custom actions match the enabled menu items`() =
        desktopOnlyTest {
            showTable()
            val labels =
                onNode(headerWithActions and hasAnyDescendant(hasText("Name")))
                    .fetchSemanticsNode()
                    .config[SemanticsActions.CustomActions]
                    .map { it.label }

            assertThat(labels).contains("Sort ascending")
            assertThat(labels).contains("Move right")
            assertThat(labels).doesNotContain("Move left")
        }

    @Test
    fun `a custom builder can remove and add items`() =
        desktopOnlyTest {
            var clicked = false
            showTable(
                columnMenu =
                    ColumnMenuBuilder { _, defaults ->
                        defaults.filter { it.id == ColumnMenuDefaults.Sections.Sort } +
                            ColumnMenuSection(
                                id = "custom",
                                items =
                                    listOf(
                                        ColumnMenuItem(ColumnMenuItemId("x"), "Do X", icon = null) {
                                            clicked =
                                                true
                                        },
                                    ),
                            )
                    },
            )
            rightClickHeader("Name")

            onNodeWithText("Hide column").assertDoesNotExist()
            onNodeWithText("Do X").performClick()
            waitForIdle()
            assertThat(clicked).isTrue()
        }

    @Test
    fun `an empty builder removes the menu and its actions`() =
        desktopOnlyTest {
            showTable(columnMenu = ColumnMenuBuilder { _, _ -> emptyList() })
            rightClickHeader("Name")

            onNodeWithText("Sort ascending").assertDoesNotExist()
            onAllNodes(headerWithActions).assertCountEquals(0)
        }

    @Test
    fun `a group header right click opens exactly one menu`() =
        desktopOnlyTest {
            val groupColumns =
                tableColumns<String, String, Unit> {
                    column("kind", valueOf = { it.substringBefore('-') }) {
                        header("Kind")
                        cell { item, _ -> Text(item) }
                        groupHeader { Text("Group $it") }
                    }
                }
            lateinit var state: TableState<String>
            setContent {
                state = rememberTableState(columns = persistentListOf("kind"))
                Box(Modifier.size(400.dp, 300.dp)) {
                    Table(
                        itemsCount = 4,
                        itemAt = { listOf("a-1", "a-2", "b-1", "b-2")[it] },
                        state = state,
                        columns = groupColumns,
                    )
                }
            }
            waitForIdle()
            state.groupBy("kind")
            waitForIdle()

            onAllNodesWithText("Group a").onFirst().performMouseInput { rightClick() }
            waitForIdle()

            onAllNodesWithText("Ungroup").assertCountEquals(1)
        }

    @Test
    fun `a custom builder shapes the column header menu but not the group header menu`() =
        desktopOnlyTest {
            val groupColumns =
                tableColumns<String, String, Unit> {
                    column("kind", valueOf = { it.substringBefore('-') }) {
                        header("Kind")
                        cell { item, _ -> Text(item) }
                        groupHeader { Text("Group $it") }
                    }
                }
            val addItem =
                ColumnMenuBuilder<String> { _, defaults ->
                    val custom = ColumnMenuItem(ColumnMenuItemId("x"), "Do X", icon = null) {}
                    defaults + ColumnMenuSection(id = "custom", items = listOf(custom))
                }
            lateinit var state: TableState<String>
            setContent {
                state = rememberTableState(columns = persistentListOf("kind"))
                Box(Modifier.size(400.dp, 300.dp)) {
                    Table(
                        itemsCount = 4,
                        itemAt = { listOf("a-1", "a-2", "b-1", "b-2")[it] },
                        state = state,
                        columns = groupColumns,
                        columnMenu = addItem,
                    )
                }
            }
            waitForIdle()
            state.groupBy("kind")
            waitForIdle()

            rightClickHeader("Kind")
            onNodeWithText("Do X").assertExists()
            state.closeColumnMenu()
            waitForIdle()

            onAllNodesWithText("Group a").onFirst().performMouseInput { rightClick() }
            waitForIdle()
            onAllNodesWithText("Ungroup").assertCountEquals(1)
            onNodeWithText("Do X").assertDoesNotExist()
        }

    @Test
    fun `Filter opens the panel on a column without header decorations`() =
        desktopOnlyTest {
            val plainColumns =
                tableColumns<String, String, Unit> {
                    column("name", valueOf = { it }) {
                        header("Name")
                        headerDecorations(false)
                        filter(TableFilterType.TextTableFilter())
                        cell { item, _ -> Text(item) }
                    }
                }
            setContent {
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp, 300.dp)) {
                    Table(itemsCount = 2, itemAt = { "row-$it" }, state = state, columns = plainColumns)
                }
            }
            waitForIdle()

            rightClickHeader("Name")
            onNodeWithText("Filter…").performClick()
            waitForIdle()

            onNodeWithText("Search...").assertExists()
        }

    @Test
    fun `the menu button is off by default`() =
        desktopOnlyTest {
            showTable()
            onNodeWithContentDescription("Column options: Name").assertDoesNotExist()
        }

    @Test
    fun `the menu button opens the menu`() =
        desktopOnlyTest {
            showTable(settings = TableSettings(showColumnMenuButton = true))
            onNodeWithContentDescription("Column options: Name").performClick()
            waitForIdle()
            onNodeWithText("Sort ascending").assertExists()
        }

    @Test
    fun `no menu means no menu button`() =
        desktopOnlyTest {
            showTable(
                settings = TableSettings(showColumnMenuButton = true),
                columnMenu = ColumnMenuBuilder { _, _ -> emptyList() },
            )
            onNodeWithContentDescription("Column options: Name").assertDoesNotExist()
        }
}
