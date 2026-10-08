package ua.wwind.table.format

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

/** The condition list's icon buttons are named through the string provider (#78). */
@OptIn(ExperimentalTestApi::class)
class FormatConditionLabelsTest {
    private enum class Field { Name }

    @Test
    fun `a condition's expand button is named by its state and its clear button remove filter`() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(600.dp)) {
                    FormatDialogConditionTab(
                        item = TableFormatRule<Field, Unit>(id = 0, columns = listOf(Field.Name), filter = Unit),
                        getTitle = { it.name },
                        filters = { _, _ ->
                            listOf(
                                FormatFilterData(
                                    field = Field.Name,
                                    filterType = TableFilterType.TextTableFilter(),
                                    filterState = TableFilterState(FilterConstraint.CONTAINS, listOf("abc")),
                                    onChange = {},
                                ),
                            )
                        },
                        onChange = {},
                        strings = DefaultStrings,
                    )
                }
            }
            onNodeWithContentDescription("Remove filter").assertExists()
            onNodeWithContentDescription("Collapse").assertDoesNotExist()

            onNodeWithContentDescription("Expand").performClick()
            waitForIdle()

            onNodeWithContentDescription("Collapse").assertExists()
            onNodeWithContentDescription("Expand").assertDoesNotExist()
        }
}
