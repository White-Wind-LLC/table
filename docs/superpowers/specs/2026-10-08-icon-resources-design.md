# Icons as Compose resource files

## Goal

Icons are drawn from vector-drawable XML files under `composeResources/drawable`, the way
`wms-client`'s `core/designsystem` does it, instead of Kotlin `ImageVector.Builder` code. Adding or
swapping an icon becomes "drop in an XML file and add a property"; the glyphs themselves do not
change.

Scope: the 26 library icons in `table-core` and the 11 sample-only icons in `table-sample`.

## Decisions

- `TableIcons` exposes `DrawableResource`, as `wms-client`'s `AppIcons` does. Breaking change.
- Parameters that accept an icon from the caller take `Painter`, so callers can pass a library
  resource, their own resource, a `rememberVectorPainter(...)` of any `ImageVector`, or a bitmap.
- The XML is generated from the current path data, so every glyph stays identical. No switch to
  Material Symbols.
- File names follow `wms-client`: snake_case name plus the Material style family,
  e.g. `close_rounded.xml`.

## Resources

### table-core

`table-core/src/commonMain/composeResources/drawable/`, one file per `TableIcons` property:

| Property | Material source | File |
|---|---|---|
| Close | Rounded.Close | `close_rounded.xml` |
| KeyboardArrowLeft | AutoMirrored.Rounded.KeyboardArrowLeft | `keyboard_arrow_left_auto_mirrored_rounded.xml` |
| KeyboardArrowRight | AutoMirrored.Rounded.KeyboardArrowRight | `keyboard_arrow_right_auto_mirrored_rounded.xml` |
| ArrowUpward | Rounded.ArrowUpward | `arrow_upward_rounded.xml` |
| ArrowDownward | Rounded.ArrowDownward | `arrow_downward_rounded.xml` |
| Sort | AutoMirrored.Outlined.Sort | `sort_auto_mirrored_outlined.xml` |
| FilterAltFilled | Filled.FilterAltFilled | `filter_alt_filled.xml` |
| FilterAltOutlined | Filled.FilterAltOutlined | `filter_alt_outlined.xml` |
| DragIndicator | Filled.DragIndicator | `drag_indicator_filled.xml` |
| SwapHoriz | Filled.SwapHoriz | `swap_horiz_filled.xml` |
| Add | Rounded.Add | `add_rounded.xml` |
| Delete | Rounded.Delete | `delete_rounded.xml` |
| ContentCopy | Rounded.ContentCopy | `content_copy_rounded.xml` |
| Save | Rounded.Save | `save_rounded.xml` |
| ArrowDropUp | Rounded.ArrowDropUp | `arrow_drop_up_rounded.xml` |
| Check | Rounded.Check | `check_rounded.xml` |
| FormatColorReset | Filled.FormatColorReset | `format_color_reset_filled.xml` |
| PushPin | Rounded.PushPin | `push_pin_rounded.xml` |
| PushPinOutlined | Outlined.PushPin | `push_pin_outlined.xml` |
| Visibility | Rounded.Visibility | `visibility_rounded.xml` |
| VisibilityOff | Rounded.VisibilityOff | `visibility_off_rounded.xml` |
| SettingsEthernet | Rounded.SettingsEthernet | `settings_ethernet_rounded.xml` |
| SettingsBackupRestore | Rounded.SettingsBackupRestore | `settings_backup_restore_rounded.xml` |
| TableRows | Rounded.TableRows | `table_rows_rounded.xml` |
| ErrorOutline | Rounded.ErrorOutline | `error_outline_rounded.xml` |
| MoreVert | Rounded.MoreVert | `more_vert_rounded.xml` |

The three `AutoMirrored` icons carry `android:autoMirrored="true"` so they still flip in RTL.

`table-core/build.gradle.kts`:

- adds the `org.jetbrains.compose.components:components-resources` dependency (version catalog
  entry `compose-components-resources`, version `compose-multiplatform`);
- `compose.resources { packageOfResClass = "ua.wwind.table.generated.resources";
  publicResClass = false; generateResClass = always }`. `Res` stays internal; the public way in is
  `TableIcons`.

`androidResources { enable = true }` is already set in the Android target convention (CMP-9547),
so the files package into consumers' APKs.

### table-sample

`table-sample/src/commonMain/composeResources/drawable/`, one file per `SampleIcons` property, all
`Filled`: `settings_filled.xml`, `edit_filled.xml`, `link_filled.xml`, `link_off_filled.xml`,
`expand_less_filled.xml`, `expand_more_filled.xml`, `reorder_filled.xml`, `star_filled.xml`,
`bar_chart_filled.xml`, `close_filled.xml`, `delete_filled.xml`. Same resources setup as core,
with `packageOfResClass = "ua.wwind.table.sample.generated.resources"`.

### Removed

`table-core/.../icon/vector/*.kt` (26 files) and `table-sample/.../icon/vector/*.kt` (11 files).

## Public API

```kotlin
public object TableIcons {
    /** Material `Icons.Rounded.Close`. */
    public val Close: DrawableResource get() = Res.drawable.close_rounded
    // … one property per row of the table above, same names as today
}

@Immutable
public data class TableHeaderIcons(
    val sortAsc: Painter,
    val sortDesc: Painter,
    val sortNeutral: Painter,
    val filterActive: Painter,
    val filterInactive: Painter,
)

public object TableHeaderDefaults {
    @Composable
    public fun icons(
        sortAsc: Painter = painterResource(TableIcons.ArrowUpward),
        sortDesc: Painter = painterResource(TableIcons.ArrowDownward),
        sortNeutral: Painter = painterResource(TableIcons.Sort),
        filterActive: Painter = painterResource(TableIcons.FilterAltFilled),
        filterInactive: Painter = painterResource(TableIcons.FilterAltOutlined),
    ): TableHeaderIcons
}

public val LocalTableHeaderIcons: ProvidableCompositionLocal<TableHeaderIcons> =
    staticCompositionLocalOf { error("TableHeaderIcons are provided by Table") }

@Immutable
public data class ColumnMenuItem(
    val id: ColumnMenuItemId,
    val label: String,
    val icon: Painter?,
    // … unchanged
)
```

- `LocalTableHeaderIcons` can no longer have a non-composable default, because a `Painter` from a
  resource exists only inside composition. It is read only in `HeaderCell`, under `TableHeader`,
  which always provides it (`TableHeader.kt`), so an erroring default is safe.
- `columnMenuModel()` is not composable. Its internal entries hold `DrawableResource`; the
  composable `resolve()` turns them into `Painter` with `painterResource` when it builds the public
  `ColumnMenuItem`s.
- Every `Icon(imageVector = TableIcons.X, …)` in `table-core`, `table-format` and `table-paging`
  becomes `Icon(painter = painterResource(TableIcons.X), …)`. `table-format` and `table-paging`
  get the `components-resources` dependency only; they own no resources.
- `SampleIcons` stays internal and returns `DrawableResource`; sample call sites use
  `painterResource`.

## Conversion

A one-off, uncommitted `jvmTest` in each module walks the current `ImageVector`s and writes the
XML: every `VectorPath` becomes a `<path>` with its `pathData` serialized from the `PathNode`s,
`android:fillColor="#FF000000"`, and `android:fillType="evenOdd"` where the path uses
`PathFillType.EvenOdd`. Viewport and size are copied from the vector, and `android:autoMirrored`
from `ImageVector.autoMirror`.

Before the `vector/*.kt` files are deleted, the same test loads each generated XML back through
`vectorResource` and asserts that its path nodes, fill types, viewport and auto-mirror flag equal
the original's. Then the test and the old files are deleted.

The header comment of `TableIcons` changes to: take the icon's XML from Material Icons (or
Android Studio *Vector Asset*), save it as `composeResources/drawable/<name>_<style>.xml`, add a
property. The license attribution for the Material path data stays.

## Tests

- `TableIconsTest` moves from `commonTest` to `jvmTest`: reading Compose resources from common
  tests on iOS and wasm is not reliable. It loads every `TableIcons` property through
  `vectorResource` in `runComposeUiTest` and keeps the current checks (24-unit viewport, non-empty
  outline inside the viewport), extended from 8 icons to all 26. A missing or malformed file fails
  here.
- Existing header and column menu tests keep passing; `icon = null` remains valid for `Painter?`.

## Docs and changelog

- `docs/content/guides/custom-header-icons.md`, `row-blocks.md`, `row-reordering.md`,
  `grouping.md`, `docs/content/reference/core-api.md`: `TableIcons.X` becomes
  `painterResource(TableIcons.X)`; "substitute any `ImageVector`" becomes "any `Painter`, e.g.
  `rememberVectorPainter(...)`". The custom-header-icons guide notes that on web, icons load
  asynchronously and may be blank for the first frame.
- `CHANGELOG.md`, Unreleased, one line without an issue link: `TableIcons` now returns
  `DrawableResource` and icon parameters take `Painter`; wrap with `painterResource(...)` or
  `rememberVectorPainter(...)`.

## Verification

- `:table-core:jvmTest` and the other modules' tests pass.
- All targets compile, including iOS (`-PenableIos=false` hides errors there).
- `publishToMavenLocal`, then confirm the `drawable/*.xml` files are inside the JVM jar, the
  Android aar, and the iOS and wasm klibs of `table-core`.
- The desktop and wasm samples show every icon, including the column menu, header sort/filter and
  format dialog.

## Out of scope

- Preloading icons on web to avoid the first-frame blank. Revisit only if it shows in the sample.
- Switching glyphs to Material Symbols.
