package ua.wwind.table.strings

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.LocalDate
import ua.wwind.table.platform.formatLocalizedDate
import ua.wwind.table.platform.formatLocalizedNumber

/**
 * The current locale's numeric date with a four-digit year, e.g. `08.10.2026` (uk, de) or
 * `10/08/2026` (en-US). Default of [StringProvider.formatDate].
 */
@Composable
public fun defaultFormatDate(date: LocalDate): String = formatLocalizedDate(date, Locale.current.toLanguageTag())

/**
 * [value] with the current locale's grouping and decimal separator, e.g. `1 234,5` (uk) or
 * `1,234.5` (en-US). Default of [StringProvider.formatNumber].
 */
@Composable
public fun defaultFormatNumber(value: Number): String = formatLocalizedNumber(value, Locale.current.toLanguageTag())
