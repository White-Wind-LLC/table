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
import androidx.compose.ui.test.onLast
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
import ua.wwind.table.data.SortOrder
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
    ): () -> TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state = rememberTableState(columns = persistentListOf("name", "copy"))
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
}
