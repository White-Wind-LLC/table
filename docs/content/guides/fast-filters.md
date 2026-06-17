# Fast Filters

Fast filters provide quick inline filtering directly in a dedicated row below the header. They share the same
`TableFilterState` as main filters but with simplified UI and pre-set default constraints:

- **Location**: Rendered as a horizontal row below the header when `settings.showFastFilters = true` and at least
  one visible column has a filter configured (not `null` or `DisabledTableFilter`).
- **Synchronized state**: Fast filters and main filter panels use the same `state.filters`, changes in one immediately
  reflect in the other.
- **Default constraints**: Each fast filter type uses a sensible default:
    - `TextTableFilter` → CONTAINS
    - `NumberTableFilter` → EQUALS
    - `BooleanTableFilter` → EQUALS (tri-state checkbox)
    - `DateTableFilter` → EQUALS (date picker)
    - `EnumTableFilter` → EQUALS (dropdown)
    - `CustomTableFilter` → fully custom (implement `RenderFastFilter` or leave empty)
- **Auto-apply**: Fast filters always apply changes automatically with debounce (controlled by
  `settings.autoFilterDebounce`).

Fast filters are ideal for quick data exploration and filtering without opening the full filter panel dialog.
