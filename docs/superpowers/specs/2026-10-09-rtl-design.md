# Right-to-left layouts — design

Issue: [#134](https://github.com/White-Wind-LLC/table/issues/134) (part of
[#84](https://github.com/White-Wind-LLC/table/issues/84); large font scale is split out to
[#135](https://github.com/White-Wind-LLC/table/issues/135))

## Goal

A table placed under `LocalLayoutDirection provides LayoutDirection.Rtl` behaves as the mirror
image of the LTR table: pinned columns hold still, shadows, indicators and menus appear on the
correct side, resize and drag-to-scroll follow the pointer, and the horizontal arrow keys move
focus in the direction of the arrow.

Today nothing in the library reads `LocalLayoutDirection`. `Row`, `LazyRow`, `Modifier.offset`,
relative alignments and auto-mirrored icons already mirror, and the scroll-into-view math is
start-relative, so it is already correct. What breaks is every physical horizontal transform, draw
coordinate, pointer delta and arrow key.

## Decisions taken during brainstorming

- #84 is split into #134 (RTL) and #135 (font scale). This spec covers #134 only.
- `PinnedSide` gets logical semantics with new names: `Start` / `End`. `Left` / `Right` are
  deprecated aliases of the same values.
- Menu labels keep the existing physical words ("Pin left", "Move right") and are chosen by the
  physical side the action affects in the current direction. No new `UiString` keys.
- The column menu ids `Ids.MoveLeft` / `Ids.MoveRight` are unreleased and are renamed outright to
  `Ids.MoveToStart` / `Ids.MoveToEnd` (`"move-start"` / `"move-end"`) without deprecation.
- The layout direction is read where it is used: `LocalLayoutDirection.current` in composables,
  `DrawScope.layoutDirection` in draw lambdas, and an explicit parameter for non-composable
  modifiers. One internal helper does the sign flip. No direction is stored in `TableState`.
- One branch (`feat/rtl-134`) and one PR.

## 1. Public API

### `PinnedSide` (`config/TableSettings.kt`)

```kotlin
/** Side to pin columns to, relative to the layout direction. */
public enum class PinnedSide {
    /** The leading columns: physically left in LTR, right in RTL. */
    Start,

    /** The trailing columns: physically right in LTR, left in RTL. */
    End,
    ;

    public companion object {
        @Deprecated("Pins the leading columns, which are on the right in RTL.", ReplaceWith("PinnedSide.Start"))
        public val Left: PinnedSide get() = Start

        @Deprecated("Pins the trailing columns, which are on the left in RTL.", ReplaceWith("PinnedSide.End"))
        public val Right: PinnedSide get() = End
    }
}
```

- The aliases are companion properties, not extra enum entries. As a result
  `PinnedSide.Left === PinnedSide.Start`, so consumer comparisons such as
  `side == PinnedSide.Left` keep working after the default changes to `Start`.
- The default `TableSettings.pinnedColumnsSide` becomes `PinnedSide.Start`, which behaves the same
  as before.
- Accepted breaks, documented in the CHANGELOG:
  - Code compiled against 2.4.x that references `Left` / `Right` must be recompiled, because the
    enum constants become companion getters.
  - An exhaustive `when (side) { PinnedSide.Left -> …; PinnedSide.Right -> … }` used as an
    expression stops compiling and needs `Start` / `End`.
  - `PinnedSide.valueOf("Left")` throws.

### Column menu ids (`component/ColumnMenuApi.kt`)

- `Ids.MoveLeft` → `Ids.MoveToStart`, id `"move-start"`, doc "Move the column one step toward the
  start".
- `Ids.MoveRight` → `Ids.MoveToEnd`, id `"move-end"`, doc "Move the column one step toward the end".

### Unchanged

- `UiString.ColumnMenuPinLeft/Right` and `ColumnMenuMoveLeft/Right` keep their keys and English
  text; only the logic that picks them changes (§3.10).

## 2. Internal groundwork

- A helper in the existing `LayoutUtils.kt` (package root):

  ```kotlin
  /** [x] measured from the start edge, as a physical (left-to-right) offset delta. */
  internal fun LayoutDirection.sign(x: Float): Float = if (this == LayoutDirection.Rtl) -x else x
  ```
- `PinnedEdge.Left/Right` → `PinnedEdge.Start/End`. The doc changes from "physical side" to
  "logical side". `PinnedEdge.hasContentUnder` keeps its logic, which is already start-relative.
- `showLeftDivider` / `showRightDivider` and similar names in `TableCell` / `HeaderCell` are renamed
  to `Start` / `End` where they mean logical sides.
- All internal `PinnedSide.Left/Right` uses move to `Start/End` (`TableColumnsState`,
  `ColumnGeometry`, `PinnedColumnState`, `ColumnMenuModel`, footer, tests, docs).

## 3. Fixes

The `rtl` direction comes from `LocalLayoutDirection.current` unless stated otherwise.

1. **Pinned `translationX`** (`state/PinnedColumnState.kt`). `calculatePinnedColumnState` takes
   `layoutDirection: LayoutDirection`. `pinnedTranslationX` returns `sign(value)` for `Start` and
   `sign(value - maxValue)` for `End`. Callers: `TableRowItem`, `TableHeaderRow`,
   `TableFooterRow`, `FastFiltersRow`.
2. **Pinned edge shadow** (`component/PinnedEdgeShadow.kt`). The physical x of the shadow is
   derived from the logical edge and `DrawScope.layoutDirection`: `End` edge in LTR and `Start`
   edge in RTL draw at `size.width`, the others at `-w`.
3. **Viewport-wide headers.** Inline group header (`component/body/TableBody.kt`), sticky group
   overlay (`component/body/GroupStickyOverlay.kt`) and row-block band (`component/body/RowUnit.kt`)
   use `translationX = sign(horizontalState.value)`.
4. **Selected-row indicator** (`component/body/TableRowItem.kt`). Drawn at
   `x = horizontalState.value` in LTR and `x = size.width - horizontalState.value - barWidth` in RTL,
   using `DrawScope.layoutDirection`.
5. **Column resize** (`component/header/ColumnResizersOverlay.kt`).
   - The width change is `grow(sign(dragAmount))`.
   - The pointer position inside the viewport, used for edge auto-scroll, uses the logical pointer
     x: `position.x` in LTR, `span.width - position.x` in RTL.
   - The hover line of the last strip is drawn at the strip's physical end edge (right in LTR,
     left in RTL). Middle strips are centred and need no change.
   - `shouldGrowAtResizeEdge`, `resizeScrollPullback` and `GrowWhileHeldAtEdge` already receive
     logical values and stay unchanged.
6. **Drag-to-scroll and fling** (`interaction/DraggableTable.kt`). The horizontal drag delta and
   `velocity.x` pass through `sign` before `dispatchRawDelta`, because these calls bypass the
   auto-reversal of `horizontalScroll`.
7. **Arrow keys** (`interaction/KeyboardNavigation.kt`, `interaction/HeaderKeyboardNavigation.kt`).
   Both modifiers take `layoutDirection: LayoutDirection`, passed from `Table.kt` and
   `TableHeader.kt`. In RTL `DirectionLeft` maps to `col + 1` and `DirectionRight` to `col - 1`.
   Home, End and Tab are index-based and stay unchanged.
8. **Header content arrangement** (`component/header/HeaderCell.kt`). The alignment-to-arrangement
   conversion uses the real layout direction instead of the hardcoded `LayoutDirection.Ltr`, and
   maps the physical side back to a relative arrangement for the current direction, so an absolutely
   right-aligned title stays physically on the right and the sort icon stays on its inner side.
   (Absolute arrangements would not reverse the child order in RTL.)
9. **Column menu at the pointer** (`component/header/ColumnHeaderDropdownMenuBox.kt`). Material3
   `DropdownMenu` anchors to the right edge and negates `offset.x` in RTL, so the pointer offset is
   `x = anchorWidth - pointer.x` in RTL.
10. **Menu labels and icons** (`component/header/ColumnMenuModel.kt`). The model builder takes
    `layoutDirection`.
    - Pin: `Start` in LTR or `End` in RTL → `ColumnMenuPinLeft`; otherwise `ColumnMenuPinRight`.
    - Move: toward the start in LTR or toward the end in RTL → `ColumnMenuMoveLeft`; otherwise
      `ColumnMenuMoveRight`. Ids are `MoveToStart` / `MoveToEnd` by logical direction.
    - Icons stay logical (`KeyboardArrowLeft` toward the start): they are auto-mirrored drawables.
      Only labels follow the physical side.
11. **Filter chip scroll buttons** (`TableActiveFilters.kt`). Verify during implementation. If the
    button with the left arrow scrolls toward the start in RTL, its label `FilterChipsScrollLeft` is
    wrong; then pick the label by direction as in §3.10. Otherwise no change.

## 4. Out of scope

- Pinned cells recompose on every scroll frame because `translationX` is computed during
  composition. This is a performance issue in both directions.
- Scroll-into-view ignores the width of the pinned run, so a focused cell can stop under the pinned
  columns. Same in both directions.
- Column drag-to-reorder uses `sh.calvin.reorderable`, which reads the layout direction itself. It
  is checked manually in the sample only.
- Context-menu callback positions in the body stay root-physical, as today.
- Large font scale (#135).

## 5. Testing

All tests live in `table-core/src/commonTest`. RTL is set with
`CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` around the table.
UI tests use `return runComposeUiTest { … }` with assertions inside, so they run on web as well.

### Unit

- `calculatePinnedColumnState`: `translationX` for `Start` / `End` in LTR and RTL.
- Arrow mapping in body and header navigation for both directions.
- `ColumnMenuModel`: pin/move labels, icons and ids in LTR and RTL for both pinned sides.
- `HeaderCell` alignment conversion: relative and absolute alignments in both directions.
- `PinnedSide.Left === PinnedSide.Start` and `PinnedSide.Right === PinnedSide.End`.
- Existing tests move from `Left/Right` to `Start/End`.

### UI

- Pinned column in RTL keeps its bounds after a horizontal scroll.
- Resize in RTL: dragging the handle physically left widens the column (mouse input, modelled on
  `ColumnResizeTest`).
- Keyboard in RTL: the Left arrow moves focus to the next column (modelled on `TableFocusTest`).
- Selected-row indicator: a pixel check via `captureToImage` on JVM if practical; otherwise a unit
  test of the x calculation.

### Verification before the PR

- `jvmTest`, `wasmJsBrowserTest`, and an iOS compile (`-PenableIos=false` misses native
  compile errors).
- Manual check in the sample with the RTL switch: pinning both sides, shadow, resize, drag-scroll,
  keyboard, column menu, column reorder, group headers.

## 6. Sample

- `SampleTableConfig` gets `rtl: Boolean = false`; `SettingsSidebar` gets a "Right-to-left" switch.
- In `SampleApp.kt` the forced `LayoutDirection.Ltr` around the content becomes
  `if (config.rtl) Rtl else Ltr`, wrapping both `MainTable` and `PagingDemo`.

## 7. Documentation

- CHANGELOG, Unreleased:
  - `Added:` right-to-left layout support (#134).
  - `Changed:` `PinnedSide.Start` / `End` replace the deprecated `Left` / `Right`; recompile code
    built against 2.4.x, and exhaustive `when` over `PinnedSide` needs the new names (#134).
- `docs/content/guides/column-menu.md`: the restore example uses `PinnedSide.Start/End`.
- KDoc on `PinnedSide`, `PinnedEdge` and the renamed ids.
