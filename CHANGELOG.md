### Changelog

All notable changes to this project will be documented in this file.

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
