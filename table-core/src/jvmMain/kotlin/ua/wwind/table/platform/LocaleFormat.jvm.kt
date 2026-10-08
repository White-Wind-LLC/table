package ua.wwind.table.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.text.NumberFormat
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

// DecimalFormat's own ceiling for double fraction digits.
private const val MAX_FRACTION_DIGITS = 340

internal actual fun platformFormatDate(
    date: LocalDate,
    languageTag: String?,
): String {
    val locale = languageTag.toLocale()
    val shortPattern =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale)
    // SHORT has a two-digit year (M/d/yy); keep the locale's order and separators, widen the fields.
    val pattern =
        shortPattern
            .replace(Regex("y+"), "yyyy")
            .replace(Regex("M+"), "MM")
            .replace(Regex("d+"), "dd")
    return DateTimeFormatter.ofPattern(pattern, locale).format(date.toJavaLocalDate())
}

internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String =
    NumberFormat
        .getNumberInstance(languageTag.toLocale())
        .apply { maximumFractionDigits = MAX_FRACTION_DIGITS }
        .format(value)

private fun String?.toLocale(): Locale = this?.let(Locale::forLanguageTag) ?: Locale.getDefault()
