package ua.wwind.table.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isGreaterThan
import kotlin.math.abs
import kotlin.test.Test

// Pixels are read here: the selection highlight is drawn, not exposed in semantics.
@OptIn(ExperimentalTestApi::class)
class TableTextFieldSelectionTest {
    @Test
    fun `selection takes the app's text selection colors`() =
        runComposeUiTest {
            val highlight = Color(0x66FFB300)
            setContent {
                MaterialTheme {
                    CompositionLocalProvider(
                        LocalTextSelectionColors provides TextSelectionColors(Color(0xFFE65100), highlight),
                    ) {
                        Box(Modifier.background(Color.White).padding(8.dp).testTag("root")) {
                            TableTextField(
                                "Selected text",
                                {},
                                Modifier.width(240.dp).testTag("field"),
                                singleLine = true,
                            )
                        }
                    }
                }
            }
            onNodeWithTag("field").performClick()
            onNodeWithTag("field").performTextInputSelection(TextRange(0, 8))
            waitForIdle()

            // The field's container is transparent, so the highlight lies over the white root.
            val expected = highlight.compositeOver(Color.White)
            val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
            var matches = 0
            for (x in 0 until pixels.width) {
                for (y in 0 until pixels.height) {
                    val c = pixels[x, y]
                    if (abs(c.red - expected.red) < 0.03f && abs(c.green - expected.green) < 0.03f &&
                        abs(c.blue - expected.blue) < 0.03f
                    ) {
                        matches++
                    }
                }
            }
            assertThat(matches).isGreaterThan(MIN_HIGHLIGHT_PIXELS)
        }

    private companion object {
        const val MIN_HIGHLIGHT_PIXELS = 50
    }
}
