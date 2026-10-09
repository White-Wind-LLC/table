package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import assertk.assertThat
import assertk.assertions.doesNotContainKey
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.config.TableTypography
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
    fun `a group header sits above the first row of its group`() {
        for (reorder in listOf(false, true)) {
            for (scale in listOf(1f, 2f)) {
                assertGroupHeadersAboveRows(reorder, scale)
            }
        }
    }

    private fun assertGroupHeadersAboveRows(
        reorder: Boolean,
        scale: Float,
    ) = runComposeUiTest {
        setContent {
            val state =
                rememberTableState(
                    columns = persistentListOf("group", "id"),
                    settings = TableSettings(rowReorderEnabled = reorder),
                )
            remember { state.groupBy("group") }
            FontScale(scale) {
                Box(Modifier.size(400.dp, 800.dp)) {
                    Table(
                        itemsCount = 4,
                        itemAt = { Item(it, if (it < 2) "a" else "b") },
                        state = state,
                        columns = columns,
                        onRowMove = if (reorder) { _, _ -> } else null,
                    )
                }
            }
        }
        waitForIdle()

        fun groupHeader(value: String) =
            onAllNodesWithText(value).let { nodes ->
                nodes
                    .fetchSemanticsNodes()
                    .indices
                    .map { nodes[it].getBoundsInRoot() }
                    .single { it.width > 200.dp }
            }
        val row0 = onNodeWithText("row-0").getBoundsInRoot()
        val row1 = onNodeWithText("row-1").getBoundsInRoot()
        val row2 = onNodeWithText("row-2").getBoundsInRoot()
        assertThat(row0.top, "row-0 top, reorder=$reorder scale=$scale").isGreaterThanOrEqualTo(groupHeader("a").bottom)
        assertThat(row1.top, "row-1 top, reorder=$reorder scale=$scale").isGreaterThanOrEqualTo(row0.bottom)
        assertThat(groupHeader("b").top, "header b top, reorder=$reorder scale=$scale")
            .isGreaterThanOrEqualTo(row1.bottom)
        assertThat(row2.top, "row-2 top, reorder=$reorder scale=$scale").isGreaterThanOrEqualTo(groupHeader("b").bottom)
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

    @Test
    fun `changing the font scale drops cached row heights`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            var scale by mutableFloatStateOf(1f)
            setContent {
                state =
                    rememberTableState(
                        columns = persistentListOf("group", "id"),
                        settings = TableSettings(rowHeightMode = RowHeightMode.Dynamic),
                    )
                FontScale(scale) {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        Table(itemsCount = 3, itemAt = { Item(it, "a") }, state = state, columns = columns)
                    }
                }
            }
            waitForIdle()
            // An offscreen entry no row re-measures: only a reset removes it.
            runOnIdle { state.rowHeightsPx[999] = 1 }
            scale = 2f
            waitForIdle()
            assertThat(state.rowHeightsPx).doesNotContainKey(999)
        }

    @Test
    fun `a style without a size grows by the ambient text it renders with`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            val bold = TextStyle(fontWeight = FontWeight.Bold)
            setContent {
                state = rememberTableState(columns = persistentListOf("group", "id"))
                FontScale(2f) {
                    ProvideTextStyle(TextStyle(fontSize = 16.sp, lineHeight = 24.sp)) {
                        Box(Modifier.size(400.dp, 500.dp)) {
                            Table(
                                itemsCount = 1,
                                itemAt = { Item(it, "a") },
                                state = state,
                                columns = columns,
                                typography =
                                    TableTypography(header = bold, body = bold, footer = bold, groupHeader = bold),
                            )
                        }
                    }
                }
            }
            waitForIdle()
            assertThat(state.effectiveDimensions.headerHeight).isEqualTo(80.dp)
            assertThat(state.effectiveDimensions.rowHeight).isEqualTo(76.dp)
        }

    @Test
    fun `an equal density in a new object keeps cached row heights`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            var tick by mutableIntStateOf(0)
            setContent {
                state =
                    rememberTableState(
                        columns = persistentListOf("group", "id"),
                        settings = TableSettings(rowHeightMode = RowHeightMode.Dynamic),
                    )
                val outer = LocalDensity.current
                // Reading tick builds a new, equal-valued, non-data Density on every change.
                val density =
                    tick.let {
                        object : Density {
                            override val density = outer.density
                            override val fontScale = outer.fontScale
                        }
                    }
                CompositionLocalProvider(LocalDensity provides density) {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        Table(itemsCount = 3, itemAt = { Item(it, "a") }, state = state, columns = columns)
                    }
                }
            }
            waitForIdle()
            runOnIdle { state.rowHeightsPx[999] = 1 }
            tick++
            waitForIdle()
            assertThat(state.rowHeightsPx[999]).isEqualTo(1)
        }
}
