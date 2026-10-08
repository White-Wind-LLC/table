package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableMotion
import ua.wwind.table.config.TableSettings
import ua.wwind.table.config.isReduced
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/** Rows move to their new place after a sort, and reduced motion makes the move instant (issue #92). */
@OptIn(ExperimentalTestApi::class)
class TableMotionTest {
    private val sorted = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")

    private fun ComposeUiTest.showTable(
        motion: TableMotion,
        items: MutableState<List<String>>,
    ) {
        setContent {
            val columns =
                remember {
                    tableColumns<String, String, Unit> {
                        column("name", valueOf = { it }) {
                            header("Name")
                            width(160.dp, 160.dp)
                            cell { item, _ -> Text(item) }
                        }
                    }
                }
            val state =
                rememberTableState(
                    columns = persistentListOf("name"),
                    settings = TableSettings(motion = motion),
                )
            Box(Modifier.size(200.dp, 400.dp)) {
                Table(
                    itemsCount = items.value.size,
                    itemAt = { items.value.getOrNull(it) },
                    state = state,
                    columns = columns,
                    rowKey = { item, index -> item ?: index },
                )
            }
        }
    }

    private fun ComposeUiTest.topOf(text: String): Dp = onNodeWithText(text).getBoundsInRoot().top

    /** Reverses the rows with the clock paused and returns where "Apple" is one frame later and at rest. */
    private fun ComposeUiTest.appleTopAfterReverse(items: MutableState<List<String>>): Pair<Dp, Dp> {
        waitForIdle()
        mainClock.autoAdvance = false
        items.value = items.value.reversed()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        val early = topOf("Apple")
        mainClock.autoAdvance = true
        waitForIdle()
        return early to topOf("Apple")
    }

    @Test
    fun `rows animate to their new place after a sort`() =
        runComposeUiTest {
            val items = mutableStateOf(sorted)
            showTable(TableMotion.Full, items)
            val (early, settled) = appleTopAfterReverse(items)
            assertThat(early).isNotEqualTo(settled)
        }

    @Test
    fun `reduced motion puts rows in place at once after a sort`() =
        runComposeUiTest {
            val items = mutableStateOf(sorted)
            showTable(TableMotion.Reduced, items)
            val (early, settled) = appleTopAfterReverse(items)
            assertThat(early).isEqualTo(settled)
        }

    @Test
    fun `explicit motion settings ignore the platform`() =
        runComposeUiTest {
            var full: Boolean? = null
            var reduced: Boolean? = null
            setContent {
                full = TableMotion.Full.isReduced()
                reduced = TableMotion.Reduced.isReduced()
            }
            waitForIdle()
            assertThat(full).isEqualTo(false)
            assertThat(reduced).isEqualTo(true)
        }
}
