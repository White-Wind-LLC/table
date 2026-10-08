package ua.wwind.table.platform

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.componentsFromLocaleIdentifier
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.localeIdentifierFromComponents

private const val MAX_FRACTION_DIGITS = 20uL

// Long.MIN_VALUE has 19 digits.
private const val MAX_LONG_DIGITS = 19uL

private val datePatterns =
    LocaleCache { id ->
        // A Buddhist (th) or Japanese calendar locale may add an era to the pattern.
        val locale = NSLocale(localeIdentifier = id).withKeyword("calendar", "gregorian")
        val pattern = NSDateFormatter.dateFormatFromTemplate("yMMdd", 0u, locale)
        DatePattern.parse(pattern ?: "dd.MM.y")
    }

// NSNumberFormatter is thread-safe once configured; one for integers and one for fractions.
private val numberFormatters =
    LocaleCache { id ->
        // `numbers=latn` also switches the separators to the Latin ones (ar: 1,234.5, not 1٬234٫5).
        val locale = NSLocale(localeIdentifier = id).withKeyword("numbers", "latn")
        NumberFormatters(
            integer =
                decimalFormatter(locale).apply {
                    // By default the formatter rounds to double precision (2^53 + 1 -> ...990).
                    usesSignificantDigits = true
                    maximumSignificantDigits = MAX_LONG_DIGITS
                },
            fraction = decimalFormatter(locale).apply { maximumFractionDigits = MAX_FRACTION_DIGITS },
        )
    }

private class NumberFormatters(
    val integer: NSNumberFormatter,
    val fraction: NSNumberFormatter,
)

internal actual fun platformDatePattern(languageTag: String?): DatePattern =
    datePatterns[languageTag.orCurrentLocaleId()]

internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String {
    val formatters = numberFormatters[languageTag.orCurrentLocaleId()]
    val formatted =
        when (value) {
            is Int, is Long, is Short, is Byte -> {
                formatters.integer.stringFromNumber(NSDecimalNumber(string = value.toString()))
            }

            else -> {
                formatters.fraction.stringFromNumber(NSNumber(double = value.toDouble()))
            }
        }
    return formatted ?: value.toString()
}

private fun decimalFormatter(locale: NSLocale): NSNumberFormatter =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        this.locale = locale
    }

private fun String?.orCurrentLocaleId(): String = this ?: NSLocale.currentLocale.localeIdentifier

private fun NSLocale.withKeyword(
    key: String,
    value: String,
): NSLocale {
    val components = NSLocale.componentsFromLocaleIdentifier(localeIdentifier).toMutableMap()
    components[key] = value
    return NSLocale(localeIdentifier = NSLocale.localeIdentifierFromComponents(components))
}
