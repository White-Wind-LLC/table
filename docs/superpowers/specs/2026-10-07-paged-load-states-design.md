# Paged table load states — design

Issue: [#89](https://github.com/White-Wind-LLC/table/issues/89)

## Goal

The paged `Table` (`table-paging`) shows built-in UI for every `LoadState` it receives: a loading
body before any data exists, an error body with **Retry**, the existing empty state, a subtle
progress indicator while rows load behind data already on screen, and an inline error bar with
**Retry** when such a load fails. All text is localized through `StringProvider`. The sample app
demonstrates every state on a real `Pager`.

Today `handleLoadState` (`LoadStateHandler.kt`) is never called by the table, only logs errors,
offers no retry and hardcodes "No data available"; the sample has no paging screen.

## Decisions taken during brainstorming

- **Positional mapping.** `ua.wwind.paging` is not AndroidX Paging: `LoadState` is one global
  `Loading | Error(throwable, key) | Success`, `PagingMap.size` is the full total, unloaded rows
  are placeholders, and an error can sit at any key. There is no refresh/append split, so there
  is **no append footer row**. Placeholders already show where rows are loading; the issue's
  "append" and "refresh" indicators collapse into one progress indicator and one error bar.
- **Core `bodyOverlay` slot.** The core table owns the geometry of the visible rows area, so it
  gets a generic overlay slot; the paging module feeds its indicator and error bar into it.
- **paging-core fix.** `Pager.flow` currently emits `size = 0, Success` before its first load,
  which would flash "No data". `Pager` regains an initial `Loading` state in paging-kmp (lost in
  refactor `21418cb`, present in 2.2.5). The table does not depend on that release: it already
  treats `null` and `size == 0 && Loading` as loading.
- Every change is additive; existing callers keep compiling. The new `UiString` keys break only a
  custom `StringProvider` with an exhaustive `when`, as #88 did.

## 1. State mapping (`table-paging`)

A pure internal function maps a snapshot to what the table shows:

```kotlin
internal sealed interface PagedBodyState {
    data object Loading : PagedBodyState                       // body: loadingContent
    data class Failed(val error: LoadState.Error) : PagedBodyState  // body: errorContent
    data object Empty : PagedBodyState                         // body: emptyContent
    data class Rows(val loading: Boolean, val error: LoadState.Error?) : PagedBodyState
}

internal fun pagedBodyState(items: PagingData<*>?): PagedBodyState
```

| `items`                 | `size == 0` | `size > 0`                                   |
|-------------------------|-------------|----------------------------------------------|
| `null`                  | `Loading`   | —                                            |
| `loadState = Loading`   | `Loading`   | `Rows(loading = true, error = null)`         |
| `loadState = Error`     | `Failed`    | `Rows(loading = false, error = it)`          |
| `loadState = Success`   | `Empty`     | `Rows(loading = false, error = null)`        |

## 2. Public API (`table-paging`)

Both paged `Table` overloads gain, next to the existing `emptyContent`:

```kotlin
loadingContent: @Composable () -> Unit = PagedTableDefaults.LoadingContent,
errorContent: @Composable PagedTableErrorScope.() -> Unit = PagedTableDefaults.ErrorContent,
loadingIndicator: (@Composable () -> Unit)? = PagedTableDefaults.LoadingIndicator,
errorBar: (@Composable PagedTableErrorScope.() -> Unit)? = PagedTableDefaults.ErrorBar,
```

- `loadingContent`, `errorContent` and the existing `emptyContent` are body states: they render
  only while the table has no rows (`Loading`, `Failed`, `Empty`). The paged table passes the one
  matching the current state as the core table's `emptyContent`, so they are centred in the
  visible body with header, fast filters and footer still shown — exactly like #88.
- `loadingIndicator` renders at the top of the visible body while `Rows.loading` has been true
  continuously for `LoadingIndicatorDelay` (400 ms, internal). The delay absorbs the short
  `Loading` pulses the global state produces on every scroll preload (paging-kmp #11). It hides
  immediately when loading stops. `null` disables it.
- `errorBar` renders at the bottom of the visible body while `Rows.error` is non-null. `null`
  disables it.
- `PagedTableErrorScope`:

  ```kotlin
  @Stable
  public interface PagedTableErrorScope {
      /** The failure reported by the pager. */
      public val error: Throwable
      /** Retries the failed load: `items.retry(error.key)`. */
      public fun retry()
  }
  ```

- Each distinct `LoadState.Error` is logged once through Kermit (`LaunchedEffect(error)`),
  preserving what `handleLoadState` did.
- `handleLoadState` is marked `@Deprecated` with a message pointing to the paged `Table` slots.
  It is not removed.

### `PagedTableDefaults` (public object, `table-paging`)

- `LoadingContent`: centred `CircularProgressIndicator`, semantics `contentDescription` =
  `UiString.PagingLoading`.
- `ErrorContent`: error icon, `UiString.PagingLoadError` text, `TextButton` with
  `UiString.PagingRetry` calling `retry()`. Layout mirrors `DefaultTableEmptyContent` (icon 32 dp,
  `onSurfaceVariant`, 16/32 dp padding, 8 dp spacing). The raw exception message is not shown: it
  is neither user-facing nor localizable; a custom slot can read `error`.
- `LoadingIndicator`: full-width `LinearProgressIndicator`.
- `ErrorBar`: full-width `Surface` in `errorContainer` / `onErrorContainer`, one row: error icon,
  `UiString.PagingLoadMoreError`, `TextButton` with `UiString.PagingRetry`.

Icons come from core `TableIcons` (the project has no material-icons dependency); an error icon
is added there if none exists.

## 3. Core `bodyOverlay` slot (`table-core`)

- `Table` (both overloads) and `EditableTable` gain
  `bodyOverlay: @Composable BoxScope.() -> Unit = {}`, documented as content drawn over the
  visible rows area.
- `EmptyStateOverlay` becomes `BodyOverlay`: one box covering the visible rows area — below the
  header (`headerBottomPx`), above a pinned footer, with the same footer rules as today — that
  does not move with horizontal scroll. It always draws `bodyOverlay` (aligned by the caller via
  `BoxScope`), and additionally the centred empty content when `itemsCount == 0`.
- Embedded tables keep rendering the empty state inline; `bodyOverlay` is drawn over the same
  region for them too (the surface box minus the header).

## 4. Strings (`table-core`)

New `UiString` keys with `DefaultStrings` values:

| Key                   | Default                    |
|-----------------------|----------------------------|
| `PagingLoading`       | `Loading`                  |
| `PagingLoadError`     | `Couldn't load data`       |
| `PagingLoadMoreError` | `Couldn't load some rows`  |
| `PagingRetry`         | `Retry`                    |

## 5. paging-core fix (paging-kmp, separate PR)

`Pager`'s `loadState` starts as `LoadState.Loading` and the `onStart` seed emits `Loading`, so the
first snapshot is `size = 0, Loading`, matching `StreamingPager`. A test asserts the first
emission. After it is released (2.3.4, published by the maintainer), `paging` is bumped in
`gradle/libs.versions.toml` — in this PR if released by then, otherwise as a follow-up.

## 6. Sample (`table-sample`)

- `table-sample` depends on `table-paging`.
- A "Paging" screen, selectable from the app toolbar, shows a paged `Table` of demo `Person` rows
  backed by a real `Pager` whose `readData` adds simulated latency.
- Controls: "Fail loads" toggle (the source throws), "Empty dataset" toggle, and **Refresh**
  (`pager.refresh()`). Together they reach loading, error, empty, progress indicator and error bar.
- The screen collects `pager.flow` with an initial `null`.

## 7. Testing

- Unit tests for `pagedBodyState` covering every cell of the table in section 1.
- Compose UI tests in `table-paging` (`runComposeUiTest`):
  - `null` and `size 0 / Loading` show the loading content and not "No data".
  - `size 0 / Error` shows the error content; **Retry** calls `retry(key)` with the error's key.
  - `size 0 / Success` shows "No data" (existing test kept).
  - `size > 0 / Loading`: indicator absent before 400 ms, present after (`mainClock`), gone when
    the state turns `Success`.
  - `size > 0 / Error`: error bar visible over rows; **Retry** calls `retry(key)`.
  - `loadingIndicator = null` / `errorBar = null` render nothing.
- Core test: `bodyOverlay` content is placed below the header and above a pinned footer, and stays
  put under horizontal scroll.
- Verification: JVM tests for `table-core` and `table-paging`, plus an iOS compile.

## 8. Docs

- `docs/content/modules/table-paging.md`: load states section with the slot table and the
  "collect with an initial `null`" note.
- `docs/content/reference/core-api.md`: `bodyOverlay`.
- `CHANGELOG.md`: one short line linking #89, noting the new `UiString` keys.

## Out of scope

- Per-range load states (the pager reports only one global state; paging-kmp #11).
- Error rows in place of the failed placeholders.
- Skeleton rows beyond the existing `placeholderRow`.
