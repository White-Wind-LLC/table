package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
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
import assertk.assertions.isCloseTo
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
import ua.wwind.table.state.ColumnWidthAction
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
    fun `a custom item draws its painter icon`() =
        desktopOnlyTest {
            showTable(
                columnMenu =
                    ColumnMenuBuilder { _, _ ->
                        listOf(
                            ColumnMenuSection(
                                id = "custom",
                                items =
                                    listOf(
                                        ColumnMenuItem(
                                            ColumnMenuItemId("y"),
                                            "Do Y",
                                            icon = ColorPainter(Color.Red),
                                        ) {},
                                        ColumnMenuItem(ColumnMenuItemId("z"), "Do Z", icon = null) {},
                                    ),
                            ),
                        )
                    },
            )
            rightClickHeader("Name")

            onNodeWithText("Do Y").assertExists()
            // Only the item with a painter gets a leading icon.
            onAllNodes(hasTestTag(LEADING_ICON_TAG), useUnmergedTree = true).assertCountEquals(1)
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

            onNodeWithText("Condition").assertExists()
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

    @Test
    fun `a disabled item exposes its reason as state description`() =
        desktopOnlyTest {
            showTable()
            rightClickHeader("Name")

            onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Already first"),
            ).assertExists()
        }

    @Test
    fun `only the active sort direction shows the trailing check`() =
        desktopOnlyTest {
            val state = showTable()
            val checked = hasTestTag(CHECKED_ITEM_TAG)

            rightClickHeader("Name")
            onAllNodes(checked, useUnmergedTree = true).assertCountEquals(0)
            state().closeColumnMenu()
            waitForIdle()

            state().setSort("name", SortOrder.ASCENDING)
            waitForIdle()
            rightClickHeader("Name")
            onAllNodes(checked, useUnmergedTree = true).assertCountEquals(1)
            onNodeWithText("Sort ascending").assertExists()
            onNodeWithText("Clear sort").assertExists()
        }

    @Test
    fun `with row reorder sort and group are disabled with the reason while the rest stays enabled`() =
        desktopOnlyTest {
            val filtered =
                tableColumns<String, String, Unit> {
                    column("name", valueOf = { it }) {
                        header("Name")
                        sortable()
                        filter(TableFilterType.TextTableFilter())
                        cell { item, _ -> Text(item) }
                    }
                    column("copy", valueOf = { it }) {
                        header("Copy")
                        cell { item, _ -> Text("$item copy") }
                    }
                }
            setContent {
                val state =
                    rememberTableState(
                        columns = persistentListOf("name", "copy"),
                        settings = TableSettings(rowReorderEnabled = true),
                    )
                Box(Modifier.size(400.dp, 300.dp)) {
                    Table(itemsCount = 2, itemAt = { "row-$it" }, state = state, columns = filtered)
                }
            }
            waitForIdle()
            rightClickHeader("Name")

            val reason = "Unavailable while rows can be reordered"
            onNodeWithText("Sort ascending", substring = true).assertIsNotEnabled()
            onNodeWithText("Group by", substring = true).assertIsNotEnabled()
            onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, reason))
                .assertCountEquals(3)
            onNodeWithText("Filter…").assertIsEnabled()
            onNodeWithText("Move right", substring = true).assertIsEnabled()
            onNodeWithText("Hide column", substring = true).assertIsEnabled()
        }

    @Test
    fun `a width drag does not rebuild the menu for every width`() =
        desktopOnlyTest {
            var builds = 0
            val state =
                showTable(
                    columnMenu =
                        ColumnMenuBuilder { _, defaults ->
                            builds++
                            defaults
                        },
                )
            state().columns.resize("name", ColumnWidthAction.Set(150.dp))
            waitForIdle()
            val afterOverride = builds
            (1..5).forEach { step ->
                state().columns.resize("name", ColumnWidthAction.Set((150 + step * 10).dp))
                waitForIdle()
            }
            assertThat(builds).isEqualTo(afterOverride)
        }

    @Test
    fun `auto-fit width grows by the menu button once the menu appears`() =
        desktopOnlyTest {
            var menuOn by mutableStateOf(false)
            val wide =
                tableColumns<String, String, Unit> {
                    column("name", valueOf = { it }) {
                        header("Name")
                        width(8.dp)
                        cell { item, _ -> Text(item.take(1)) }
                    }
                }
            lateinit var state: TableState<String>
            setContent {
                state =
                    rememberTableState(
                        columns = persistentListOf("name"),
                        settings = TableSettings(showColumnMenuButton = true),
                    )
                val builder =
                    ColumnMenuBuilder<String> { _, defaults -> if (menuOn) defaults else emptyList() }
                Box(Modifier.size(400.dp, 300.dp)) {
                    Table(itemsCount = 1, itemAt = { "r" }, state = state, columns = wide, columnMenu = builder)
                }
            }
            waitUntil { state.columns.headerWidths["name"] != null }
            val without = state.columns.headerWidths["name"]!!

            menuOn = true
            waitUntil { state.columns.headerWidths["name"]!! > without }
            val grown = state.columns.headerWidths["name"]!! - without
            assertThat(grown.value).isCloseTo(state.dimensions.headerIconTargetSize.value, 1f)
        }
}
