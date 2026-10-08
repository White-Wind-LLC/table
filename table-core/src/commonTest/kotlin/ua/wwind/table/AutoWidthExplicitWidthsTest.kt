package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.doesNotContainKey
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEqualTo
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ua.wwind.table.state.ColumnWidthAction
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private data class WidthRow(
    val id: Int,
)

private const val WIDE_CELL_TEXT = "a cell wide enough to push its column past the explicit width"

private fun widthColumns() =
    tableColumns<WidthRow, String, Unit> {
        column("name", valueOf = { it.id }) {
            header("Name")
            width(8.dp)
            autoWidth(max = 500.dp)
            cell { _, _ -> Text(WIDE_CELL_TEXT) }
        }
        column("other", valueOf = { it.id }) {
            header("Other")
            width(8.dp)
            autoWidth()
            cell { _, _ -> Text(WIDE_CELL_TEXT) }
        }
    }

/**
 * Auto-fit only sizes columns that have no explicit width (#116): widths passed as `initialWidths`,
 * restored with `setWidths` or set by a resize survive both auto-fit passes, and
 * `explicitWidths` leaves out what auto-fit wrote.
 */
@OptIn(ExperimentalTestApi::class)
class AutoWidthExplicitWidthsTest {
    private fun runTable(
        embedded: Boolean = false,
        initialWidths: ImmutableMap<String, Dp> = persistentMapOf(),
        initialCount: Int = 20,
        block: ComposeUiTest.(state: () -> TableState<String>, setCount: (Int) -> Unit) -> Unit,
    ) = runComposeUiTest {
        lateinit var state: TableState<String>
        var count by mutableIntStateOf(initialCount)
        setContent {
            val columns = remember { widthColumns() }
            state =
                rememberTableState(
                    columns = persistentListOf("name", "other"),
                    initialWidths = initialWidths,
                )
            Box(Modifier.size(1200.dp, 320.dp)) {
                Table(
                    itemsCount = count,
                    itemAt = { index -> WidthRow(index) },
                    state = state,
                    columns = columns,
                    embedded = embedded,
                )
            }
        }
        block({ state }, { count = it })
    }

    @Test
    fun `initialWidths survive auto-fit once data is visible`() =
        runTable(initialWidths = persistentMapOf("name" to 400.dp)) { state, _ ->
            waitUntil { state().columns.autoWidthAppliedForData }
            waitForIdle()

            assertThat(state().columns.widths["name"]).isEqualTo(400.dp)
        }

    @Test
    fun `initialWidths survive auto-fit in an embedded table`() =
        runTable(embedded = true, initialWidths = persistentMapOf("name" to 400.dp)) { state, _ ->
            waitUntil { state().columns.autoWidthAppliedForData }
            waitForIdle()

            assertThat(state().columns.widths["name"]).isEqualTo(400.dp)
        }

    @Test
    fun `widths restored before data arrives survive the data pass`() =
        runTable(initialCount = 0) { state, setCount ->
            waitUntil { state().columns.autoWidthAppliedForEmpty }
            runOnIdle { state().columns.setWidths(mapOf("name" to 400.dp)) }
            setCount(20)
            waitUntil { state().columns.autoWidthAppliedForData }
            waitForIdle()

            assertThat(state().columns.widths["name"]).isEqualTo(400.dp)
        }

    @Test
    fun `a column without an explicit width still fits its content`() =
        runTable(initialWidths = persistentMapOf("name" to 400.dp)) { state, _ ->
            waitUntil { state().columns.autoWidthAppliedForData }
            waitForIdle()

            val other = state().columns.widths["other"]!!
            assertThat(other).isGreaterThan(8.dp)
            assertThat(other).isEqualTo(state().columns.contentMaxWidths["other"]!!)
        }

    @Test
    fun `explicitWidths leaves out auto-fit results and keeps resizes`() =
        runTable(initialWidths = persistentMapOf("name" to 400.dp)) { state, _ ->
            waitUntil { state().columns.autoWidthAppliedForData }
            waitForIdle()

            assertThat(state().columns.explicitWidths).isEqualTo(mapOf("name" to 400.dp))

            runOnIdle { state().columns.resize("other", ColumnWidthAction.Set(321.dp)) }
            assertThat(state().columns.explicitWidths)
                .isEqualTo(mapOf("name" to 400.dp, "other" to 321.dp))

            runOnIdle { state().columns.resize("name", ColumnWidthAction.Reset) }
            assertThat(state().columns.explicitWidths).doesNotContainKey("name")
        }

    @Test
    fun `recalculateAutoWidths overrides an explicit width`() =
        runTable(initialWidths = persistentMapOf("name" to 600.dp)) { state, _ ->
            waitUntil { state().columns.autoWidthAppliedForData }
            runOnIdle { state().columns.recalculateAutoWidths() }
            waitUntil { state().columns.autoWidthAppliedForData }
            waitForIdle()

            assertThat(state().columns.widths["name"]).isNotEqualTo(600.dp)
            assertThat(state().columns.explicitWidths).doesNotContainKey("name")
        }
}
