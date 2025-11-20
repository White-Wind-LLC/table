### Changelog

All notable changes to this project will be documented in this file.

### 1.5.0 — 2025-11-20

- Added: Fixed (pinned) columns support with configurable side and count via `TableSettings`.
    - New `fixedColumnsCount` and `fixedColumnsSide` properties allow pinning one or more columns on the left or right
      side of the table header and body.
    - Fixed columns remain visible while horizontally scrolling the rest of the table.
    - Visual separators and z-index handling ensure clear separation between fixed and scrollable columns.
- Added: Public access to `TableState.settings` and `TableState.dimensions` to simplify integration with custom UI
  controls and preview tooling.

Compare: [v1.4.1...v1.5.0](https://github.com/White-Wind-LLC/table/compare/v1.4.1...v1.5.0)

### 1.4.1 — 2025-11-20

**BREAKING CHANGES:**

- **Removed:** `rowLeading` and `rowTrailing` parameters from all `Table` composable functions.
    - These parameters previously allowed rendering custom content before or after table rows.
    - **Migration:** Use regular table columns defined via `ColumnSpec` instead. For leading content (e.g., checkboxes,
      avatars), define a dedicated column at the start of your column list. For trailing content (e.g., action buttons),
      add a column at the end.
    - **Example:**
      ```kotlin
      // Before (1.4.0)
      Table(
          rowLeading = { item -> Checkbox(...) },
          rowTrailing = { item -> IconButton(...) },
          columns = dataColumns,
          // ...
      )
      
      // After (1.4.1)
      val columns = tableColumns<Item, Field> {
          // Leading column for checkbox
          column(Field.Select, valueOf = { it }) {
              width(48.dp)
              cell { Checkbox(...) }
          }
          
          // Your data columns
          // ...
          
          // Trailing column for actions
          column(Field.Actions, valueOf = { it }) {
              width(64.dp)
              cell { IconButton(...) }
          }
      }
      
      Table(
          columns = columns,
          // ...
      )
      ```
- **Removed:** All internal infrastructure related to `rowLeading`/`rowTrailing`:
    - `hasLeading`, `leadingColumnWidth`, `rowLeadingPresent`, `leadingOffset` parameters removed from internal
      functions.
    - `RowLeadingSection` component removed.

**Benefits of this change:**

- Simplified API with fewer special-case parameters.
- Leading/trailing content now benefits from full column capabilities: sorting, resizing, filtering, custom styling,
  etc.
- More consistent and flexible table architecture.

Compare: [v1.4.0...v1.4.1](https://github.com/White-Wind-LLC/table/compare/v1.4.0...v1.4.1)

### 1.4.0 — 2025-11-19

+ Added: Support for embedded detail tables via the `embedded` flag and `rowEmbedded` slot for rendering nested tables
  inside parent rows.
+ Added: `DisabledTableFilter` type to explicitly disable filtering and conditional formatting conditions for
  specific columns while still satisfying per-column filter type requirements.

Compare: [v1.3.1...v1.4.0](https://github.com/White-Wind-LLC/table/compare/v1.3.1...v1.4.0)

### 1.3.1 — 2025-11-17

- Changed: Updated Kotlin to 2.2.21 and Compose Multiplatform to 1.9.2.

Compare: [v1.3.0...v1.3.1](https://github.com/White-Wind-LLC/table/compare/v1.3.0...v1.3.1)

### 1.3.0 — 2025-11-17

- Added: Fast filter components with animated visibility and synchronized state management for boolean, enum, number,
  text, and date filters.
- Added: Date filter state management with synchronized external state and debounce handling for improved filtering
  functionality.
- Added: `enableDragToScroll` setting to `TableSettings` for customizable scroll behavior.
- Added: Custom `TableTextField` component with configurable padding for consistent styling across filter components.
- Changed: Converted all list types to immutable collections (`ImmutableList`) for better performance, thread safety,
  and prevention of unintended modifications.
- Fixed: Scroll to top on sort change for better user experience.
- Fixed: Updated `LaunchedEffect` dependencies to track table state for correct cell selection handling.
- Chore: Sample app updated with horizontal scrolling toolbar, new demo data, and Position enum.
- Chore: Documentation updated with Immutable Collections section in README.

Compare: [v1.2.2...v1.3.0](https://github.com/White-Wind-LLC/table/compare/v1.2.2...v1.3.0)

### 1.2.2 — 2025-09-26

- Added: Inertial fling support with velocity tracking and decay animation for smoother scroll momentum.
- Fixed: Added table state dependency to `LaunchedEffect` in auto width effect and measurement utilities for correct
  recomposition and measurement updates.

Compare: [v1.2.1...v1.2.2](https://github.com/White-Wind-LLC/table/compare/v1.2.1...v1.2.2)

### 1.2.1 — 2025-09-26

- Fixed: Render sticky header for unordered list.

Compare: [v1.2.0...v1.2.1](https://github.com/White-Wind-LLC/table/compare/v1.2.0...v1.2.1)

### 1.2.0 — 2025-09-26

- Added: Group by functionality with customizable group headers and sticky positioning.
- Added: Column header dropdown menu with group by option for enhanced data organization.
- Fixed: Viewport scrolling logic and cell visibility calculations for improved user interaction.
- Changed: Extracted cell visibility logic and nested scroll handling for better user experience.
- Chore: Unused filter expression class to clean up codebase.

Compare: [v1.1.3...v1.2.0](https://github.com/White-Wind-LLC/table/compare/v1.1.3...v1.2.0)

### 1.1.3 — 2025-09-23

- Fixed: Resolved header resizing bug that could cause column misalignment during dynamic layout updates.

Compare: [v1.1.2...v1.1.3](https://github.com/White-Wind-LLC/table/compare/v1.1.2...v1.1.3)

### 1.1.2 — 2025-09-23

- Changed: Table core and interaction internals updated (layout, dimensions, keyboard navigation, viewport utils) and
  sample columns updated to the new header API.

Compare: [v1.1.1...v1.1.2](https://github.com/White-Wind-LLC/table/compare/v1.1.1...v1.1.2)

### 1.1.1 — 2025-09-22

- Fixed: Cell selection border now covers the full cell height in dynamic row height mode.
- Fixed: Vertical scrolling to the selected cell behaves like Excel — only the next/previous row is revealed without
  jumping.
- Added: Precise row height measurement and caching to support minimal, direction-aware scrolling.
- Added: Cache of measured row heights is automatically cleared when `itemsCount` changes (new data loaded).

Compare: [v1.1.0...v1.1.1](https://github.com/White-Wind-LLC/table/compare/v1.1.0...v1.1.1)

### 1.1.0 — 2025-09-22

- Added: Auto-fit column widths based on content (measurement utilities and smart sizing).
- Added: Dynamic row height with min/max constraints via table settings and column specifications.
- Added: Keyboard navigation and cell selection; improved cell visibility handling.
- Changed: Refactored `TableHeader` for more stable layout and interaction handling.
- Changed: Dependencies updated; deprecated modules removed.
- Changed: Samples updated (JS/WASM styles, better demo data and layout).
- Fixed: Header column animation during resizing.

Compare: `v1.0.3...v1.1.0` on GitHub (`https://github.com/White-Wind-LLC/table/compare/v1.0.3...main`).
