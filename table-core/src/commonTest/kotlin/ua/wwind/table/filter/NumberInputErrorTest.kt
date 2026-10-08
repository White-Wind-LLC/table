package ua.wwind.table.filter

import androidx.compose.ui.text.input.KeyboardType
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ua.wwind.table.filter.component.main.number.NumberInputError
import ua.wwind.table.filter.component.main.number.numberInputError
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.DoubleDelegate
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import kotlin.test.Test

class NumberInputErrorTest {
    private fun error(
        text: String,
        secondText: String = "",
        constraint: FilterConstraint,
    ): NumberInputError? = numberInputError(text, secondText, constraint, IntDelegate)

    @Test
    fun `valid single value has no error`() {
        assertThat(error("5", constraint = FilterConstraint.EQUALS)).isNull()
    }

    @Test
    fun `blank single value has no error`() {
        assertThat(error("", constraint = FilterConstraint.GT)).isNull()
    }

    @Test
    fun `unparsable single value is an invalid number`() {
        assertThat(error("-", constraint = FilterConstraint.EQUALS)).isEqualTo(NumberInputError.InvalidNumber)
    }

    @Test
    fun `unparsable double is an invalid number`() {
        assertThat(numberInputError(".", "", FilterConstraint.EQUALS, DoubleDelegate))
            .isEqualTo(NumberInputError.InvalidNumber)
    }

    @Test
    fun `null check has no error`() {
        assertThat(error("-", constraint = FilterConstraint.IS_NULL)).isNull()
    }

    @Test
    fun `valid range has no error`() {
        assertThat(error("1", "5", FilterConstraint.BETWEEN)).isNull()
    }

    @Test
    fun `blank range has no error`() {
        assertThat(error("", "", FilterConstraint.BETWEEN)).isNull()
    }

    @Test
    fun `range with one bound is incomplete`() {
        assertThat(error("1", "", FilterConstraint.BETWEEN)).isEqualTo(NumberInputError.RangeIncomplete)
        assertThat(error("", "5", FilterConstraint.BETWEEN)).isEqualTo(NumberInputError.RangeIncomplete)
    }

    @Test
    fun `range with an unparsable bound is an invalid number`() {
        assertThat(error("-", "5", FilterConstraint.BETWEEN)).isEqualTo(NumberInputError.InvalidNumber)
    }

    @Test
    fun `range with from above to is inverted`() {
        assertThat(error("9", "5", FilterConstraint.BETWEEN)).isEqualTo(NumberInputError.RangeInverted)
    }

    @Test
    fun `int delegate uses a number keyboard and double delegate a decimal one`() {
        assertThat(IntDelegate.keyboardType).isEqualTo(KeyboardType.Number)
        assertThat(DoubleDelegate.keyboardType).isEqualTo(KeyboardType.Decimal)
    }
}
