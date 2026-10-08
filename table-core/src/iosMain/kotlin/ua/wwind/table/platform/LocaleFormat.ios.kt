package ua.wwind.table.platform

import kotlinx.datetime.LocalDate
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
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

internal actual fun platformFormatDate(
    date: LocalDate,
    languageTag: String?,
): String {
    val locale = languageTag.toNSLocale()
    val formatter =
        NSDateFormatter().apply {
            this.locale = locale
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
            is Double, is Float -> NSNumber(double = value.toDouble())
            else -> NSNumber(longLong = value.toLong())
        }
    return formatter.stringFromNumber(number) ?: value.toString()
}

private fun String?.toNSLocale(): NSLocale = this?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
