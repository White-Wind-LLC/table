package ua.wwind.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.PinnedEdge
import ua.wwind.table.state.hasContentUnder
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/** The colors and pinned-edge shadow of #87 reach the drawn table. */
@OptIn(ExperimentalTestApi::class)
class TableVisualsTest {
    private val keys = (0 until 6).map { "c$it" }

    private val columns =
        tableColumns<String, String, Unit> {
            keys.forEach { key ->
                column(key, valueOf = { it }) {
                    header(key)
                    width(100.dp, 100.dp)
                    resizable(false)
                    cell { _, _ -> }
                }
            }
        }

    private fun PixelMap.contains(color: Color): Boolean =
        (0 until width).any { x -> (0 until height).any { y -> this[x, y] == color } }

    @Test
    fun `a left-pinned edge has content under it only once scrolled`() {
        assertThat(PinnedEdge.End.hasContentUnder(ScrollState(0))).isFalse()
        assertThat(PinnedEdge.End.hasContentUnder(ScrollState(10))).isTrue()
        assertThat(PinnedEdge.None.hasContentUnder(ScrollState(10))).isFalse()
    }

    @Test
    fun `custom divider and border colors are drawn`() =
        runComposeUiTest {
            var colors: TableColors? = null
            setContent {
                colors = TableDefaults.colors(dividerColor = Color.Red, borderColor = Color.Blue)
                Box(Modifier.size(300.dp, 200.dp)) {
                    Table(
                        itemsCount = 2,
                        itemAt = { "row" },
                        state = rememberTableState(columns = keys.toImmutableList()),
                        columns = columns,
                        colors = colors!!,
                    )
                }
            }

            waitForIdle()
            val pixels = onRoot().captureToImage().toPixelMap()
            assertThat(pixels.contains(Color.Red)).isTrue()
            assertThat(pixels.contains(Color.Blue)).isTrue()
        }

    @Test
    fun `the pinned column casts a shadow only while content scrolls under it`() =
        runComposeUiTest {
            lateinit var horizontalState: ScrollState
            lateinit var dimensions: TableDimensions
            setContent {
                horizontalState = rememberScrollState()
                val state =
                    rememberTableState(
                        columns = keys.toImmutableList(),
                        settings =
                            TableSettings(
                                showVerticalDividers = false,
                                pinnedColumnsCount = 1,
                                pinnedColumnsSide = PinnedSide.Start,
                            ),
                    )
                dimensions = state.dimensions
                Box(Modifier.size(300.dp, 200.dp)) {
                    Table(
                        itemsCount = 2,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                        horizontalState = horizontalState,
                        border = TableDefaults.NoBorder,
                    )
                }
            }

            waitForIdle()
            val x = with(density) { (100.dp + dimensions.pinnedColumnDividerThickness + 1.dp).roundToPx() }
            val y = with(density) { (dimensions.headerHeight + dimensions.rowHeight / 2).roundToPx() }
            val atRest = onRoot().captureToImage().toPixelMap()[x, y]

            horizontalState.dispatchRawDelta(with(density) { 50.dp.toPx() })
            waitForIdle()
            val scrolled = onRoot().captureToImage().toPixelMap()[x, y]

            assertThat(scrolled.red).isLessThan(atRest.red)
            horizontalState.dispatchRawDelta(-with(density) { 50.dp.toPx() })
            waitForIdle()
            assertThat(onRoot().captureToImage().toPixelMap()[x, y]).isEqualTo(atRest)
        }
}
