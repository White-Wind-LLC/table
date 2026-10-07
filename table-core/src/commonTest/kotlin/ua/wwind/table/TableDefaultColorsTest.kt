package ua.wwind.table

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.resolve
import ua.wwind.table.config.resolveRowSelectedIndicatorColor
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TableDefaultColorsTest {
    @Test
    fun `selected and striped rows use readable container roles`() =
        runComposeUiTest {
            var scheme: ColorScheme? = null
            var colors: TableColors? = null
            setContent {
                val s = MaterialTheme.colorScheme
                val c = TableDefaults.colors()
                SideEffect {
                    scheme = s
                    colors = c
                }
            }

            waitForIdle()
            assertThat(scheme).isNotNull()
            assertThat(colors?.rowSelectedContainerColor).isEqualTo(scheme!!.secondaryContainer)
            assertThat(colors?.stripedRowContainerColor).isEqualTo(scheme!!.surfaceContainerLow)
        }

    @Test
    fun `an unspecified selection indicator resolves to primary`() =
        runComposeUiTest {
            var primary: Color? = null
            var resolved: Color? = null
            var explicit: Color? = null
            setContent {
                val base = TableDefaults.colors()
                val p = MaterialTheme.colorScheme.primary
                val r = resolveRowSelectedIndicatorColor(base.copy(rowSelectedIndicatorColor = Color.Unspecified))
                val e = resolveRowSelectedIndicatorColor(base.copy(rowSelectedIndicatorColor = Color.Red))
                SideEffect {
                    primary = p
                    resolved = r
                    explicit = e
                }
            }

            waitForIdle()
            assertThat(resolved).isEqualTo(primary)
            assertThat(explicit).isEqualTo(Color.Red)
        }

    @Test
    fun `a directly built palette resolves its unspecified colors from the theme`() =
        runComposeUiTest {
            var scheme: ColorScheme? = null
            var resolved: TableColors? = null
            setContent {
                val s = MaterialTheme.colorScheme
                val r =
                    TableColors(
                        headerContainerColor = Color.White,
                        headerContentColor = Color.Black,
                        rowContainerColor = Color.White,
                        rowSelectedContainerColor = Color.Gray,
                        stripedRowContainerColor = Color.LightGray,
                        groupContainerColor = Color.Blue,
                        footerContainerColor = Color.White,
                        footerContentColor = Color.Black,
                    ).resolve()
                SideEffect {
                    scheme = s
                    resolved = r
                }
            }

            waitForIdle()
            val s = scheme!!
            val r = resolved!!
            assertThat(r.dividerColor).isEqualTo(s.outlineVariant)
            assertThat(r.pinnedDividerColor).isEqualTo(s.outlineVariant)
            assertThat(r.borderColor).isEqualTo(s.outlineVariant)
            assertThat(r.focusIndicatorColor).isEqualTo(s.primary)
            assertThat(r.hoverColor).isEqualTo(s.onSurface)
            assertThat(r.groupContentColor).isEqualTo(s.contentColorFor(Color.Blue))
            assertThat(r.stickyGroupContainerColor).isEqualTo(Color.Blue)
        }

    @Test
    fun `explicit colors survive resolution`() =
        runComposeUiTest {
            var resolved: TableColors? = null
            setContent {
                val r =
                    TableDefaults
                        .colors(
                            dividerColor = Color.Red,
                            pinnedDividerColor = Color.Green,
                            borderColor = Color.Blue,
                            focusIndicatorColor = Color.Cyan,
                            hoverColor = Color.Magenta,
                            groupContentColor = Color.Yellow,
                            stickyGroupContainerColor = Color.DarkGray,
                        ).resolve()
                SideEffect { resolved = r }
            }

            waitForIdle()
            val r = resolved!!
            assertThat(r.dividerColor).isEqualTo(Color.Red)
            assertThat(r.pinnedDividerColor).isEqualTo(Color.Green)
            assertThat(r.borderColor).isEqualTo(Color.Blue)
            assertThat(r.focusIndicatorColor).isEqualTo(Color.Cyan)
            assertThat(r.hoverColor).isEqualTo(Color.Magenta)
            assertThat(r.groupContentColor).isEqualTo(Color.Yellow)
            assertThat(r.stickyGroupContainerColor).isEqualTo(Color.DarkGray)
        }
}
