package ua.wwind.table.platform

// The default locale is keyed as "": a page's Intl default does not change while it runs.
private val datePatterns = LocaleCache { tag -> DatePattern.parse(intlDatePattern(tag.ifEmpty { null })) }

private val numberFormats = LocaleCache<Any> { tag -> intlNumberFormat(tag.ifEmpty { null }) }

internal actual fun platformDatePattern(languageTag: String?): DatePattern = datePatterns[languageTag.orEmpty()]

internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String =
    // Intl reads a decimal string exactly; toDouble() would round Longs beyond 2^53.
    intlFormat(numberFormats[languageTag.orEmpty()], value.toString())

// The locale's numeric date as a CLDR pattern: its field order and literal separators.
// The parentheses matter: Kotlin/JS's js() parser applies `new` to the whole call chain without them.
@Suppress("UNUSED_PARAMETER")
private fun intlDatePattern(languageTag: String?): String =
    js(
        """(new Intl.DateTimeFormat(languageTag || undefined,
            { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'UTC', calendar: 'gregory' }))
            .formatToParts(new Date(0))
            .map(p => p.type === 'year' ? 'y' : p.type === 'month' ? 'MM' : p.type === 'day' ? 'dd' :
                p.type === 'literal' ? "'" + p.value.replace(/'/g, "''") + "'" : '')
            .join('')""",
    )

@Suppress("UNUSED_PARAMETER")
private fun intlNumberFormat(languageTag: String?): Any =
    js("new Intl.NumberFormat(languageTag || undefined, { maximumFractionDigits: 20, numberingSystem: 'latn' })")

@Suppress("UNUSED_PARAMETER")
private fun intlFormat(
    format: Any,
    value: String,
): String = js("format.format(value)")
