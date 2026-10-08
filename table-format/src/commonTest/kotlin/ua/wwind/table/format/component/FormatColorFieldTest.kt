package ua.wwind.table.format.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactly
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FormatColorFieldTest {
    private enum class Field { A }

    @Test
    fun `field shows the hex of its color and a named clear button`() =
        runComposeUiTest {
            val changes = mutableListOf<Color?>()
            setContent {
                FormatColorField<Field>(
                    color = Color(0xFF1E3A8A),
                    label = "Content color",
                    onClick = { changes.add(it) },
                    strings = DefaultStrings,
                )
            }
            onNodeWithText("#1E3A8A").assertExists()
            onNodeWithText("Content color").assertExists()
            onNodeWithContentDescription("Clear color").performClick()

            assertThat(changes).containsExactly(null)
        }
}
