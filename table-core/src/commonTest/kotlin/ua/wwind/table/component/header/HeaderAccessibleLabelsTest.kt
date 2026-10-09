package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.Table
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import ua.wwind.table.tableColumns
import kotlin.test.Test
import kotlin.test.assertEquals

private fun hasStateDescription(value: String) =
    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

private fun hasClickLabel(label: String) =
    SemanticsMatcher("onClick label is $label") { it.config.getOrNull(SemanticsActions.OnClick)?.label == label }

private object UkrainianStrings : StringProvider {
    @Composable
    override fun get(key: UiString): String =
        when (key) {
            UiString.HeaderSort -> "Сортувати"
            UiString.HeaderFilter -> "Фільтр"
            else -> DefaultStrings.get(key)
        }
}

/**
 * Icon-only header controls and the active-filter scroll arrows are named through [StringProvider] (#78);
 * a click-to-sort heading is a sort button for screen readers (#79).
 */
@OptIn(ExperimentalTestApi::class)
class HeaderAccessibleLabelsTest {
    private fun columns(clickToSort: Boolean) =
        tableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                width(200.dp, 200.dp)
                resizable(false)
                sortable()
                headerClickToSort(clickToSort)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
            }
        }

    private fun ComposeUiTest.setTable(
        clickToSort: Boolean = false,
        strings: StringProvider = DefaultStrings,
        settings: TableSettings = TableSettings(),
    ): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state = rememberTableState(columns = persistentListOf("name"), settings = settings)
            Box(Modifier.size(400.dp)) {
                Table(
                    itemsCount = 1,
                    itemAt = { "row" },
                    state = state,
                    columns = columns(clickToSort),
                    strings = strings,
                )
            }
        }
        waitForIdle()
        return state
    }

    @Test
    fun `the sort button is named sort and reports the sort order as its state`() =
        runComposeUiTest {
            val state = setTable()
            onNodeWithContentDescription("Sort").assert(hasStateDescription("Not sorted"))

            state.setSort("name", SortOrder.ASCENDING)
            waitForIdle()
            onNodeWithContentDescription("Sort").assert(hasStateDescription("Sorted ascending"))

            state.setSort("name", SortOrder.DESCENDING)
            waitForIdle()
            onNodeWithContentDescription("Sort").assert(hasStateDescription("Sorted descending"))
        }

    @Test
    fun `with click to sort the heading is a sort button that reports the sort order as its state`() =
        runComposeUiTest {
            val state = setTable(clickToSort = true)
            val heading = onNode(isHeading() and hasText("Name"))
            heading.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            heading.assert(hasClickLabel("Sort"))
            heading.assert(hasStateDescription("Not sorted"))
            heading.assert(hasContentDescription("Sorted ascending").not())

            state.setSort("name", SortOrder.ASCENDING)
            waitForIdle()
            heading.assert(hasStateDescription("Sorted ascending"))
            heading.assert(hasContentDescription("Sorted ascending").not())

            state.setSort("name", SortOrder.DESCENDING)
            waitForIdle()
            heading.assert(hasStateDescription("Sorted descending"))
        }

    @Test
    fun `with click to sort activating the heading sorts the column`() =
        runComposeUiTest {
            val state = setTable(clickToSort = true)
            onNode(isHeading() and hasText("Name")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            assertEquals(SortOrder.ASCENDING, state.sort?.order)
        }

    @Test
    fun `while rows reorder the heading is not a sort button`() =
        runComposeUiTest {
            setTable(clickToSort = true, settings = TableSettings(rowReorderEnabled = true))
            val heading = onNode(isHeading() and hasText("Name"))
            heading.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            heading.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            heading.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        }

    @Test
    fun `the filter button is named filter and filter active once a filter is set`() =
        runComposeUiTest {
            val state = setTable()
            onNodeWithContentDescription("Filter").assertIsDisplayed()

            state.setFilter("name", TableFilterState(FilterConstraint.CONTAINS, listOf("x")))
            waitForIdle()
            onNodeWithContentDescription("Filter (active)").assertIsDisplayed()
            onNodeWithContentDescription("Filter").assertDoesNotExist()
        }

    @Test
    fun `a custom string provider names the header buttons`() =
        runComposeUiTest {
            setTable(strings = UkrainianStrings)
            onNodeWithContentDescription("Сортувати").assertIsDisplayed()
            onNodeWithContentDescription("Фільтр").assertIsDisplayed()
            onNodeWithContentDescription("Sort").assertDoesNotExist()
        }

    @Test
    fun `the active filter chips scroll arrows are named scroll left and scroll right`() =
        runComposeUiTest {
            val keys = listOf("a", "b", "c", "d")
            val manyColumns =
                tableColumns<String, String, Unit> {
                    keys.forEach { key ->
                        column(key, valueOf = { it }) {
                            header(key.uppercase())
                            width(100.dp, 100.dp)
                            resizable(false)
                            filter(TableFilterType.TextTableFilter())
                            cell { item, _ -> Text(item) }
                        }
                    }
                }
            lateinit var state: TableState<String>
            setContent {
                state =
                    rememberTableState(
                        columns = persistentListOf("a", "b", "c", "d"),
                        settings = TableSettings(showActiveFiltersHeader = true),
                    )
                Box(Modifier.size(300.dp, 400.dp)) {
                    Table(itemsCount = 1, itemAt = { "row" }, state = state, columns = manyColumns)
                }
            }
            waitForIdle()
            keys.forEach { state.setFilter(it, TableFilterState(FilterConstraint.CONTAINS, listOf("a long value"))) }
            waitForIdle()

            onNodeWithContentDescription("Scroll left").assertIsDisplayed()
            onNodeWithContentDescription("Scroll right").assertIsDisplayed()
        }
}
