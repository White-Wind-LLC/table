# Header column menu and header Tab stop — design

Issue: [#82](https://github.com/White-Wind-LLC/table/issues/82)

## Goal

Every column action that today needs a drag, a double-click or is missing entirely must be
reachable from a discoverable, keyboard-operable column menu (WCAG 2.5.7). Consumers can tailor
that menu through a stable public API.

Today `ColumnHeaderDropdownMenuBox` offers only Group by / Ungroup, opens only on right-click or
long-press, and disappears entirely while row reorder is enabled. Resize is drag-only, auto-fit is
double-click-only, reorder needs a hover-only drag handle, pin/unpin and hide are not reachable at
runtime at all, and header cells cannot receive keyboard focus.

## Decisions taken during brainstorming

- Pin and hide state is owned by the library (`TableColumnsState`), not by consumer callbacks.
- The table gets two Tab stops: header and body (refines #81's single stop).
- Items a column can never do are hidden; items blocked by current state are disabled with a
  visible reason.
- Architecture: a menu model shared by the dropdown and `customActions`, plus a public builder
  for customization. The API is stable (no `@ExperimentalTableApi`).
- One branch and one PR, committed layer by layer.

## 1. State

### `TableColumnsState`

- `pinnedCount: Int` (snapshot state). Initialised from `TableSettings.pinnedColumnsCount` and
  re-synced when that setting changes. Every pinned-column calculation
  (`calculatePinnedColumnState` callers, header and body) reads `state.columns.pinnedCount`
  instead of the setting.
- `pin(key: C)`: moves the column to the inner end of the pinned block (adjacent to unpinned
  columns) and increments `pinnedCount`. The block is on `settings.pinnedColumnsSide`.
- `unpin(key: C)`: moves the column to the first unpinned position next to the block and
  decrements `pinnedCount`.
- `hidden: SnapshotStateSet<C>`, plus `hide(key)`, `show(key)` and `showAll()`.
  - A column is visible when `spec.visible && key !in hidden`.
  - `hide` is a no-op when the column is the last visible one.
  - `HeaderDerivedState` and `Table.kt`'s `visibleColumns` (also assigned to
    `state.visibleColumns`) apply this rule.
  - Columns with `spec.visible = false` stay under consumer control; `showAll()` does not
    affect them.
- `moveBy(key: C, delta: Int)`: moves a column by `delta` positions among visible columns, using
  the same full-order mapping as drag reorder (`computeReorderMove`). It never crosses the pinned
  boundary; a move inside the pinned block is allowed.

### `TableState`

- `clearSort()`: sets `sort` to null. It is a no-op while row reorder is enabled, matching
  `setSort`.
- `focusedHeaderColumn: C?` (internal): the header column that holds keyboard focus within the
  header Tab stop.

## 2. Menu model and public API (stable)

Public types live in `ua.wwind.table.component.ColumnMenuApi.kt` (beside `TableHeaderApi.kt`); internal model and renderer live in `ua.wwind.table.component.header`.

```kotlin
@JvmInline public value class ColumnMenuItemId(public val value: String)

@Immutable public data class ColumnMenuItem(
    val id: ColumnMenuItemId,
    val label: String,
    val icon: ImageVector?,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val checked: Boolean = false,
    val onClick: () -> Unit,
)

@Immutable public data class ColumnMenuSection(
    val id: String,
    val items: List<ColumnMenuItem>,
)

public fun interface ColumnMenuBuilder<C> {
    @Composable
    public fun build(column: C, defaults: List<ColumnMenuSection>): List<ColumnMenuSection>
}

public object ColumnMenuDefaults {
    public object Ids { /* SortAsc, SortDesc, ClearSort, OpenFilter, ClearFilter, Pin, Unpin,
                           MoveLeft, MoveRight, AutoFit, ResetWidth, GroupBy, Ungroup, Hide,
                           ShowHidden */ }
    public object Sections { /* Sort, Filter, Layout, Group, Visibility */ }
    public fun <C> builder(): ColumnMenuBuilder<C>          // returns defaults unchanged
    @Composable public fun icons(...): ColumnMenuIcons      // overridable icon set
}
```

- `Table`, `EditableTable` and the `table-paging` `Table` get a new parameter
  `columnMenu: ColumnMenuBuilder<C> = ColumnMenuDefaults.builder()`, documented in KDoc.
- An internal pure function `columnMenuModel(spec, state, context, strings, icons, callbacks)`
  produces the default sections. `context` is `Header` or `GroupHeader`.
- The builder receives those defaults and returns the final list: it can remove, reorder or add
  items. An empty list disables the menu and `customActions` for that column.
- The final list feeds both the dropdown and `semantics { customActions }`. Disabled items are
  excluded from `customActions`.
- New vendored icons in `TableIcons`: push-pin (pin/unpin), visibility, visibility-off,
  width-fit, width-reset. They are generated the same way as the existing vendored vectors.
- New `UiString` keys for item labels, the "Column options" button label and the disabled
  reasons. `DefaultStrings` gets English text for each.

### Default items

| Section | Items | Shown when |
|---|---|---|
| Sort | Sort ascending ✓, Sort descending ✓, Clear sort | `spec.sortable`; Clear sort only when this column is the sort column |
| Filter | Open filter, Clear filter | filterable column; Clear filter only when its filter is active |
| Layout | Pin / Unpin, Move left, Move right, Auto-fit width, Reset width | Pin vs Unpin by current state; width items only when `spec.resizable` |
| Group | Group by / Ungroup | Group by vs Ungroup by `state.groupBy` |
| Visibility (after a divider) | Hide column, Show hidden columns (N) | Show hidden only when `hidden` is not empty |

With `PinnedSide.Right` the pin label reads "Pin right" instead of "Pin left".

The sticky group header (`GroupHeaderContent`) gets only the Sort and Group sections.

### Disabled items and their reasons

| Item | Condition | Reason |
|---|---|---|
| Sort items, Clear sort, Group by | `rowReorderEnabled` | Unavailable while rows can be reordered |
| Group by | row blocks active (`rowBlocksNonEmpty`) | Unavailable while row blocks are shown |
| Move left / right | at the edge of its block (pinned or unpinned) | Already first / already last / already at the pinned edge |
| Reset width | no width override for the column | Already at default width |
| Auto-fit width | nothing measured yet | No content to fit yet |
| Hide column | last visible column | The last visible column can't be hidden |

While row reorder is enabled the menu is no longer suppressed. Filter, layout and visibility items
stay available, and sort and group items show the reason above.

## 3. Menu UI

- Material3 `DropdownMenu`. Sections are separated by `HorizontalDivider`. Each item has a leading
  icon and a label. The active sort direction gets a trailing check.
- A disabled item renders its reason in `bodySmall` under the label and exposes it as semantics
  `stateDescription`.
- Anchoring: at the pointer for right-click or long-press (as today); below the header cell,
  start-aligned, when opened by keyboard or the ⋮ button.
- `TableSettings.showColumnMenuButton: Boolean = false`. When true, each header shows a small ⋮
  button. The button is `canFocus = false` and labelled "Column options: <title>".
- "Open filter" opens the existing `FilterPanel` for the column (via the `filterColumn` state that
  `TableHeader` already holds).
- Primary-click-to-sort and the existing right-click/long-press triggers keep working.
  `ColumnHeaderDropdownMenuBox` becomes a renderer of the final section list.

## 4. Keyboard focus: two Tab stops

- The single `tableKeyboardNavigation` focus target on the scroll `Box` is split into two sibling
  focus targets. Scrolling and drag-to-scroll stay on the outer `Box`.
  - Header stop: a new `Modifier.tableHeaderKeyboardNavigation` on the header row.
  - Body stop: the existing `tableKeyboardNavigation`, moved onto the body section.
    `state.isFocused` keeps meaning "body focused".
- Tab moves header → body and Shift+Tab moves back, relying on composition order. If that proves
  unreliable with the lazy body or in `embedded` mode, the order is pinned with
  `focusProperties { next / previous }`.
- When the header stop gains focus with no `focusedHeaderColumn`, it falls back to the selected
  cell's column, then to the first visible column. The focused header shows a state layer in the
  same style as #81.

| Where | Key | Action |
|---|---|---|
| Header | ← / → | Previous / next visible column; scroll it into view |
| Header | Home / End | First / last visible column |
| Header | Enter / Space | Toggle sort if sortable and sort is not locked |
| Header | Shift+F10, Menu, Alt+↓ | Open the column menu |
| Header | ↓ | Move focus to the body. If the body has a selected cell, select row 0 of this column; otherwise only move focus |
| Body | ↑ on row 0 | Move focus to the header on the same column |
| Body | Shift+F10, Menu | Move focus to the header on the selected cell's column and open its menu |

- A pointer click on a header cell also moves focus to the header stop on that column.
- Inside the menu, standard `DropdownMenu` navigation applies. Closing the menu (Esc or after an
  action) returns focus to the header stop. Closing a `FilterPanel` opened from the menu does too.
- `SortButton`, `FilterButton` and the ⋮ button get `focusProperties { canFocus = false }`, so the
  table has exactly two Tab stops.
- Header cell semantics: `heading()` plus `customActions` built from the enabled items of the
  final menu. This exposes resize (auto-fit, reset width) and reorder (move left/right) to
  assistive technology.
- Arbitrary-width resize stays a pointer drag. Auto-fit and reset width are its non-drag
  alternatives.

## 5. Testing

All tests are in `table-core/src/commonTest`, using `runComposeUiTest` and assertk, following the
existing style.

- `TableColumnsStatePinHideTest`:
  - `pin`/`unpin` on both sides.
  - `hide`/`show`/`showAll`, and refusing to hide the last visible column.
  - `moveBy` never crosses the pinned boundary and skips hidden columns.
  - `pinnedCount` re-syncs from settings.
- `TableStateClearSortTest`: `clearSort`, and its no-op under the row reorder lock.
- `ColumnMenuModelTest`:
  - Items are hidden for incapable columns.
  - Every disabled reason from the table above.
  - Item pairs (Pin/Unpin, Group by/Ungroup, Clear sort, Clear filter, Show hidden).
  - The group header context gets only the Sort and Group sections.
- `ColumnMenuBuilderTest`:
  - A custom builder can remove, add and reorder items.
  - An empty list means no menu and no `customActions`.
- `ColumnHeaderMenuUiTest`:
  - Right-click opens the menu.
  - Clicking an item runs its action.
  - A disabled item shows its reason.
  - Header `customActions` match the enabled items.
- `HeaderKeyboardFocusTest` (desktop-only, beside `TableFocusTest`):
  - Tab and Shift+Tab move between the two stops.
  - Header navigation keys.
  - Enter sorts.
  - Shift+F10, Menu and Alt+↓ open the menu.
  - Esc returns focus to the header.
  - ↓ and ↑ cross between header and body.
  - Shift+F10 in the body opens the menu.
  - Sort and filter buttons are not Tab stops.
- `TableFocusTest` is updated for the body being the second stop. `ColumnVisibilityTest` and the
  resize tests must stay green.

## 6. Sample, docs, changelog

- `table-sample`:
  - A "Show column menu button" toggle in `SettingsSidebar`.
  - One custom `columnMenu` builder that adds an item.
- `docs/content`: a short page on the column menu, its keyboard access and `ColumnMenuBuilder`.
- `CHANGELOG.md` under `### Unreleased`, using the short one-line style with a link to #82:
  - Added: the header menu with sort, filter, pin, move, auto-fit, reset width, group and hide,
    and keyboard access to it.
  - Changed: the header is now its own Tab stop. New `UiString` keys break custom
    `StringProvider` implementations that have no `else` branch.

## Out of scope

- Keyboard resize by a fixed step.
- A per-column `pinned` flag in `ColumnSpec`.
- Persisting `hidden` and `pinnedCount`. Consumers can read and restore them through the public
  state.
