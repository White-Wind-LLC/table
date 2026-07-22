# TableState delegates: columns, selection, editing

Closes the `TooManyFunctions` detekt debt (#37) by splitting the responsibilities the rule was
pointing at, rather than suppressing the finding.

## Why

`TableState` carried 25 functions against a limit of 11. The count was a symptom: column sizing and
ordering, row and cell selection, and row editing had accumulated in one holder alongside what a
table is actually asked for — sort, group, filter. `RowBlocksState` (16) and `SampleViewModel` (25)
were flagged by the same rule for a different reason: both are mostly *private* helpers, extracted
in 86acb38 to satisfy `CyclomaticComplexMethod` and `LongMethod`.

2.0.0 declared the API stable one day earlier, so the split cannot break call sites. The deprecation
cycle the release already established for `@ExperimentalTableApi` — kept one release, deprecated,
removed in the next major — is reused here.

## Locked decisions

1. **Three holders, not four.** `columns`, `selection`, `editing`. Sort, grouping and filtering stay
   on `TableState`: they describe the table as a whole, they are its most-used API, and moving them
   would churn the most call sites for the least structural gain. The issue named the same three.
2. **State moves with behaviour.** A holder that owned functions but left its state behind would be
   a function bag. `columns` owns the order, the width overrides and the auto-fit measurements;
   `selection` owns the focused row, the checked rows and the selected cell; `editing` owns the
   edited row, the edited column and the edit callbacks.
3. **Editing depends on selection, never the reverse.** Starting an edit selects the cell it starts
   in, and a refused completion re-selects the cell the consumer must return to. One-way, so the two
   holders cannot loop.
4. **Cross-holder coordination stays on `TableState`.** `selectCellUnchecked` (selection + editing)
   and `remapRowPositions` (block drag remap) are composition-root concerns. Both are `internal`.
5. **`TableState.SelectedCell` does not move.** A nested type cannot be re-exported under a new name
   the way a function can, so relocating it would break every consumer that names the type — with no
   deprecation path available. It stays nested and the holders reference it.
6. **Renames only where the holder makes the prefix redundant.** `columnOrder` → `columns.order`,
   `setColumnWidthToMaxContent` → `columns.fitToContent`, `startEditing` → `editing.start`.
   `checkedIndices`, `focusRow`, `toggleCheck` and the callbacks keep their names. The one judgement
   call is `toggleSelect` → `selection.toggleRow`: inside the holder, "select" said nothing, and
   `toggleRow` next to `toggleCheck` names the pair by what each toggles.

## Deprecation contract

All 31 members the holders took over stay on `TableState` as forwarders with
`@Deprecated(..., ReplaceWith(...))` at `WARNING` level, so consumers compile unchanged and the IDE
rewrites call sites. They are removed in the next major. Properties whose setter was already
`private` become read-only forwarders — no consumer could write them.

Every call site inside the repository (16 files in `table-core`, 3 tests, `SampleApp`) is migrated,
so the library never uses its own deprecated surface: a deprecation warning in a build of this repo
means something was missed.

## Detekt configuration

`TooManyFunctions` stops counting two categories, with the reasoning recorded in
`config/detekt/detekt.yml`:

- `ignorePrivate: true` — a private helper exists because `CyclomaticComplexMethod` or `LongMethod`
  asked for it; counting it makes the rules contradict each other. Class bulk is still guarded by
  `LargeClass`. This is what clears `RowBlocksState` (9 non-private functions).
- `ignoreDeprecated: true` — a deprecated member is a forwarder on its way out, carrying its own
  migration. Counting it penalises the deprecation cycle that keeps consumers compiling.

`SampleViewModel` needs neither: it gives up `matchesPerson` (a pure forwarder to
`PersonFilterMatcher`) and `buildFormatFilterData` (now `PersonFormatFilterData.build`). Both were
stateless, so they belong with the other `Person*` helpers, not on a state holder.

## Verification

- `./gradlew qualityCheck` green with the three `TooManyFunctions` entries removed from the
  baselines.
- Flipping either knob back to `false` reports the classes again — the rule still has teeth, and the
  numbers it reports are the ones this document claims.
- `./gradlew :table-core:jvmTest` — 157 tests, including `TableStateRemapTest` and
  `RowBlocksTableTest`, which exercise selection, editing and the block-drag remap through the new
  holders.
- Compilation of `table-core` across JVM, JS, wasmJs, iosSimulatorArm64 and metadata, plus the
  `table-sample` JVM target.
