package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private data class MeasuredRow(
    val id: Int,
)

private const val LONG_CELL_TEXT = "a cell wide enough that no header could account for its width"

private fun measuredColumns() =
    tableColumns<MeasuredRow, String, Unit> {
        column("short", valueOf = { it.id }) {
            header("A")
            width(8.dp)
            autoWidth()
            cell { _, _ -> Text("x") }
        }
        column("long", valueOf = { it.id }) {
            header("B")
            width(8.dp)
            autoWidth()
            cell { _, _ -> Text(LONG_CELL_TEXT) }
        }
    }

/**
 * Auto-width measures a throwaway copy of each cell that is composed but never placed. The copy is
 * hidden from selection, so this pins the measurement itself: it has to keep reporting a width that
 * tracks the cell content, and it has to do so on the path that wraps the body in a
 * SelectionContainer.
 */
@OptIn(ExperimentalTestApi::class)
class AutoWidthMeasurementTest {
    @Test
    fun `a selectable table measures each column against its own cell content`() =
        runComposeUiTest {
            lateinit var widths: Map<String, androidx.compose.ui.unit.Dp>
            setContent {
                val columns = remember { measuredColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("short", "long"),
                        settings = TableSettings(enableTextSelection = true),
                    )
                widths = state.columns.contentMaxWidths
                Box(Modifier.size(600.dp, 320.dp)) {
                    Table(
                        itemsCount = 20,
                        itemAt = { index -> MeasuredRow(index) },
                        state = state,
                        columns = columns,
                    )
                }
            }
            waitUntil { widths["short"] != null && widths["long"] != null }

            val short = widths["short"]
            val long = widths["long"]
            assertThat(short).isNotNull()
            assertThat(long).isNotNull()
            assertThat(long!!).isGreaterThan(short!!)
        }
}
