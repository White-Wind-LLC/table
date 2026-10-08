package ua.wwind.table.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import ua.wwind.table.buildFilterChipTextUnsafe
import ua.wwind.table.filter.component.main.FilterPanel
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.DoubleDelegate
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.test.Test

/** Number fields take and show the decimal separator of [StringProvider.formatNumber] (issue #118). */
@OptIn(ExperimentalTestApi::class)
class LocaleNumberInputTest {
    private val commaStrings =
        object : StringProvider {
            @Composable
            override fun get(key: UiString): String = DefaultStrings.get(key)

            @Composable
            override fun formatNumber(value: Number): String = value.toString().replace('.', ',')
        }

    private val type = TableFilterType.NumberTableFilter(delegate = DoubleDelegate)

    // Returns the TestResult: on web runComposeUiTest runs asynchronously, so the assertion stays inside it.
    private fun typeIntoPanel(input: String) =
        runComposeUiTest {
            var applied: TableFilterState<Double>? = null
            var state by mutableStateOf(TableFilterState<Double>(FilterConstraint.EQUALS, null))
            setContent {
                FilterPanel(
                    type = type,
                    state = state,
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = commaStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = { newState ->
                        applied = newState
                        if (newState != null) state = newState
                    },
                )
            }
            onNodeWithText("Value").performTextInput(input)
            waitForIdle()
            assertThat(applied).isEqualTo(TableFilterState(FilterConstraint.EQUALS, listOf(1.5)))
            onNodeWithText("1,5").assertIsDisplayed()
        }

    @Test
    fun `typing the locale separator applies the value`() = typeIntoPanel("1,5")

    @Test
    fun `typing a dot applies the value and shows the locale separator`() = typeIntoPanel("1.5")

    @Test
    fun `applied value shows the locale separator and agrees with the chip`() =
        runComposeUiTest {
            var chip: String? = null
            val state = TableFilterState(FilterConstraint.EQUALS, listOf(1.5))
            setContent {
                FilterPanel(
                    type = type,
                    state = state,
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = commaStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
                chip = buildFilterChipTextUnsafe(type, state, commaStrings)
            }
            onNodeWithText("1,5").assertIsDisplayed()
            assertThat(chip).isNotNull().endsWith(" 1,5")
        }

    @Test
    fun `default strings keep the dot`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = type,
                    state = TableFilterState(FilterConstraint.EQUALS, listOf(1.5)),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DotStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("1.5").assertIsDisplayed()
        }

    /** en-US number format, whatever the machine's locale. */
    private object DotStrings : StringProvider {
        @Composable
        override fun get(key: UiString): String = DefaultStrings.get(key)

        @Composable
        override fun formatNumber(value: Number): String = value.toString()
    }
}
