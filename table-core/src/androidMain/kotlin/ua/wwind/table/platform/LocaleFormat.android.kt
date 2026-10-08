package ua.wwind.table.platform

import android.text.format.DateFormat
import java.text.NumberFormat
import java.util.Locale

// DecimalFormat's own ceiling for double fraction digits.
private const val MAX_FRACTION_DIGITS = 340

private val datePatterns =
    LocaleCache { tag -> DatePattern.parse(DateFormat.getBestDateTimePattern(Locale.forLanguageTag(tag), "yMMdd")) }

private val numberFormats =
    LocaleCache { tag ->
        NumberFormat
            .getNumberInstance(Locale.forLanguageTag(tag).withLatinDigits())
            .apply { maximumFractionDigits = MAX_FRACTION_DIGITS }
    }

internal actual fun platformDatePattern(languageTag: String?): DatePattern = datePatterns[languageTag.orDefaultTag()]

internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String {
    val format = numberFormats[languageTag.orDefaultTag()]
    // NumberFormat is not thread-safe.
    return synchronized(format) { format.format(value) }
}

private fun String?.orDefaultTag(): String = this ?: Locale.getDefault(Locale.Category.FORMAT).toLanguageTag()

// `nu-latn` also switches the separators to the Latin ones (ar: 1,234.5, not 1٬234٫5).
private fun Locale.withLatinDigits(): Locale =
    Locale
        .Builder()
        .setLocale(this)
        .setUnicodeLocaleKeyword("nu", "latn")
        .build()
