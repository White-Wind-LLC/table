package ua.wwind.table.filter

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.filter.component.main.FilterPanel
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

private enum class Planet { Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, Pluto }

/** Enum options search, Select all / None and the boolean "Any" option (issue #104). */
@OptIn(ExperimentalTestApi::class)
class EnumBooleanFilterPanelTest {
    private fun planetFilter(options: ImmutableList<Planet> = Planet.entries.toImmutableList()) =
        TableFilterType.EnumTableFilter(options = options, getTitle = { it.name })

    @Suppress("UNCHECKED_CAST")
    private fun planetState(
        constraint: FilterConstraint,
        values: List<Planet>?,
    ) = TableFilterState(constraint, values) as TableFilterState<ImmutableList<Planet>>

    @Test
    fun `search narrows a long options list`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = planetFilter(),
                    state = planetState(FilterConstraint.IN, null),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Select Many").performClick()
            onNodeWithText("Search options…").performTextInput("ur")
            onNodeWithText("Saturn").assertIsDisplayed()
            onNodeWithText("Venus").assertDoesNotExist()
        }

    @Test
    fun `short options list has no search field`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = planetFilter(persistentListOf(Planet.Mercury, Planet.Venus, Planet.Earth)),
                    state = planetState(FilterConstraint.IN, null),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Select Many").performClick()
            onNodeWithText("Venus").assertIsDisplayed()
            onNodeWithText("Search options…").assertDoesNotExist()
        }

    @Test
    fun `select all adds only the options matching the search`() =
        runComposeUiTest {
            var applied: TableFilterState<*>? = null
            setContent {
                FilterPanel(
                    type = planetFilter(),
                    state = planetState(FilterConstraint.IN, listOf(Planet.Earth)),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = { applied = it },
                )
            }
            onNodeWithText("Earth").performClick()
            onNodeWithText("Search options…").performTextInput("ur")
            onNodeWithText("Select all").performClick()
            waitForIdle()
            assertThat(applied?.values.orEmpty())
                .containsExactlyInAnyOrder(Planet.Earth, Planet.Mercury, Planet.Saturn, Planet.Uranus)
        }

    @Test
    fun `none removes only the options matching the search`() =
        runComposeUiTest {
            var applied: TableFilterState<*>? = null
            setContent {
                FilterPanel(
                    type = planetFilter(),
                    state = planetState(FilterConstraint.IN, listOf(Planet.Earth, Planet.Mercury, Planet.Saturn)),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = { applied = it },
                )
            }
            onNodeWithText("Earth, Mercury, Saturn").performClick()
            onNodeWithText("Search options…").performTextInput("ur")
            onNodeWithText("None").performClick()
            waitForIdle()
            assertThat(applied?.values.orEmpty()).containsExactlyInAnyOrder(Planet.Earth)
        }

    @Test
    fun `boolean panel selects any when no filter is set`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.BooleanTableFilter(),
                    state = null,
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Any").assertIsSelected()
            onNodeWithText("Yes").assertIsNotSelected()
        }

    @Test
    fun `boolean any clears an active filter`() =
        runComposeUiTest {
            var applied: TableFilterState<*>? = TableFilterState(FilterConstraint.EQUALS, listOf(true))
            setContent {
                FilterPanel(
                    type = TableFilterType.BooleanTableFilter(),
                    state = TableFilterState(FilterConstraint.EQUALS, listOf(true)),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = { applied = it },
                )
            }
            onNodeWithText("Yes").assertIsSelected()
            onNodeWithText("Any").performClick()
            waitForIdle()
            assertThat(applied).isNull()
        }
}
