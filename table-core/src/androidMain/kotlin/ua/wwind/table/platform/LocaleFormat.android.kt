package ua.wwind.table.platform

import android.text.format.DateFormat
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.component.main.date.toDatePickerMillis
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// DecimalFormat's own ceiling for double fraction digits.
private const val MAX_FRACTION_DIGITS = 340

internal actual fun platformFormatDate(
    date: LocalDate,
    languageTag: String?,
): String {
    val locale = languageTag.toLocale()
    val pattern = DateFormat.getBestDateTimePattern(locale, "yMMdd")
    val format = SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return format.format(Date(date.toDatePickerMillis()))
}

internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String =
    NumberFormat
        .getNumberInstance(languageTag.toLocale())
        .apply { maximumFractionDigits = MAX_FRACTION_DIGITS }
        .format(value)

private fun String?.toLocale(): Locale = this?.let(Locale::forLanguageTag) ?: Locale.getDefault(Locale.Category.FORMAT)
