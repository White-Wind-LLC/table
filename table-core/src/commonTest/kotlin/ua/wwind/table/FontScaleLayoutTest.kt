package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/** At 2× font scale the fixed bands grow by one extra line of their text (#135). */
@OptIn(ExperimentalTestApi::class)
class FontScaleLayoutTest {
    private data class Item(
        val id: Int,
        val group: String,
    )

    private val columns =
        tableColumns<Item, String, Unit> {
            column("group", valueOf = { it.group }) {
                header("Group")
                width(160.dp, 160.dp)
                resizable(false)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item.group) }
                footer { Text("total") }
            }
            column("id", valueOf = { it.id }) {
                header("Id")
                width(160.dp, 160.dp)
                resizable(false)
                cell { item, _ -> Text("row-${item.id}") }
            }
        }

    @Composable
    private fun FontScale(
        scale: Float,
        content: @Composable () -> Unit,
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, scale), content = content)
    }

    private fun assertNear(
        actual: Dp,
        expected: Dp,
    ) = assertThat(actual).isBetween(expected - 4.dp, expected + 4.dp)

    @Test
    fun `the overlay spans the grown header and pinned footer`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                state =
                    rememberTableState(
                        columns = persistentListOf("group", "id"),
                        settings = TableSettings(showFooter = true, footerPinned = true),
                    )
                FontScale(2f) {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        Table(
                            itemsCount = 3,
                            itemAt = { Item(it, "a") },
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
            }
            waitForIdle()
            assertThat(state.dimensions.headerHeight).isEqualTo(56.dp)
            assertThat(state.effectiveDimensions.headerHeight).isEqualTo(76.dp)
            assertThat(state.effectiveDimensions.footerHeight).isEqualTo(72.dp)
            assertNear(onNodeWithText("top").getBoundsInRoot().top, 76.dp)
            val footerTop = 500.dp - 72.dp - state.dimensions.dividerThickness
            assertNear(onNodeWithText("bottom").getBoundsInRoot().bottom, footerTop)
        }

    @Test
    fun `fixed rows grow with the font`() =
        runComposeUiTest {
            setContent {
                val state = rememberTableState(columns = persistentListOf("group", "id"))
                FontScale(2f) {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        Table(itemsCount = 3, itemAt = { Item(it, "a") }, state = state, columns = columns)
                    }
                }
            }
            waitForIdle()
            val first = onNodeWithText("row-0").getBoundsInRoot()
            val second = onNodeWithText("row-1").getBoundsInRoot()
            val pitch = second.top - first.top
            assertThat(pitch).isGreaterThanOrEqualTo(72.dp)
            assertNear(pitch, 72.dp + TableDefaults.standardDimensions().dividerThickness)
        }

    @Test
    fun `the group header grows with the row height`() =
        runComposeUiTest {
            setContent {
                val state = rememberTableState(columns = persistentListOf("group", "id"))
                remember { state.groupBy("group") }
                FontScale(2f) {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        Table(itemsCount = 3, itemAt = { Item(it, "a") }, state = state, columns = columns)
                    }
                }
            }
            waitForIdle()
            // The group header spans both columns; the "a" cells of the group column span one.
            val cells = onAllNodesWithText("a")
            val header =
                cells
                    .fetchSemanticsNodes()
                    .indices
                    .map { cells[it].getBoundsInRoot() }
                    .single { it.width > 200.dp }
            assertNear(header.height, 72.dp)
        }

    @Test
    fun `the fast filters row grows with the font`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                state =
                    rememberTableState(
                        columns = persistentListOf("group", "id"),
                        settings = TableSettings(showFastFilters = true),
                    )
                FontScale(2f) {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        Table(
                            itemsCount = 1,
                            itemAt = { Item(it, "a") },
                            state = state,
                            columns = columns,
                            modifier = Modifier.fillMaxSize(),
                            bodyOverlay = { Text("top", Modifier.align(Alignment.TopCenter)) },
                        )
                    }
                }
            }
            waitForIdle()
            assertThat(state.effectiveDimensions.fastFilterRowHeight).isEqualTo(60.dp)
            assertThat(onNodeWithText("top").getBoundsInRoot().top).isGreaterThanOrEqualTo(76.dp + 60.dp)
        }

    @Test
    fun `opting out keeps the given heights at 2x`() =
        runComposeUiTest {
            setContent {
                val state =
                    rememberTableState(
                        columns = persistentListOf("group", "id"),
                        dimensions = TableDefaults.standardDimensions().copy(scaleWithFontSize = false),
                    )
                FontScale(2f) {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        Table(
                            itemsCount = 1,
                            itemAt = { Item(it, "a") },
                            state = state,
                            columns = columns,
                            modifier = Modifier.fillMaxSize(),
                            bodyOverlay = { Text("top", Modifier.align(Alignment.TopCenter)) },
                        )
                    }
                }
            }
            waitForIdle()
            assertNear(onNodeWithText("top").getBoundsInRoot().top, 56.dp)
        }

}
