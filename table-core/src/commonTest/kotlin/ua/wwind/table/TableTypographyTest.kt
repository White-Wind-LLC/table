package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.config.TableTypography
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/** Header, body, footer and group rows each get their own [TableTypography] role. */
@OptIn(ExperimentalTestApi::class)
class TableTypographyTest {
    private class Seen {
        var header: TextStyle? = null
        var body: TextStyle? = null
        var footer: TextStyle? = null
        var group: TextStyle? = null
    }

    private fun columns(seen: Seen) =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header { _ ->
                    val style = LocalTextStyle.current
                    SideEffect { seen.header = style }
                    Text("A")
                }
                width(200.dp, 200.dp)
                resizable(false)
                cell { item, _ ->
                    val style = LocalTextStyle.current
                    SideEffect { seen.body = style }
                    Text(item)
                }
                groupHeader { value ->
                    val style = LocalTextStyle.current
                    SideEffect { seen.group = style }
                    Text(value.toString())
                }
                footer { _ ->
                    val style = LocalTextStyle.current
                    SideEffect { seen.footer = style }
                    Text("total")
                }
            }
        }

    private fun styled(style: TableCellStyle) =
        object : TableCustomization<String, String> {
            @Composable
            override fun resolveCellStyle(ctx: TableCellContext<String, String>): TableCellStyle = style
        }

    @Test
    fun `default roles follow the Material type scale`() =
        runComposeUiTest {
            val seen = Seen()
            var expected: TableTypography? = null
            setContent {
                val typography = MaterialTheme.typography
                SideEffect {
                    expected =
                        TableTypography(
                            header = typography.titleSmall,
                            body = typography.bodyMedium,
                            footer = typography.labelLarge,
                            groupHeader = typography.titleSmall,
                        )
                }
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state =
                            rememberTableState(
                                columns = persistentListOf("a"),
                                settings = TableSettings(showFooter = true),
                            ),
                        columns = columns(seen),
                    )
                }
            }

            waitForIdle()
            val roles = expected
            assertThat(roles).isNotNull()
            assertThat(seen.header?.fontSize).isEqualTo(roles!!.header.fontSize)
            assertThat(seen.header?.fontWeight).isEqualTo(roles.header.fontWeight)
            assertThat(seen.body?.fontSize).isEqualTo(roles.body.fontSize)
            assertThat(seen.body?.letterSpacing).isEqualTo(roles.body.letterSpacing)
            assertThat(seen.footer?.fontSize).isEqualTo(roles.footer.fontSize)
            assertThat(seen.footer?.fontWeight).isEqualTo(roles.footer.fontWeight)
        }

    @Test
    fun `group header row uses the groupHeader role`() =
        runComposeUiTest {
            val seen = Seen()
            val groupStyle = TextStyle(fontSize = 21.sp)
            val bodyStyle = TextStyle(fontSize = 11.sp)
            setContent {
                Box(Modifier.size(400.dp)) {
                    val state = rememberTableState(columns = persistentListOf("a"))
                    SideEffect { state.groupBy("a") }
                    Table(
                        itemsCount = 2,
                        itemAt = { "row" },
                        state = state,
                        columns = columns(seen),
                        typography = TableDefaults.typography(body = bodyStyle, groupHeader = groupStyle),
                    )
                }
            }

            waitForIdle()
            assertThat(seen.group?.fontSize).isEqualTo(21.sp)
        }

    @Test
    fun `a custom typography replaces the defaults`() =
        runComposeUiTest {
            val seen = Seen()
            setContent {
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = rememberTableState(columns = persistentListOf("a")),
                        columns = columns(seen),
                        typography =
                            TableDefaults.typography(
                                header = TextStyle(fontSize = 19.sp),
                                body = TextStyle(fontSize = 13.sp),
                            ),
                    )
                }
            }

            waitForIdle()
            assertThat(seen.header?.fontSize).isEqualTo(19.sp)
            assertThat(seen.body?.fontSize).isEqualTo(13.sp)
        }

    @Test
    fun `the cell style's text style merges over the body role`() =
        runComposeUiTest {
            val seen = Seen()
            setContent {
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = rememberTableState(columns = persistentListOf("a")),
                        columns = columns(seen),
                        customization = styled(TableCellStyle(textStyle = TextStyle(fontWeight = FontWeight.Bold))),
                        typography = TableDefaults.typography(body = TextStyle(fontSize = 13.sp)),
                    )
                }
            }

            waitForIdle()
            assertThat(seen.body?.fontWeight).isEqualTo(FontWeight.Bold)
            assertThat(seen.body?.fontSize).isEqualTo(13.sp)
        }
}
