package ua.wwind.table.sample.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import ua.wwind.table.sample.viewmodel.SampleViewModel
import kotlin.test.Test

private const val BLOCKED = "Filter contains unknown fields — remove them to apply"

/** The sample app shows no rows while its filter has unknown fields, and rows again once it is valid. */
@OptIn(ExperimentalTestApi::class)
class UnresolvedFilterDemoTest {
    @Test
    fun `unknown fields demo shows no rows until the unknown fields are removed`() =
        runComposeUiTest {
            val viewModel = SampleViewModel()
            setContent {
                Box(Modifier.size(1500.dp, 950.dp)) { SampleApp(viewModel = viewModel) }
            }
            waitUntil(timeoutMillis = 10_000) {
                viewModel.tableData.value.displayedPeople
                    .isNotEmpty()
            }

            onNodeWithText("Unknown fields demo").performClick()
            waitForIdle()

            assertThat(viewModel.tableData.value.displayedPeople).isEmpty()
            // The active filters row and the table's empty state both say so.
            assertThat(onAllNodesWithText(BLOCKED).fetchSemanticsNodes().size).isEqualTo(2)
            onNodeWithText("No results match the current filters").assertDoesNotExist()

            onNodeWithContentDescription("Remove filter Salary band").performClick()
            waitForIdle()
            onAllNodesWithText(BLOCKED).onFirst().assertIsDisplayed()
            assertThat(viewModel.tableData.value.displayedPeople).isEmpty()

            onAllNodesWithContentDescription("Unknown field: legacyField", substring = true).onFirst().performClick()
            waitForIdle()
            onNodeWithContentDescription("Remove filter legacyField").performClick()
            waitUntil(timeoutMillis = 10_000) {
                viewModel.tableData.value.displayedPeople
                    .isNotEmpty()
            }

            onNodeWithText(BLOCKED).assertDoesNotExist()
        }
}
