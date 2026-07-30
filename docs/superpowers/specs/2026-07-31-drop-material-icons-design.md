# Drop the deprecated compose-material-icons dependency — Design

**Date:** 2026-07-31
**Status:** Approved
**Topic:** Remove `org.jetbrains.compose.material:material-icons-extended` from the project by
vendoring the icons the library actually draws.

## Problem

`material-icons-extended` is wired into the shared Compose convention plugin
(`build-logic/src/main/kotlin/ua/wwind/convention/compose.gradle.kts:20`) as an `implementation`
dependency, so every Compose module inherits it: `table-core`, `table-format`, `table-paging` and
`table-sample`.

The pinned version is **1.7.3** while the project is on Compose Multiplatform 1.11.1. The artifact is
deprecated upstream and frozen at the Compose 1.7 line — it receives no fixes and will not track
future Compose releases. It also ships roughly two thousand icons so that the library can draw
seventeen of them, which every consumer pays for on their runtime classpath.

## Goals

- No reference to `material-icons-extended` anywhere in the repository, including `table-sample`.
- Zero visual change: every icon renders exactly as it does today.
- The icons the library draws become part of its public API, so consumers can reference the same
  vectors that `TableHeaderDefaults` uses as defaults.

## Non-Goals

- Modernising the iconography to Material Symbols. That is a visual change; this work is a
  dependency removal and must be invisible on screen.
- Adding icons the library does not currently use.
- Any refactoring of the table, filter or format code beyond the import swap.

## Decisions (locked)

| Decision | Choice |
|---|---|
| Scope | All modules, `table-sample` included — the dependency leaves the repository entirely |
| Visibility | `public object TableIcons` in `table-core`; `internal object SampleIcons` in `table-sample` |
| Graphics source | Exact path data extracted from the `material-icons-extended` 1.7.3 artifact already in use |
| Extraction method | One-shot generator that walks `ImageVector` and emits Kotlin, deleted after use |
| Naming | Material names in PascalCase; style suffix only where two styles are both needed |
| Version bump | None — this commit adds a `### Unreleased` bullet only |

## Architecture

Two icon sets, because they have different fates.

### `TableIcons` — public, `table-core`

New file `table-core/src/commonMain/kotlin/ua/wwind/table/icon/TableIcons.kt`. Holds the **17 icons
the published library draws** (18 if `DragIndicator` splits — see the style-variant rule below):

| From | Icons |
|---|---|
| `table-core` | `Close`, `KeyboardArrowLeft`, `KeyboardArrowRight`, `ArrowUpward`, `ArrowDownward`, `Sort`, `FilterAltFilled`, `FilterAltOutlined`, `DragIndicator`, `SwapHoriz` |
| `table-format` | `Add`, `Delete`, `ContentCopy`, `Save`, `ArrowDropUp`, `Check`, `FormatColorReset` |

`table-format` sees them through its existing `api(project(":table-core"))`, so no new module wiring
is required.

Each icon is a `by lazy` property over an `ImageVector.Builder`, so the seventeen vectors are not all
allocated at startup for the sake of the two visible in a table header.

`autoMirror = true` is set on `KeyboardArrowLeft`, `KeyboardArrowRight` and `Sort`. Omitting it would
break RTL locales silently.

### `SampleIcons` — internal, `table-sample`

`internal object SampleIcons` inside `table-sample`, holding **9 further icons**: `Settings`, `Edit`,
`Link`, `LinkOff`, `ExpandLess`, `ExpandMore`, `Reorder`, `Star`, `BarChart` — plus up to two more if
the `Close`/`Delete` style variants turn out to differ, as described below.

These stay out of `TableIcons` deliberately. `table-sample` is not published, and padding the
library's public surface with icons the table itself never draws is API debt with no upside.

### Naming rule for style variants

An icon gets its plain Material name. A style suffix is added only when both styles of the same icon
are needed at once. Today that is exactly one case: `FilterAltFilled` and `FilterAltOutlined`, which
distinguish an active from an inactive column filter.

Three icons are currently used in two different styles at different call sites:

| Icon | Styles in use today |
|---|---|
| `Close` | `Rounded` (core, format), `Filled` (sample) |
| `Delete` | `Rounded` (format), `Filled` (sample) |
| `DragIndicator` | `Filled` (core, sample), `Rounded` (format) |

The generator resolves these rather than guesswork: it dumps both styles and compares the path data.
Where the two are identical, one entry remains. Where they differ, both are kept with style suffixes,
so no call site changes appearance.

The three cases do not all land in the same place, and only one of them can affect the public API:

- **`DragIndicator`** — both styles are used by *published* modules, so a split grows `TableIcons`
  from 17 to 18 (`DragIndicatorFilled`, `DragIndicatorRounded`).
- **`Close` and `Delete`** — the second style is used only by `table-sample`. A split leaves
  `TableIcons` untouched and adds the variant to `SampleIcons` instead; a collapse lets
  `table-sample` reuse `TableIcons.Close` / `TableIcons.Delete` and keeps `SampleIcons` at 9.

So the totals are **`TableIcons` 17–18, `SampleIcons` 9–11, drawn from 29 (name, style) pairs**. The
generator's comparison decides; nothing here is assumed.

## Generation procedure

A temporary `table-core/src/jvmTest/kotlin/ua/wwind/table/icon/IconDumpTest.kt` that lives for
exactly one commit.

While `material-icons-extended` is still on the classpath, it references all 29 (name, style) pairs,
walks each `ImageVector.root` as a `VectorGroup`, and translates every `VectorPath`'s
`List<PathNode>` back into `PathBuilder` DSL calls (`moveTo`, `lineTo`, `curveTo`, `arcTo`, `close`
and their relative forms), writing ready-to-paste Kotlin into `build/`.

**The generator fails loudly rather than degrading quietly.** Material icons are expected to have
`defaultWidth`/`defaultHeight` of `24.dp`, a `24f` viewport, a solid black fill and no stroke. If any
icon deviates — a different fill, a non-zero `strokeLineWidth`, a nested group carrying a transform,
a non-default `pathFillType` — the generator throws with that icon's name instead of dropping the
property and handing back a subtly wrong vector. A surprise must surface now, not after release.

Run with a targeted task: `./gradlew :table-core:jvmTest --tests "*IconDumpTest*"`.

The output is pasted into `TableIcons.kt` and `SampleIcons.kt`. The generator is deleted in the same
commit that removes the dependency — it stops compiling the moment the dependency is gone.

## Call-site migration

19 files change imports. The edit is mechanical: `Icons.Rounded.Close` becomes `TableIcons.Close`.

- **`table-core` (5):** `TableActiveFilters.kt`, `component/TableHeader.kt`,
  `component/TableHeaderApi.kt`, `component/header/TableHeaderRow.kt`,
  `filter/component/main/number/NumberFilter.kt`
- **`table-format` (7):** `FormatDialogButtons.kt`, `FormatDialogTitle.kt`, `FormatDialogBody.kt`,
  `FormatDialogConditionTab.kt`, `component/FormatColorField.kt`, `component/FormatDropdownField.kt`,
  `component/ColorPickerDialog.kt`
- **`table-sample` (7):** `app/SampleApp.kt`, `app/components/AppToolbar.kt`,
  `app/components/PersonMovementsSection.kt`, `app/components/SettingsSidebar.kt`,
  `app/components/SelectionActionBar.kt`, `column/TableColumns.kt`, `filter/NumericRangeFilter.kt`

`table-paging` and `table-sample-android` use no icons and are untouched.

## Compatibility

`TableHeaderDefaults.icons()` and `LocalTableHeaderIcons`
(`table-core/src/commonMain/kotlin/ua/wwind/table/component/TableHeaderApi.kt`) carry
`Icons.Rounded.ArrowUpward` and friends as **default argument values of published API**. Swapping
those defaults is nevertheless safe:

- **Binary compatible.** Default arguments compile into the synthetic `icons$default` bridge; the
  function signature does not change.
- **Source compatible.** Callers passing their own `ImageVector` are unaffected.
- **No transitive break.** The dependency was `implementation`, never `api`, so consumers never had
  `material-icons-extended` on their compile classpath. It disappears from their *runtime* classpath,
  which is the point.

Public API delta: **one new `public object` with 17–18 properties, nothing removed.** That is a minor
change, not a major one.

## Build and quality gates

Removals:

- `build-logic/src/main/kotlin/ua/wwind/convention/compose.gradle.kts:20` — the
  `implementation(libs.findLibrary("compose-material-icons-extended").get())` line.
- `gradle/libs.versions.toml:17` — the `compose-material-icons = "1.7.3"` version.
- `gradle/libs.versions.toml:73` — the `compose-material-icons-extended` library entry.

### Naming suppressions

PascalCase properties violate two active rules: detekt's `VariableNaming` (active with no exclusion
in `config/detekt/detekt.yml`) and ktlint's `standard:property-naming` (`.editorconfig` sets
`ktlint_code_style = ktlint_official`).

The chosen fix is an inline suppression on the object itself:

```kotlin
@Suppress("VariableNaming", "ktlint:standard:property-naming")
public object TableIcons { … }
```

Not a rule relaxation in `detekt.yml` or `.editorconfig`, and not a baseline entry. The rule must
keep catching an accidental `val Foo` elsewhere, and baseline entries in this repository have already
demonstrated a habit of labelling the wrong declaration. A suppression confined to the one file where
PascalCase is deliberate and matches Material's own convention reads as a decision rather than an
evasion.

Generated path data is emitted one command per line, which keeps it inside the 120-column limit from
`.editorconfig`.

### Attribution

Both this project and Material Icons are Apache 2.0, so vendoring is licence-compatible and no
separate `NOTICE` file is required. `TableIcons.kt` and `SampleIcons.kt` each carry a header comment
recording that the path data derives from Material Icons (Google, Apache 2.0).

## Documentation

- `docs/content/guides/custom-header-icons.md` — note that header defaults come from `TableIcons` and
  that the object is available to consumers. The guide already demonstrates customisation through
  `MyUp`/`MyDown` placeholders, so this slots in naturally.
- `docs/content/guides/row-blocks.md`, `row-reordering.md`, `grouping.md` — these snippets are
  *consumer* code and stay valid, but now that the library ships no icon dependency, a one-line note
  should say the reader supplies these from their own icon set. `Icons.Default.DragIndicator` has a
  direct equivalent and becomes `TableIcons.DragIndicator`; `Reorder` and `Group` have none and
  remain as-is with the note.

## Changelog and versioning

The change is observable — a new public `TableIcons`, one fewer transitive dependency — so it earns a
bullet under `### Unreleased` in `CHANGELOG.md`.

No version bump. The five-file release checklist runs separately, when the user asks for a release to
be numbered.

## Verification

**Fidelity is proved, not asserted.** In the generation commit, while `material-icons-extended` is
still on the classpath, the generator also emits a temporary equality test asserting for all 29 pairs
that the freshly generated `TableIcons.X` matches the original `Icons.<Style>.X` on viewport,
`autoMirror`, path count and the full `PathNode` list. This test is deleted alongside the generator
in the following commit.

Remaining checks, using targeted tasks rather than `./gradlew build`:

1. Compile `:table-core`, `:table-format` and `:table-sample` for JVM, plus the common metadata task.
2. `./gradlew qualityCheck`.
3. **The objective itself:**
   `./gradlew :table-core:dependencies --configuration jvmRuntimeClasspath | grep material-icons`
   must return nothing. This is what separates "removed" from "believed removed".
4. Run the desktop sample and compare the table header, the active-filters bar and the format dialog
   against current `main`.

Pre-existing Skiko/karma failures in `jsBrowserTest` are out of scope and must not be read as
regressions from this work.

## Risks

| Risk | Mitigation |
|---|---|
| A vendored icon differs subtly from the original | The equality test over all 29 pairs runs while both are available |
| An icon uses a vector feature the generator does not translate | The generator throws on any deviation from the expected shape instead of emitting a lossy vector |
| Growing the public API with 17–18 properties that must now be maintained | The set is closed — it is exactly what the library draws; new icons are an explicit decision |
| Future icons need adding without the old dependency | Documented in the spec: re-add the dependency temporarily in a scratch branch, or transcribe the SVG from `google/material-design-icons` |
