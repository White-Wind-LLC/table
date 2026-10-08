package ua.wwind.table.format

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

/** Changing only the operator of a condition must reach the rule, not wait for a value edit. */
@OptIn(ExperimentalTestApi::class)
class FormatConditionOperatorTest {
    private enum class Color { Red, Green }

    private fun SemanticsNodeInteractionsProvider.switchOperator(
        from: String,
        to: String,
    ) {
        onNodeWithText(from).performClick()
        onNodeWithText(to).performClick()
    }

    @Test
    fun `text condition reports an operator change`() =
        runComposeUiTest {
            var last: TableFilterState<String>? = null
            setContent {
                FormatTextFilter(
                    filter = TableFilterType.TextTableFilter(),
                    state = TableFilterState(FilterConstraint.CONTAINS, listOf("abc")),
                    onChange = { last = it },
                    strings = DefaultStrings,
                )
            }
            switchOperator(from = "Contains", to = "Equals")
            waitForIdle()

            assertThat(last).isNotNull()
            assertThat(last?.constraint).isEqualTo(FilterConstraint.EQUALS)
            assertThat(last?.values).isEqualTo(listOf("abc"))
        }

    @Test
    fun `enum condition reports an operator change`() =
        runComposeUiTest {
            var last: TableFilterState<*>? = null
            setContent {
                FormatEnumFilter(
                    filter =
                        TableFilterType.EnumTableFilter(
                            options = persistentListOf(Color.Red, Color.Green),
                            getTitle = { it.name },
                        ),
                    state = TableFilterState(FilterConstraint.IN, listOf(Color.Red)),
                    onChange = { last = it },
                    strings = DefaultStrings,
                )
            }
            switchOperator(from = "Is any of", to = "Is none of")
            waitForIdle()

            assertThat(last?.constraint).isEqualTo(FilterConstraint.NOT_IN)
            assertThat(last?.values).isEqualTo(listOf(Color.Red))
        }

    @Test
    fun `number condition reports an operator change`() =
        runComposeUiTest {
            var last: TableFilterState<Int>? = null
            setContent {
                FormatNumberFilter(
                    filter = TableFilterType.NumberTableFilter(delegate = IntDelegate),
                    state = TableFilterState(FilterConstraint.GT, listOf(5)),
                    onChange = { last = it },
                    strings = DefaultStrings,
                )
            }
            switchOperator(from = "Greater than", to = "Equals")
            waitForIdle()

            assertThat(last?.constraint).isEqualTo(FilterConstraint.EQUALS)
            assertThat(last?.values).isEqualTo(listOf(5))
        }
}
