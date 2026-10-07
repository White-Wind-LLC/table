package ua.wwind.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isBetween
import assertk.assertions.isGreaterThan
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/** `bodyOverlay`: drawn over the visible rows area, fixed under scrolling, transparent to input. */
@OptIn(ExperimentalTestApi::class)
class TableBodyOverlayTest {
    private fun columns(width: Dp = 120.dp) =
        tableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                width(width, width)
                resizable(false)
                cell { item, _ -> Text(item) }
                footer { Text("total") }
            }
        }

    @Test
    fun `the overlay spans the body between the header and a pinned footer`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { columns() }
                state =
                    rememberTableState(
                        columns = persistentListOf("name"),
                        settings = TableSettings(showFooter = true, footerPinned = true),
                    )
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 3,
                        itemAt = { "row $it" },
                        state = state,
                        columns = columns,
                        modifier = Modifier.fillMaxSize(),
                        bodyOverlay = {
                            Text("top", Modifier.align(Alignment.TopCenter))
                            Text("bottom", Modifier.align(Alignment.BottomCenter))
                        },
                    )
                }
            }
            val top = onNodeWithText("top").getBoundsInRoot()
            val bottom = onNodeWithText("bottom").getBoundsInRoot()
            val dimensions = state.dimensions
            assertThat(top.top).isBetween(dimensions.headerHeight - 4.dp, dimensions.headerHeight + 4.dp)
            val footerTop = 400.dp - dimensions.footerHeight - dimensions.dividerThickness
            assertThat(bottom.bottom).isBetween(footerTop - 4.dp, footerTop + 4.dp)
        }

    @Test
    fun `the overlay stays in view when the rows scroll horizontally`() =
        runComposeUiTest {
            val horizontal = ScrollState(0)
            setContent {
                val columns = remember { columns(width = 900.dp) }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 3,
                        itemAt = { "row $it" },
                        state = state,
                        columns = columns,
                        modifier = Modifier.fillMaxSize(),
                        horizontalState = horizontal,
                        bodyOverlay = { Text("centre", Modifier.align(Alignment.Center)) },
                    )
                }
            }
            runOnIdle { horizontal.dispatchRawDelta(300f) }
            waitForIdle()
            assertThat(horizontal.value).isGreaterThan(0)
            val bounds = onNodeWithText("centre").getBoundsInRoot()
            assertThat((bounds.left + bounds.right) / 2).isBetween(196.dp, 204.dp)
        }

    @Test
    fun `rows under the overlay stay clickable`() =
        runComposeUiTest {
            val clicked = mutableListOf<String>()
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                        modifier = Modifier.fillMaxSize(),
                        onRowClick = { clicked += it },
                        bodyOverlay = { Text("bar", Modifier.align(Alignment.BottomCenter)) },
                    )
                }
            }
            onNodeWithText("row").performClick()
            runOnIdle { assertThat(clicked).containsExactly("row") }
        }

    @Test
    fun `the overlay is drawn alongside the empty state`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 0,
                        itemAt = { null },
                        state = state,
                        columns = columns,
                        bodyOverlay = { Text("bar", Modifier.align(Alignment.TopCenter)) },
                    )
                }
            }
            onNodeWithText("bar").assertIsDisplayed()
            onNodeWithText("No data").assertIsDisplayed()
        }
}
