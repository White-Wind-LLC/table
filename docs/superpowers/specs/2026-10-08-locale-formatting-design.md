# Locale-aware date and number formatting — design

Issue: [#106](https://github.com/White-Wind-LLC/table/issues/106) (part of #90)

## Goal

Dates and numbers shown by the filter UI follow the user's locale: the date field of the date
filters (panel and fast row) and the text of active-filter chips. By default the table formats
with the platform locale; a custom `StringProvider` can override both formats.

Today the date field is fixed to `dd.MM.yyyy` (`LocalDate.toFormatString()` in `DateFilter.kt`),
date chips use ISO `LocalDate.toString()` and number chips use raw `Number.toString()`
(`TableActiveFilters.kt`).

## Decisions taken during brainstorming

- **Platform locale by default.** The defaults format with the current locale, through an
  `expect`/`actual` layer per target (JVM, Android, iOS, JS, Wasm). `kotlinx-datetime` has no
  locale-aware formatting. The visible output changes, e.g. en-US dates become `10/08/2026`.
- **Hook on `StringProvider`.** `StringProvider` is already the localization unit callers pass to
  `Table` and `TableActiveFilters`, so the formatter lives there as two methods with default
  bodies. No new parameters; existing implementations keep compiling. The repo has no binary
  compatibility validator, so source compatibility is the bar.
- **Number input fields stay as they are.** The field text is edited by the user and must parse
  with `NumberFilterDelegate.parse`; grouped or comma-decimal text (`1 234,5`) would break it.
  The fields keep `delegate.format`. Accepting a locale decimal separator while typing is out of
  scope.
- **Out of scope:** range chip text (`from – to`) and chip click behaviour belong to #107; date
  BETWEEN chips keep showing the first date until then, but formatted.

## 1. API (`strings/UiString.kt`)

```kotlin
public interface StringProvider {
    @Composable
    public fun get(key: UiString): String

    @Composable
    public fun formatDate(date: LocalDate): String = defaultFormatDate(date)

    @Composable
    public fun formatNumber(value: Number): String = defaultFormatNumber(value)
}
```

`defaultFormatDate` and `defaultFormatNumber` are public `@Composable` top-level functions in
`ua.wwind.table.strings`, so a custom provider can override one format and delegate the other.
They read the language tag from `androidx.compose.ui.text.intl.Locale.current.toLanguageTag()`,
the locale Compose itself uses, and call the platform layer.

## 2. Platform layer (`platform/LocaleFormat.kt`)

```kotlin
internal expect fun platformFormatDate(date: LocalDate, languageTag: String?): String
internal expect fun platformFormatNumber(value: Number, languageTag: String?): String
```

`null` means the platform default locale. Common wrappers `formatLocalizedDate` /
`formatLocalizedNumber(…, languageTag: String)` hold the rules shared by every target: an empty or
`und` tag becomes `null`, non-finite values use `toString()`, and a `Float` is passed as the
`Double` of its decimal text so `0.1f` does not show as `0.10000000149011612`.

**Date** — the locale's numeric date with a four-digit year and two-digit month and day (the
`yMMdd` skeleton): uk-UA and de-DE give `08.10.2026`, en-US gives `10/08/2026`. The plain
SHORT style is not used because it has a two-digit year on JVM and Android (`10/8/26`).

| Target | Implementation |
|---|---|
| Android | `android.text.format.DateFormat.getBestDateTimePattern(locale, "yMMdd")` with `SimpleDateFormat` |
| JVM | `DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale)`, widened to four-digit year and two-digit month and day, formatted with `java.time` |
| iOS | `NSDateFormatter.dateFormatFromTemplate("yMMdd", 0u, locale)` |
| JS, Wasm | `Intl.DateTimeFormat(tag, { year: "numeric", month: "2-digit", day: "2-digit" })` |

A date is turned into a platform date at UTC midnight and formatted in UTC, so no time zone can
shift the day (the same reason `toDatePickerMillis` uses UTC).

**Number** — the locale's decimal format with grouping: uk-UA gives `1 234,5`, en-US gives
`1,234.5`. Fraction digits are not cut (the platform defaults cap them at three), so `0.1234`
stays `0.1234`. `NaN` and infinities fall back to `toString()`.

| Target | Implementation |
|---|---|
| JVM, Android | `java.text.NumberFormat.getNumberInstance(locale)` with a large `maximumFractionDigits` |
| iOS | `NSNumberFormatter` with `NSNumberFormatterDecimalStyle` and a large `maximumFractionDigits` |
| JS, Wasm | `Intl.NumberFormat(tag, { maximumFractionDigits: 20 })` |


## 3. Call sites

- `DateField` (`DateFilter.kt`) shows `strings.formatDate(value)`. It already takes `strings`,
  and the fast date filter uses the same composable, so both pick it up.
  `LocalDate.toFormatString()` is removed.
- `buildFilterChipTextUnsafe` (`TableActiveFilters.kt`) formats number values (single and
  BETWEEN) with `strings.formatNumber` and the date value with `strings.formatDate`.

## 4. Testing

- `jvmTest`: `formatLocalizedDate` and `formatLocalizedNumber` for `en-US`, `uk-UA` and `de-DE`,
  including Int, Long, Float and Double values, fraction digits beyond three, negative numbers,
  a date before 1970, NaN and infinities, and empty / `und` tags.
- `commonTest` (Compose UI test): a `StringProvider` that overrides `formatDate` and
  `formatNumber` with marker output; the active-filter chips for a number and a date filter and
  the `DateField` text show the marker. This proves the hook is used without depending on the
  test machine's locale.
- Verification: `qualityCheck` plus compile tasks for every target (JVM, Android, iOS, JS, Wasm).

## 5. Docs

- CHANGELOG entry under Unreleased: `StringProvider.formatDate` / `formatNumber`, locale-aware
  defaults, and the visible change in date format.
- `docs/content/modules/table-core.md`, where `StringProvider` is described, gains a short example
  of overriding the formats.
