# Large font scale — design

Issue: [#135](https://github.com/White-Wind-LLC/table/issues/135) (part of
[#84](https://github.com/White-Wind-LLC/table/issues/84))

## Goal

At 1.5× to 2× system font scale, single-line text in the header, body rows, footer, group headers
and the fast filters row is no longer clipped. At 1.0× and below, every height stays
pixel-identical to today.

Today every height except a body row in `RowHeightMode.Dynamic` is a fixed `Dp` from
`TableDimensions` (header, footer, inline and sticky group headers), or a private constant (the
fast filters row, `40.dp`). Children fill that height with `fillMaxHeight()`, so text that grows
with the font scale is cut off. The dynamic row height cache `TableState.rowHeightsPx` also
survives a font scale or density change, so offscreen entries go stale.

## Decisions taken during brainstorming

- Heights **grow by the text delta**: each height gains exactly as much as one line of its text
  style grew compared with 1.0×. Proportional scaling (`height × fontScale`) overgrows; measuring
  content (`heightIn(min = …)` plus measured overlay and padding maths) is a much larger change and
  is out of scope.
- The adjustment is applied **inside the table**, to any `TableDimensions` (defaults or custom),
  with an opt-out flag `scaleWithFontSize`. Existing callers get the fix without code changes.
- Heights stay known values, so the sticky group overlay and footer padding maths keep their
  current shape; they only read the resolved dimensions.
- Dynamic body rows are unchanged; they already size to their content.
- The sample gets a "Large font (2×)" toggle rather than a separate screen, since the sample has
  no navigation.
- One branch and one PR.

## 1. Public API

### `TableDimensions` (`config/TableDimensions.kt`)

Two new defaulted fields, appended after the existing ones so positional and named calls keep
compiling:

```kotlin
/** Height of the fast filters row under the header. */
val fastFilterRowHeight: Dp = 40.dp,
/**
 * Grow [rowHeight], [headerHeight], [footerHeight] and [fastFilterRowHeight] by as much as one
 * line of their text grows above 1.0× font scale, so text is not clipped. Set to false to keep the
 * heights exactly as given.
 */
val scaleWithFontSize: Boolean = true,
```

`init` adds `require(fastFilterRowHeight >= 0.dp)`. `TableDefaults.standardDimensions()` and
`compactDimensions()` keep their values and take the new defaults.

No other public API changes. `TableState.dimensions` keeps returning the caller's dimensions as
given.

## 2. Resolving heights

New internal file `config/FontScaledDimensions.kt`:

```kotlin
internal fun TableDimensions.scaledForFont(typography: TableTypography, density: Density): TableDimensions
```

- Returns `this` unchanged when `!scaleWithFontSize`, or when no delta below is positive. This
  covers `fontScale <= 1f`, so 1.0× is pixel-identical.
- Per text style:
  `delta(style) = max(0.dp, lineHeightSp.toDp() − lineHeightSp.value.dp)`, where
  `lineHeightSp` is `style.lineHeight`, falling back to `style.fontSize × 1.2` when the line
  height is unspecified, and to no growth when both are unspecified (or not in `sp`).
  Converting through `Density.toDp()` instead of multiplying by `fontScale` keeps it correct
  under Android 14's non-linear font scaling.
- Growth per height:

  | Height                | Grows by                                             |
  |-----------------------|------------------------------------------------------|
  | `rowHeight`           | `max(delta(body), delta(groupHeader))`               |
  | `headerHeight`        | `delta(header)`                                      |
  | `footerHeight`        | `delta(footer)`                                      |
  | `fastFilterRowHeight` | `delta(body)` (the filter text fields use body text) |

  Group headers (inline and sticky) keep using `rowHeight`, which is why it takes the larger of
  the body and group header deltas.

## 3. Wiring

- `TableState` gets `internal var effectiveDimensions: TableDimensions by mutableStateOf(dimensions)`.
- In `Table.kt`, the root replaces `val dimensions = state.dimensions` with
  `remember(state.dimensions, typography, LocalTextStyle.current, density.density, density.fontScale) { state.dimensions.scaledForFont(typography.mergedOver(LocalTextStyle.current), density) }`.
  Each style is merged over the ambient `LocalTextStyle` first, as the bands render it, so a style
  that only sets weight or colour still grows by the size it inherits.
  and assigns it to `state.effectiveDimensions`, in the same way the root already assigns
  `state.visibleColumns` and `state.rowUnits`.
- Every internal height read switches from `state.dimensions` to `state.effectiveDimensions` (or
  receives the resolved `dimensions` from the root):
  - `Table.kt` pinned footer padding (`:281`) and `BodyOverlay` (`:731`), plus the `dimensions`
    handed to the footer overlay and rows;
  - `component/TableHeader.kt:118` header height;
  - `component/footer/TableFooterRow.kt:36,59`;
  - `component/body/TableRowItem.kt:218,305` (placeholder and fixed row height);
  - `component/body/TableBody.kt:491` inline group header;
  - `component/body/GroupStickyOverlay.kt:61-62,128`;
  - `interaction/ViewportUtils.kt:152` `estimatedRowHeight`;
  - `filter/component/fast/FastFiltersRow.kt:66`, which now uses
    `effectiveDimensions.fastFilterRowHeight`; the private `FAST_FILTER_ROW_HEIGHT` constant is
    removed.
- Readers of non-height fields (divider thickness, paddings, icon sizes) may read either; for
  consistency every internal `state.dimensions` read moves to `effectiveDimensions`.

## 4. Row height cache

`Table.kt:244` `LaunchedEffect(itemsCount) { state.rowHeightsPx.clear() }` becomes keyed on
`itemsCount`, `density.density`, `density.fontScale` and `state.effectiveDimensions`. Visible rows
re-report through `onGloballyPositioned`; `TableViewportPrefetcher` refills the rest, since it only
skips indices that are cached.

## 5. Sample

- `SampleTableConfig` gets `largeFont: Boolean = false`.
- `SettingsSidebar` Appearance section gets a "Large font (2×)" `SettingSwitch`.
- `SampleApp` wraps the table next to the RTL provider:
  `CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = if (largeFont) 2f else density.fontScale))`.

## 6. Tests (commonTest, `runComposeUiTest`)

- `FontScaledDimensionsTest` (unit):
  - unchanged at 1.0× and at 0.85×;
  - unchanged with `scaleWithFontSize = false`;
  - at 2× each height grows by its style's line height (e.g. 20.sp → +20.dp);
  - unspecified line height falls back to `fontSize × 1.2`; both unspecified → no growth;
  - `rowHeight` takes the larger of the body and group header deltas.
- UI tests with `LocalDensity` overridden to `fontScale = 2f`:
  - header, footer, inline group header and fast filters row are taller than their 1.0× heights
    by the expected delta;
  - the body overlay and pinned footer still line up with the measured header and footer;
  - the sticky group overlay height matches the inline group header;
  - with `scaleWithFontSize = false` the heights stay as given;
  - changing the font scale clears `rowHeightsPx` in Dynamic mode.
- `TableDimensionsTest`: the new `require` and defaults.
- Existing tests run at 1.0× and must pass unchanged.

## 7. Docs and changelog

- `CHANGELOG.md` Unreleased:
  `- Added: row, header, footer, group header and fast filter heights grow with the system font scale; TableDimensions.scaleWithFontSize opts out, TableDimensions.fastFilterRowHeight sets the fast filters row height ([#135](https://github.com/White-Wind-LLC/table/issues/135))`.
- `docs/content/guides/row-height-auto-width.md`: a short "Large font scale" section.
- `docs/content/reference/core-api.md:106`: replace the stale `defaultRowHeight` /
  `checkBoxColumnWidth` mention with the actual `TableDimensions` fields, including the two new
  ones.

## Out of scope

- Multi-line or custom header, footer and group header content taller than one line of text: it
  still clips in fixed areas. That needs content measurement (follow-up if needed).
- A minimum height for Dynamic body rows.
