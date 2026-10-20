package ua.wwind.table.strings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.filter.data.TableFilterNode.Operator
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FilterChipStringsTest {
    @Test
    fun `default strings cover the filter chip keys`() =
        runComposeUiTest {
            val resolved = mutableMapOf<UiString, String>()
            setContent {
                listOf(
                    UiString.FilterOperatorAny,
                    UiString.FilterOperatorAll,
                    UiString.FilterOperatorOfThese,
                    UiString.FilterNot,
                    UiString.FilterOr,
                    UiString.FilterAnd,
                    UiString.FilterBack,
                    UiString.FilterUnknownField,
                ).forEach { resolved[it] = DefaultStrings.get(it) }
            }
            runOnIdle {
                assertThat(resolved).isEqualTo(
                    mapOf(
                        UiString.FilterOperatorAny to "Any",
                        UiString.FilterOperatorAll to "All",
                        UiString.FilterOperatorOfThese to "of these",
                        UiString.FilterNot to "NOT",
                        UiString.FilterOr to "or",
                        UiString.FilterAnd to "and",
                        UiString.FilterBack to "Back",
                        UiString.FilterUnknownField to "Unknown field",
                    ),
                )
            }
        }

    @Test
    fun `remove labels name what they remove`() =
        runComposeUiTest {
            var filter = ""
            var group = ""
            setContent {
                filter = DefaultStrings.removeFilterLabel("Qty")
                group = DefaultStrings.removeFilterGroupLabel("Type, Qty, Vip")
            }
            runOnIdle {
                assertThat(filter).isEqualTo("Remove filter Qty")
                assertThat(group).isEqualTo("Remove group Type, Qty, Vip")
            }
        }

    @Test
    fun `group description reads the operator with its parts and the negation`() =
        runComposeUiTest {
            val actual = mutableListOf<String>()
            setContent {
                actual.clear()
                actual += DefaultStrings.filterGroupDescription(Operator.OR, negated = false, parts = listOf("A", "B"))
                actual += DefaultStrings.filterGroupDescription(Operator.AND, negated = false, parts = listOf("A", "B"))
                actual +=
                    DefaultStrings.filterGroupDescription(Operator.OR, negated = false, parts = listOf("A", "B", "C"))
                actual += DefaultStrings.filterGroupDescription(Operator.OR, negated = true, parts = listOf("A", "B"))
                actual += DefaultStrings.filterGroupDescription(Operator.AND, negated = true, parts = listOf("A"))
            }
            runOnIdle {
                assertThat(actual).isEqualTo(
                    listOf(
                        "Any of: A, or B",
                        "All of: A and B",
                        "Any of: A, or B, or C",
                        "Not any of: A, or B",
                        "Not all of: A",
                    ),
                )
            }
        }
}
