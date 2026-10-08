package ua.wwind.table.platform

import kotlinx.datetime.LocalDate

/** Formats [date] as the numeric date of [languageTag] (BCP 47), with a four-digit year. */
internal fun formatLocalizedDate(
    date: LocalDate,
    languageTag: String,
): String = platformFormatDate(date, languageTag.orDefaultLocale())

/** Formats [value] with the grouping and decimal separator of [languageTag] (BCP 47). */
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

internal expect fun platformFormatDate(
    date: LocalDate,
    languageTag: String?,
): String

internal expect fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String
