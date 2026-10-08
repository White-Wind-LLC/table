package ua.wwind.table.platform

import kotlinx.datetime.LocalDate

/**
 * Formats [date] as the numeric date of [languageTag] (BCP 47): the locale's field order and
 * separators, a four-digit year and ASCII digits, in the proleptic Gregorian calendar.
 */
internal fun formatLocalizedDate(
    date: LocalDate,
    languageTag: String,
): String = platformDatePattern(languageTag.orDefaultLocale()).format(date)

/** Formats [value] with the grouping and decimal separator of [languageTag] (BCP 47) and ASCII digits. */
internal fun formatLocalizedNumber(
    value: Number,
    languageTag: String,
): String =
    when {
        value is Double && !value.isFinite() -> value.toString()

        value is Float && !value.isFinite() -> value.toString()

        // Float.toDouble() exposes binary noise (0.1f -> 0.10000000149011612); its decimal text does not.
        value is Float -> platformFormatNumber(value.toString().toDouble(), languageTag.orDefaultLocale())

        else -> platformFormatNumber(value, languageTag.orDefaultLocale())
    }

/** `null` means the platform default locale. */
private fun String.orDefaultLocale(): String? = takeUnless { it.isEmpty() || it == "und" }

/** The numeric date pattern (year, month, day) of [languageTag]; `null` is the default locale. */
internal expect fun platformDatePattern(languageTag: String?): DatePattern

internal expect fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String
