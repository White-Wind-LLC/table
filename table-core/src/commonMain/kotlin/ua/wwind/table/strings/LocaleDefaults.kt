package ua.wwind.table.strings

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import ua.wwind.table.platform.formatLocalizedDate
import ua.wwind.table.platform.formatLocalizedNumber

// An empty tag selects the platform's format locale. Compose's Locale.current is the UI language
// only, which loses region formats set apart from it (macOS English with the Ukraine region).
private const val PLATFORM_FORMAT_LOCALE = ""

/**
 * The platform format locale's numeric date with a four-digit year, e.g. `08.10.2026` (uk, de) or
 * `10/08/2026` (en-US). Default of [StringProvider.formatDate].
 */
@Composable
public fun defaultFormatDate(date: LocalDate): String = formatLocalizedDate(date, PLATFORM_FORMAT_LOCALE)

/**
 * [value] with the platform format locale's grouping and decimal separator, e.g. `1 234,5` (uk) or
 * `1,234.5` (en-US). Default of [StringProvider.formatNumber].
 */
@Composable
public fun defaultFormatNumber(value: Number): String = formatLocalizedNumber(value, PLATFORM_FORMAT_LOCALE)
