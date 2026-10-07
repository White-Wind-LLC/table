package ua.wwind.table.component.header

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import ua.wwind.table.component.ColumnMenuItemId
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.UiString
import kotlin.test.Test

// Resource painters are loaded from the classpath here; on web they load asynchronously.
@OptIn(ExperimentalTestApi::class)
class ColumnMenuResolveTest {
    private val pin = ColumnMenuEntry(ColumnMenuItemId("pin"), UiString.ColumnMenuPinLeft, TableIcons.PushPin) {}
    private val clear = ColumnMenuEntry(ColumnMenuItemId("clear"), UiString.ColumnMenuClearFilter, TableIcons.Close) {}

    // An item appearing or disappearing above another must not rebuild that item's painter: on web
    // a rebuilt resource painter is blank until it loads again.
    @Test
    fun `an item keeps its painter when an item before it goes away`() =
        runComposeUiTest {
            var entries by mutableStateOf(listOf(pin, clear))
            var clearPainter: Painter? = null
            setContent {
                val items = listOf(ColumnMenuEntrySection("s", entries)).resolve(DefaultStrings).single().items
                clearPainter = items.single { it.id == clear.id }.icon
            }
            waitForIdle()
            val before = clearPainter

            entries = listOf(clear)
            waitForIdle()

            assertThat(clearPainter).isSameInstanceAs(before)
        }
}
