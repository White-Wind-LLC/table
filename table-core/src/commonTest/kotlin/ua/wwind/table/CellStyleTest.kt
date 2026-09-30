package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/** Every field of the `TableCellStyle` that `resolveCellStyle` returns reaches the body cell. */
@OptIn(ExperimentalTestApi::class)
class CellStyleTest {
    private val cellWidth = 200.dp

    private fun columns(onTextStyle: (TextStyle) -> Unit = {}) =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header("A")
                width(cellWidth, cellWidth)
                resizable(false)
                cell { item, _ ->
                    val style = LocalTextStyle.current
                    SideEffect { onTextStyle(style) }
                    Text(item)
                }
            }
        }

    private fun styled(style: TableCellStyle) =
        object : TableCustomization<String, String> {
            @Composable
            override fun resolveCellStyle(ctx: TableCellContext<String, String>): TableCellStyle = style
        }

    @Test
    fun `cell content receives the style's text style`() =
        runComposeUiTest {
            var seen: TextStyle? = null
            setContent {
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = rememberTableState(columns = persistentListOf("a")),
                        columns = columns { seen = it },
                        customization = styled(TableCellStyle(textStyle = TextStyle(fontWeight = FontWeight.Bold))),
                    )
                }
            }

            waitForIdle()
            assertThat(seen?.fontWeight).isEqualTo(FontWeight.Bold)
        }

    @Test
    fun `cell content follows the style's alignment`() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = rememberTableState(columns = persistentListOf("a")),
                        columns = columns(),
                        customization = styled(TableCellStyle(alignment = Alignment.CenterEnd)),
                    )
                }
            }

            waitForIdle()
            val textLeft = onNodeWithText("row", useUnmergedTree = true).getBoundsInRoot().left
            assertThat(textLeft).isGreaterThan(cellWidth / 2)
        }

    @Test
    fun `the style's modifier is applied to the cell`() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = rememberTableState(columns = persistentListOf("a")),
                        columns = columns(),
                        customization = styled(TableCellStyle(modifier = Modifier.testTag("styled-cell"))),
                    )
                }
            }

            waitForIdle()
            onNodeWithTag("styled-cell", useUnmergedTree = true).assertIsDisplayed()
        }
}
