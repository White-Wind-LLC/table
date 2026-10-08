package ua.wwind.table.platform

import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT
import ua.wwind.table.filter.component.main.date.toDatePickerMillis

private const val MILLIS_PER_SECOND = 1000.0
private const val MAX_FRACTION_DIGITS = 20uL

// Long.MIN_VALUE has 19 digits.
private const val MAX_LONG_DIGITS = 19uL

internal actual fun platformFormatDate(
    date: LocalDate,
    languageTag: String?,
): String {
    val locale = languageTag.toNSLocale()
    val formatter =
        NSDateFormatter().apply {
            this.locale = locale
            // The date picker is Gregorian; some locales default to another calendar (th: Buddhist).
            calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
            timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
            dateFormat = NSDateFormatter.dateFormatFromTemplate("yMMdd", 0u, locale) ?: "dd.MM.yyyy"
        }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(date.toDatePickerMillis() / MILLIS_PER_SECOND))
}

internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String {
    val formatter =
        NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterDecimalStyle
            locale = languageTag.toNSLocale()
            maximumFractionDigits = MAX_FRACTION_DIGITS
        }
    val number =
        when (value) {
            is Int, is Long, is Short, is Byte -> {
                // By default the formatter rounds to double precision (2^53 + 1 -> ...990).
                formatter.usesSignificantDigits = true
                formatter.maximumSignificantDigits = MAX_LONG_DIGITS
                NSDecimalNumber(string = value.toString())
            }

            else -> {
                NSNumber(double = value.toDouble())
            }
        }
    return formatter.stringFromNumber(number) ?: value.toString()
}

private fun String?.toNSLocale(): NSLocale = this?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
