package ua.wwind.table

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableDefaults
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
}
