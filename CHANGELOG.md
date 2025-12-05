### Changelog

All notable changes to this project will be documented in this file.

### 1.7.3 — 2025-12-05

**BREAKING CHANGES:**

- **Changed:** Cell lambda signature updated to include `tableData` parameter for consistent API across all table
  components.
    - `ColumnSpec.cell` signature changed from `@Composable BoxScope.(T) -> Unit` to
      `@Composable BoxScope.(T, E) -> Unit`.
    - All cell content now receives the table data parameter, matching the signature of `header`, `footer`, and
      `editCell`.
    - Provides access to shared state within regular cell rendering, enabling dynamic behavior based on table-wide
      context.
    - **Migration:** Update all `cell { item -> ... }` declarations to `cell { item, tableData -> ... }` or
      `cell { item, _ -> ... }` if table data is not needed.
    - **Example:**
      ```kotlin
      // Before (1.7.2)
      cell { item ->
          Text(item.name)
      }
      
      // After (1.7.3)
      cell { item, tableData ->
          // Can now access tableData for conditional rendering
          Text(item.name)
      }
      
      // Or use underscore if not needed
      cell { item, _ ->
          Text(item.name)
      }
      ```
- Changed: Internal measurement utilities updated to propagate `tableData` parameter through the measurement pipeline.
    - `MeasureCellMinWidth` now receives and forwards table data to measured content.
    - Group header rendering components updated to pass table data to cell content.
    - Ensures consistent data flow throughout the entire table rendering lifecycle.

**Benefits of this change:**

- Consistent lambda signatures across all table component slots (cell, header, footer, editCell).
- Cells can now access shared table state for dynamic rendering without prop drilling.
- Better alignment with the table data pattern introduced in v1.7.1.
- Enables more flexible cell customization based on table-wide context.

Compare: [v1.7.2...v1.7.3](https://github.com/White-Wind-LLC/table/compare/v1.7.2...v1.7.3)

### 1.7.2 — 2025-12-04

- Added: `shape` parameter to paging `Table` composables for customizable table surface shape (default:
  `RoundedCornerShape(4.dp)`).
    - Aligns paging module API with core table module for consistent shape customization.
    - Both overloads of paging `Table` function now support shape parameter.
- Added: New paging `Table` composable overload with `tableData` parameter for shared state access.
    - Enables passing table data to headers, footers, and edit cells in paging tables.
    - Generic parameter `E` represents table data type, consistent with core table API.
    - Provides full feature parity with core `Table` composable for table data customization.

Compare: [v1.7.1...v1.7.2](https://github.com/White-Wind-LLC/table/compare/v1.7.1...v1.7.2)

### 1.7.1 — 2025-12-02

- Added: Support for custom table data (`tableData` parameter) that can be shared across headers, footers, filters, and
  edit cells.
    - New `Table` overload that accepts `tableData` parameter for passing shared state to table components.
    - Generic parameter `E` now represents table data type instead of edit state type for improved flexibility.
    - Headers, footers, and edit cells receive table data as a parameter, enabling access to shared state during
      rendering.
    - Filters (custom, fast, and panel) receive table data for enhanced customization and state synchronization.
    - `EditableTable` parameter renamed from `editState` to `tableData` to better reflect its purpose.
- Changed: Enhanced column specification API for table data support.
    - `ColumnSpec.header` signature changed from `@Composable () -> Unit` to `@Composable (E) -> Unit`.
    - `ColumnSpec.footer` signature changed from `@Composable BoxScope.() -> Unit` to
      `@Composable BoxScope.(E) -> Unit`.
    - `ColumnSpec.editCell` documentation updated to clarify table data parameter usage.
    - `ReadonlyTableColumnsBuilder` now generic over table data type `E` for consistent API surface.
    - `TableFilterType.Custom` interface methods updated to receive table data parameter.
- Changed: Documentation improvements and sample app refactoring.
    - README updated with comprehensive table data usage examples and best practices.
    - Sample app refactored to demonstrate table data patterns with centralized validation logic.
    - New utility classes added: `PersonValidator`, `PersonFilterMatcher`, `PersonFilterStateFactory`,
      `DefaultFormatRulesProvider`, and `PersonSorter`.
    - `PersonTableData` model introduced in sample to showcase shared state management across table components.

Compare: [v1.7.0...v1.7.1](https://github.com/White-Wind-LLC/table/compare/v1.7.0...v1.7.1)

### 1.7.0 — 2025-12-02

- Added: Table footer support with full customization and pinning capabilities.
    - New `footer` DSL in `ColumnSpec` builder for defining per-column footer content.
    - `TableSettings.showFooter` enables footer row rendering below table body.
    - `TableSettings.footerPinned` controls whether footer stays pinned at the bottom (non-embedded tables only).
    - Footer respects pinned column configuration for consistent horizontal scrolling behavior.
    - `TableColors` extended with `footerContainerColor` and `footerContentColor` properties.
    - `TableDimensions` extended with `footerHeight` property for customizable footer sizing.
- Changed: Terminology update from "fixed" to "pinned" columns for better clarity.
    - `TableSettings.fixedColumnsCount` → `pinnedColumnsCount` (deprecated with `ReplaceWith` migration).
    - `TableSettings.fixedColumnsSide` → `pinnedColumnsSide` (deprecated with `ReplaceWith` migration).
    - `FixedSide` enum → `PinnedSide` enum (deprecated as typealias for backward compatibility).
    - `FixedColumnState` → `PinnedColumnState` (internal state class renamed).
    - `TableDimensions.fixedColumnDividerThickness` → `pinnedColumnDividerThickness` (deprecated with `ReplaceWith`
      migration).
    - All internal references updated to use "pinned" terminology throughout the codebase.
- Changed: Refactored `TableDefaults` into a dedicated object with factory methods.
    - Extracted from `TableColors.kt` to new `TableDefaults.kt` module for better organization.
    - `TableDefaults.colors()` composable for creating color schemes with Material 3 theme defaults.
    - `TableDefaults.standardDimensions()` for comfortable spacing and standard sizes (52dp rows, 56dp header).
    - `TableDefaults.compactDimensions()` for minimal spacing and compact sizes (36dp rows, 40dp header).
    - `TableDimensions` constructor now requires all parameters (no default values) to encourage explicit
      configuration.
- Fixed: Auto-width adjustment for embedded tables now uses dedicated `ApplyAutoWidthEffect` utility.
    - Resolves issues with incorrect column width calculations in nested table scenarios.
    - Ensures embedded tables properly measure and apply auto-width independently from parent tables.

Compare: [v1.6.4...v1.7.0](https://github.com/White-Wind-LLC/table/compare/v1.6.4...v1.7.0)

### 1.6.4 — 2025-11-27

- Added: `TableState.tableWidth` computed property to dynamically calculate total table width based on visible columns
  and their widths.
    - Automatically recalculates when column order, widths, or visible columns change using `derivedStateOf`.
    - Accounts for both regular column widths and divider widths, including special handling for fixed column dividers.
    - Eliminates need for manual width computation in row-embedded content and other custom layouts.
- Removed: `calculateTableWidth()` function from `LayoutUtils.kt` in favor of centralized `tableWidth` property in
  `TableState`.
- Changed: Internal visibility improvements and code cleanup:
    - Changed `tableKeyboardNavigation` modifier from public to internal as it's not part of the public API.
    - Simplified `LocalTableState` declaration with explicit type annotation.
    - Minor formatting adjustments to KDoc comments and code structure.
- Fixed: Row-embedded content (e.g., nested tables) now uses its own width instead of `tableWidth`
  for proper width alignment.

Compare: [v1.6.3...v1.6.4](https://github.com/White-Wind-LLC/table/compare/v1.6.3...v1.6.4)

### 1.6.3 — 2025-11-27

- Added: `TableState.recalculateAutoWidths()` method for manual column width recalculation.
    - Useful for deferred/paginated data loading scenarios where initial auto-width calculation occurred on empty data.
    - After data loads and content is measured, call this method to recompute column widths based on actual content.

Compare: [v1.6.1...v1.6.3](https://github.com/White-Wind-LLC/table/compare/v1.6.1...v1.6.3)

### 1.6.1 — 2025-11-22

- Added: Support for fully custom filters with renderer and state provider, enabling flexible and consistent custom
  filter UI and behavior.
    - New `TableFilterType.Custom` interface allows defining custom filter types with dedicated composable UI.
    - `CustomFilterStateProvider` interface for building chip text and managing custom filter state.
    - `CustomFastFilter` component for rendering custom fast filters with animated visibility.
    - `CustomFilter` component for rendering custom filters in filter panels with action buttons.
    - Sample implementation includes `NumericRangeFilter` with range slider, histogram visualization, and predefined
      range buttons.

Compare: [v1.6.0...v1.6.1](https://github.com/White-Wind-LLC/table/compare/v1.6.0...v1.6.1)

### 1.6.0 — 2025-11-21

- Added: Row-scoped cell editing mode with custom edit UI, validation, and keyboard navigation.
    - Enable editing via `TableSettings(editingEnabled = true)`.
    - Use `EditableTable<T, C, E>` composable for tables with editing support.
    - Declare editable columns with `editableTableColumns<T, C, E> { ... }` DSL and per-cell `editCell` content.
    - Lifecycle callbacks: `onRowEditStart`, `onRowEditComplete`, `onEditCancelled` for validation and state management.
    - Keyboard navigation: Enter/Done moves to next editable cell, Escape cancels editing (desktop targets).
    - Double-click any editable cell to enter row edit mode; all editable cells in the row become active simultaneously.
- Added: `TableCellTextField` component for text editing inside table cells.
    - Focus integration via `syncEditCellFocus()` modifier ensures correct keyboard navigation and focus handling.
    - Compact layout with reduced paddings and optional border visibility for dense table rows.
    - Visual consistency with Material 3 inputs used throughout table UI.
    - Supports error states, keyboard actions, and customizable styling.
- Added: `EditCellFocusSync.kt` module for coordinating focus between table state and editable cell composables.
- Changed: `TableTextField` visibility changed from internal to public for reuse in custom filter implementations.
- Changed: Filter components enhanced with configurable border visibility and improved layout:
    - Added `showBorder` parameter to `TableTextField`, date filters, and number filters.
    - Adjusted filter row height for better visual consistency.
    - Number range slider text aligned to end for improved readability.
- Changed: Major refactoring of `TableState` to support editing mode, edit cell tracking, and edit lifecycle.
- Changed: `ColumnSpec` API extended with `editCell` DSL for defining per-cell edit content.
- Changed: Sample app updated with comprehensive editing demo including validation, error handling, and state
  management.

Compare: [v1.5.1...v1.6.0](https://github.com/White-Wind-LLC/table/compare/v1.5.1...v1.6.0)

### 1.5.1 — 2025-11-20

- Fixed: Ensure `TableTextField` (table text field) uses the current theme's default text style to improve text
  visibility and consistency with app theming.
- Removed: Unused `okio` and `dnd` references from build scripts; updated documentation to reflect removed third-party
  libraries.

Compare: [v1.5.0...v1.5.1](https://github.com/White-Wind-LLC/table/compare/v1.5.0...v1.5.1)

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
