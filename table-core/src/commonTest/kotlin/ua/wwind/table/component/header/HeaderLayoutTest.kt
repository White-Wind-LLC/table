package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.Table
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class HeaderLayoutTest {
    private fun ComposeUiTest.showColumn(
        title: String,
        width: Dp = 300.dp,
        configure: ReadonlyColumnBuilder<String, String, Unit>.() -> Unit = {},
    ) {
        val columns =
            tableColumns<String, String, Unit> {
                column("a", valueOf = { it }) {
                    header(title)
                    width(width, width)
                    resizable(false)
                    cell { item, _ -> Text(item) }
                    configure()
                }
            }
        setContent {
            Box(Modifier.size(600.dp, 300.dp)) {
                Table(
                    itemsCount = 1,
                    itemAt = { "row" },
                    state =
                        rememberTableState(
                            columns = persistentListOf("a"),
                            settings = TableSettings(showColumnMenuButton = true),
                        ),
                    columns = columns,
                )
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.titleBounds(title: String) =
        onAllNodesWithText(title, useUnmergedTree = true).onFirst().getBoundsInRoot()

    private fun ComposeUiTest.menuButtonLeft() =
        onNodeWithContentDescription("Column options", substring = true, useUnmergedTree = true)
            .getBoundsInRoot()
            .left

    @Test
    fun `a long title leaves room for the sort icon in a narrow column`() =
        runComposeUiTest {
            val title = "A very long column title that cannot fit"
            showColumn(title, width = 140.dp) { sortable() }

            val gap = menuButtonLeft() - titleBounds(title).right
            assertThat(gap).isGreaterThanOrEqualTo(HeaderIconSize)
        }

    @Test
    fun `the header follows a start-aligned column`() =
        runComposeUiTest {
            showColumn("A")

            assertThat(titleBounds("A").left).isLessThan(300.dp / 4)
        }

    @Test
    fun `the header follows an end-aligned column`() =
        runComposeUiTest {
            showColumn("A") { align(Alignment.CenterEnd) }

            assertThat(titleBounds("A").left).isGreaterThan(300.dp / 2)
        }

    @Test
    fun `headerAlign overrides the column alignment`() =
        runComposeUiTest {
            showColumn("A") {
                align(Alignment.CenterEnd)
                headerAlign(Alignment.CenterStart)
            }

            assertThat(titleBounds("A").left).isLessThan(300.dp / 4)
        }

    @Test
    fun `an end-aligned header puts the sort icon before the title`() =
        runComposeUiTest {
            showColumn("A") {
                sortable()
                align(Alignment.CenterEnd)
            }

            // The title ends next to the menu button; the icon sits on its other side.
            val gap = menuButtonLeft() - titleBounds("A").right
            assertThat(gap).isLessThan(HeaderIconSize)
        }

    @Test
    fun `numeric aligns cells to the end`() =
        runComposeUiTest {
            showColumn("A") { numeric() }

            val textLeft = onNodeWithText("row", useUnmergedTree = true).getBoundsInRoot().left
            assertThat(textLeft).isGreaterThan(300.dp / 2)
        }

    @Test
    fun `numeric gives cells tabular figures`() =
        runComposeUiTest {
            var seen: TextStyle? = null
            val columns =
                tableColumns<String, String, Unit> {
                    column("a", valueOf = { it }) {
                        header("A")
                        numeric()
                        cell { item, _ ->
                            val style = LocalTextStyle.current
                            SideEffect { seen = style }
                            Text(item)
                        }
                    }
                }
            setContent {
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = rememberTableState(columns = persistentListOf("a")),
                        columns = columns,
                    )
                }
            }

            waitForIdle()
            assertThat(seen?.fontFeatureSettings).isEqualTo("tnum")
        }

    @Test
    fun `an active sort icon is tinted primary`() {
        val primary = Color.Blue
        assertThat(sortIconTint(SortOrder.ASCENDING, contentColor = Color.Black, primary = primary))
            .isEqualTo(primary)
        assertThat(sortIconTint(SortOrder.DESCENDING, contentColor = Color.Black, primary = primary))
            .isEqualTo(primary)
    }

    @Test
    fun `the neutral sort icon is dimmed to 38 percent`() {
        val content = Color.Black
        assertThat(sortIconTint(order = null, contentColor = content, primary = Color.Blue))
            .isEqualTo(content.copy(alpha = 0.38f))
    }
}
