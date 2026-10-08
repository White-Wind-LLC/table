package ua.wwind.table.format.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ColorPickerDialogTest {
    private val radioButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun `cancel dismisses without choosing a color`() =
        runComposeUiTest {
            val chosen = mutableListOf<Color>()
            var dismissed = false
            setContent {
                ColorPickerDialog(
                    initialColor = Color(0xFF64748B),
                    onDismiss = { dismissed = true },
                    onChooseColor = { chosen.add(it) },
                    strings = DefaultStrings,
                )
            }
            onNodeWithText("Cancel").performScrollTo().performClick()

            assertThat(dismissed).isEqualTo(true)
            assertThat(chosen).isEmpty()
        }

    @Test
    fun `swatches are named radio buttons and OK returns the selected one`() =
        runComposeUiTest {
            val chosen = mutableListOf<Color>()
            setContent {
                ColorPickerDialog(
                    initialColor = Color(0xFF64748B),
                    onDismiss = {},
                    onChooseColor = { chosen.add(it) },
                    strings = DefaultStrings,
                )
            }
            onNodeWithContentDescription("Steel gray").assert(radioButton).assertIsSelected()
            onNodeWithContentDescription("Sky blue").assert(radioButton).assertIsNotSelected().performClick()
            onNodeWithContentDescription("Sky blue").assertIsSelected()
            onNodeWithText("#3B82F6").assertExists()
            onNodeWithText("OK").performScrollTo().performClick()

            assertThat(chosen).containsExactly(Color(0xFF3B82F6))
        }

    @Test
    fun `typed hex is returned on OK`() =
        runComposeUiTest {
            val chosen = mutableListOf<Color>()
            setContent {
                ColorPickerDialog(
                    initialColor = Color.Unspecified,
                    onDismiss = {},
                    onChooseColor = { chosen.add(it) },
                    strings = DefaultStrings,
                )
            }
            onNodeWithText("Hex").performScrollTo().performTextReplacement("#801E3A8A")
            onNodeWithText("OK").performScrollTo().performClick()

            assertThat(chosen).containsExactly(Color(0x801E3A8A))
        }

    @Test
    fun `invalid hex shows a hint and keeps the color`() =
        runComposeUiTest {
            val chosen = mutableListOf<Color>()
            setContent {
                ColorPickerDialog(
                    initialColor = Color(0xFF1E3A8A),
                    onDismiss = {},
                    onChooseColor = { chosen.add(it) },
                    strings = DefaultStrings,
                )
            }
            onNodeWithText("#1E3A8A").performScrollTo().performTextReplacement("#12")
            onNodeWithText("Use #RRGGBB or #AARRGGBB").assertExists()
            onNodeWithText("OK").performScrollTo().performClick()

            assertThat(chosen).containsExactly(Color(0xFF1E3A8A))
        }

    @Test
    fun `low contrast against the paired color shows a warning with the ratio`() =
        runComposeUiTest {
            setContent {
                ColorPickerDialog(
                    initialColor = Color(0xFF64748B),
                    onDismiss = {},
                    onChooseColor = {},
                    strings = DefaultStrings,
                    pairedColor = Color(0xFFFACC15),
                )
            }
            onNodeWithText("Low contrast. Aim for at least 4.5:1").assertExists()
            onNode(hasText("3.1:1") or hasText("3,1:1")).assertExists()
        }

    @Test
    fun `enough contrast shows no warning`() =
        runComposeUiTest {
            setContent {
                ColorPickerDialog(
                    initialColor = Color.Black,
                    onDismiss = {},
                    onChooseColor = {},
                    strings = DefaultStrings,
                    pairedColor = Color.White,
                )
            }
            onNodeWithText("Low contrast. Aim for at least 4.5:1").assertDoesNotExist()
        }
}
