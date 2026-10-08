package ua.wwind.table.platform

// The default locale is keyed as "": a page's Intl default does not change while it runs.
private val datePatterns = LocaleCache { tag -> DatePattern.parse(intlDatePattern(tag.ifEmpty { null })) }

@OptIn(ExperimentalWasmJsInterop::class)
private val numberFormats = LocaleCache { tag -> intlNumberFormat(tag.ifEmpty { null }) }

internal actual fun platformDatePattern(languageTag: String?): DatePattern = datePatterns[languageTag.orEmpty()]

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformFormatNumber(
    value: Number,
    languageTag: String?,
): String =
    // Intl reads a decimal string exactly; toDouble() would round Longs beyond 2^53.
    intlFormat(numberFormats[languageTag.orEmpty()], value.toString())

// The locale's numeric date as a CLDR pattern: its field order and literal separators.
@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun intlDatePattern(languageTag: String?): String =
    js(
        """new Intl.DateTimeFormat(languageTag || undefined,
            { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'UTC', calendar: 'gregory' })
            .formatToParts(new Date(0))
            .map(p => p.type === 'year' ? 'y' : p.type === 'month' ? 'MM' : p.type === 'day' ? 'dd' :
                p.type === 'literal' ? "'" + p.value.replace(/'/g, "''") + "'" : '')
            .join('')""",
    )

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun intlNumberFormat(languageTag: String?): JsAny =
    js("new Intl.NumberFormat(languageTag || undefined, { maximumFractionDigits: 20, numberingSystem: 'latn' })")

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun intlFormat(
    format: JsAny,
    value: String,
): String = js("format.format(value)")
