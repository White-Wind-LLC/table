package ua.wwind.table.filter.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.NumberFilterDelegate
import ua.wwind.table.strings.StringProvider

/**
 * Text of a number input field in the locale's decimal separator, on top of a [delegate] that
 * works with `.`. Input takes both the locale separator and `.` (pasted values, hardware keyboards);
 * grouping separators are rejected. Field text is shown with the locale separator, e.g. `1,5` in uk/de.
 *
 * @param delegate parses and formats text with `.` as the decimal separator
 * @param decimalSeparator the locale decimal separator, see [rememberNumberInputFormat]
 */
@Immutable
public class NumberInputFormat<T : Number>(
    public val delegate: NumberFilterDelegate<T>,
    public val decimalSeparator: Char = '.',
) {
    /** [text] with the locale separator replaced by `.`, as [delegate] expects it. */
    public fun normalize(text: String): String =
        if (decimalSeparator == '.') text else text.replace(decimalSeparator, '.')

    /** Whether [text] is valid (possibly partial) input for the field, checked by [NumberFilterDelegate.regex]. */
    public fun accepts(text: String): Boolean = normalize(text).matches(delegate.regex)

    /** The value of [text], or null when it doesn't parse. */
    public fun parse(text: String): T? = delegate.parse(normalize(text))

    /** [value] as field text, with the locale separator. */
    public fun format(value: T): String =
        delegate.format(value).let { if (decimalSeparator == '.') it else it.replace('.', decimalSeparator) }

    override fun equals(other: Any?): Boolean =
        other is NumberInputFormat<*> && delegate == other.delegate && decimalSeparator == other.decimalSeparator

    override fun hashCode(): Int = 31 * delegate.hashCode() + decimalSeparator.hashCode()
}

/**
 * A [NumberInputFormat] for [delegate] with the decimal separator of [StringProvider.formatNumber],
 * so number fields agree with the active-filter chips.
 */
@Composable
public fun <T : Number> rememberNumberInputFormat(
    delegate: NumberFilterDelegate<T>,
    strings: StringProvider,
): NumberInputFormat<T> {
    val sample = strings.formatNumber(DECIMAL_SAMPLE)
    return remember(delegate, sample) { NumberInputFormat(delegate, decimalSeparatorOf(sample)) }
}

private const val DECIMAL_SAMPLE = 1.5

// The character between the 1 and the 5 of a formatted 1.5; a minus or a space is never a decimal separator.
private val decimalSeparatorPattern = Regex("1([^\\d\\s-])5")

/** The decimal separator in [formatted], a formatted `1.5`; `.` when it can't be found. */
internal fun decimalSeparatorOf(formatted: String): Char =
    decimalSeparatorPattern
        .find(formatted)
        ?.groupValues
        ?.get(1)
        ?.single() ?: '.'
