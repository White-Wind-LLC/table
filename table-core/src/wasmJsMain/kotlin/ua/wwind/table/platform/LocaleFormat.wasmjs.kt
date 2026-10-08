package ua.wwind.table.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

internal actual fun platformFormatDate(
    date: LocalDate,
    languageTag: String?,
): String = intlFormatDate(date.year, date.month.number, date.day, languageTag)

internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String =
    // Intl reads a decimal string exactly; toDouble() would round Longs beyond 2^53.
    intlFormatNumber(value.toString(), languageTag)

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun intlFormatDate(
    year: Int,
    month: Int,
    day: Int,
    languageTag: String?,
): String =
    js(
        """(() => {
            const d = new Date(0);
            d.setUTCFullYear(year, month - 1, day);
            return new Intl.DateTimeFormat(languageTag || undefined,
                { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'UTC', calendar: 'gregory' }).format(d);
        })()""",
    )

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun intlFormatNumber(
    value: String,
    languageTag: String?,
): String = js("new Intl.NumberFormat(languageTag || undefined, { maximumFractionDigits: 20 }).format(value)")
