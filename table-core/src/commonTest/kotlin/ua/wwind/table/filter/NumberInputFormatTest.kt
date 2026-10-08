package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ua.wwind.table.filter.data.NumberInputFormat
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.DoubleDelegate
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.NumberFilterDelegate
import ua.wwind.table.filter.data.decimalSeparatorOf
import kotlin.test.Test

/** Number input accepts the locale decimal separator and `.` (issue #118). */
class NumberInputFormatTest {
    private val comma = NumberInputFormat(DoubleDelegate, ',')
    private val dot = NumberInputFormat(DoubleDelegate, '.')

    @Test
    fun `comma locale parses the locale separator and a dot`() {
        assertThat(comma.parse("1,5")).isEqualTo(1.5)
        assertThat(comma.parse("1.5")).isEqualTo(1.5)
        assertThat(comma.parse("-0,25")).isEqualTo(-0.25)
    }

    @Test
    fun `comma locale accepts partial input with either separator`() {
        assertThat(comma.accepts("1,")).isTrue()
        assertThat(comma.accepts(",5")).isTrue()
        assertThat(comma.accepts("1.")).isTrue()
        assertThat(comma.accepts("-")).isTrue()
    }

    @Test
    fun `two decimal separators are rejected`() {
        assertThat(comma.accepts("1,2.3")).isFalse()
        assertThat(comma.accepts("1,2,3")).isFalse()
    }

    @Test
    fun `comma locale formats with the locale separator`() {
        assertThat(comma.format(1.5)).isEqualTo("1,5")
        assertThat(comma.parse(comma.format(-1234.25))).isEqualTo(-1234.25)
    }

    @Test
    fun `dot locale is unchanged`() {
        assertThat(dot.accepts("1,5")).isFalse()
        assertThat(dot.parse("1.5")).isEqualTo(1.5)
        assertThat(dot.format(1.5)).isEqualTo("1.5")
    }

    @Test
    fun `int delegate rejects a decimal separator`() {
        val ints = NumberInputFormat(IntDelegate, ',')
        assertThat(ints.accepts("1,")).isFalse()
        assertThat(ints.accepts("1.")).isFalse()
        assertThat(ints.parse("12")).isEqualTo(12)
        assertThat(ints.format(1234)).isEqualTo("1234")
    }

    @Test
    fun `custom delegate keeps its own contract`() {
        val format = NumberInputFormat(LongCents, ',')
        assertThat(format.accepts("12,34")).isTrue()
        assertThat(format.parse("12,34")).isEqualTo(1234L)
        assertThat(format.format(1234L)).isEqualTo("12,34")
        assertThat(format.parse("abc")).isNull()
    }

    @Test
    fun `separator is read from the formatted sample`() {
        assertThat(decimalSeparatorOf("1,5")).isEqualTo(',')
        assertThat(decimalSeparatorOf("1.5")).isEqualTo('.')
        assertThat(decimalSeparatorOf("N:1,5")).isEqualTo(',')
        assertThat(decimalSeparatorOf("1٫5")).isEqualTo('٫')
    }

    @Test
    fun `separator falls back to a dot for unexpected samples`() {
        assertThat(decimalSeparatorOf("")).isEqualTo('.')
        assertThat(decimalSeparatorOf("2")).isEqualTo('.')
        assertThat(decimalSeparatorOf("1 5")).isEqualTo('.')
        assertThat(decimalSeparatorOf("1-5")).isEqualTo('.')
    }

    /** Money in cents shown as `12.34`; a delegate written against the dot-only contract. */
    private object LongCents : NumberFilterDelegate<Long> {
        override val regex: Regex = Regex("^-?\\d*\\.?\\d{0,2}$")
        override val default: Long = 0

        override fun parse(input: String): Long? {
            val whole = input.substringBefore('.')
            val cents = input.substringAfter('.', "").padEnd(2, '0')
            return (whole + cents).toLongOrNull()
        }

        override fun format(value: Long): String = "${value / 100}.${(value % 100).toString().padStart(2, '0')}"

        override fun toSliderValue(value: Long): Float = value.toFloat()

        override fun fromSliderValue(value: Float): Long = value.toLong()

        override fun compare(
            a: Long,
            b: Long,
        ): Boolean = a <= b
    }
}
