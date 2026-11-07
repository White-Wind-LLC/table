package ua.wwind.table.filter

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.filter.component.main.FilterPanel
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

private enum class Tracking { None, Batch, Serial }

/** An enum filter option may carry a hint line under its title. */
@OptIn(ExperimentalTestApi::class)
class EnumFilterHintTest {
    private val serialHint = "Identical units told apart only by a number"

    private fun trackingFilter(getHint: ((Tracking) -> String?)?) =
        TableFilterType.EnumTableFilter(
            options = Tracking.entries.toImmutableList(),
            getTitle = { it.name },
            getHint = getHint?.let { hint -> { value: Tracking -> hint(value) } },
        )

    @Suppress("UNCHECKED_CAST")
    private fun trackingState(
        constraint: FilterConstraint,
        values: List<Tracking>?,
    ) = TableFilterState(constraint, values) as TableFilterState<ImmutableList<Tracking>>

    @Test
    fun `an option shows its hint under the title`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = trackingFilter { if (it == Tracking.Serial) serialHint else null },
                    state = trackingState(FilterConstraint.IN, null),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Select many").performClick()
            onNodeWithText("Serial").assertIsDisplayed()
            onNodeWithText(serialHint).assertIsDisplayed()
        }

    @Test
    fun `the selected value shows its title only`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = trackingFilter { if (it == Tracking.Serial) serialHint else null },
                    state = trackingState(FilterConstraint.EQUALS, listOf(Tracking.Serial)),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Serial").assertIsDisplayed()
            assertThat(onAllNodesWithText(serialHint).fetchSemanticsNodes().size).isEqualTo(0)
        }

    @Test
    fun `a filter without hints shows titles only`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = trackingFilter(null),
                    state = trackingState(FilterConstraint.IN, null),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Select many").performClick()
            onNodeWithText("Serial").assertIsDisplayed()
            assertThat(onAllNodesWithText(serialHint).fetchSemanticsNodes().size).isEqualTo(0)
        }
}
