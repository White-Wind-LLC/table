# Portable Format-Dialog Content — Design

## Goal

Expose the conditional-formatting UI as a **portable content composable** so a consumer can host it in any
container it chooses — an adaptive dialog host, a side panel, a split pane — instead of only the built-in
`AlertDialog`. Alongside it, expose explicit **color parameters** so the content themes correctly outside an
`AlertDialog`. The existing `FormatDialog` stays source-compatible (the new `colors` parameter is optional and appended last).

## Motivation

`FormatDialog` hard-codes a Material3 `AlertDialog`. Two limitations follow:

1. An application that wraps its dialogs in a custom host (window-derived sizing, corner resize,
   dismiss-on-navigation, a house dialog shape) cannot apply that host to `FormatDialog`. The formatting dialog
   then visibly diverges from every other dialog in that application.
2. The formatting UI cannot be embedded anywhere but a modal — a side panel or a master/detail pane is impossible.

There is also a hidden coupling: because the UI renders inside `AlertDialog`'s `title` / `text` slots, it silently
relies on `AlertDialog` to inject `titleContentColor` / `textContentColor`. Those must become explicit for the
content to render correctly in a bare container.

## Non-goals

- Restyling the nested `ColorPickerDialog` (a modal opened deep inside the design tab). Its call site is not
  reachable by a consumer, so it needs an injected dialog-host renderer rather than content extraction — a
  separate change (see [Out of scope](#out-of-scope)).
- Any change to `FormatDialogSettings` (stays the behavior / highlight value holder).
- Any change to `FormatDialog`'s default rendering.

## Approach

The three `AlertDialog` slots in `FormatDialog` (`title`, `confirmButton`, `text`) are three projections of one
small state machine: `editItem` (list mode vs edit mode), `itemCopyIndex`, `lazyListState`. Everything else
(`rulesState`, `currentTab`, reorderable state, the debounce effect) is local to the body.

Extract those three projections into **internal blocks that share a small internal state holder**, then reuse the
blocks from two public entry points:

- `FormatDialog` — unchanged public shape; wires the blocks into an `AlertDialog`. Same container, same look;
  existing consumers untouched.
- `FormatDialogContent` — new; lays the same blocks out itself and lets the consumer supply the container.

Both entry points reuse identical blocks, so there is no duplication and no drift. `FormatDialog` keeps its
`AlertDialog`, so back-compat is total.

## Public API

### Colors

Mirrors the existing `TableColors` / `TableDefaults.colors()` convention.

```kotlin
@Immutable
public data class FormatDialogColors(
    val containerColor: Color,
    val titleContentColor: Color,
    val textContentColor: Color,
    val tonalElevation: Dp,
)

public object FormatDialogDefaults {
    @Composable
    public fun colors(
        containerColor: Color = AlertDialogDefaults.containerColor,
        titleContentColor: Color = AlertDialogDefaults.titleContentColor,
        textContentColor: Color = AlertDialogDefaults.textContentColor,
        tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    ): FormatDialogColors = FormatDialogColors(containerColor, titleContentColor, textContentColor, tonalElevation)
}
```

No `iconContentColor`: the formatting dialog has no icon slot; it would be a dead field (add later if a title icon
is introduced).

### `FormatDialogContent`

```kotlin
@Composable
public fun <E : Enum<E>, FILTER> FormatDialogContent(
    rules: ImmutableList<TableFormatRule<E, FILTER>>,
    onRulesChange: (ImmutableList<TableFormatRule<E, FILTER>>) -> Unit,
    getNewRule: (id: Long) -> TableFormatRule<E, FILTER>,
    getTitle: @Composable (E) -> String,
    filters: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    entries: ImmutableList<E>,
    key: Any,
    strings: StringProvider,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    colors: FormatDialogColors = FormatDialogDefaults.colors(),
    settings: FormatDialogSettings = FormatDialogSettings(),
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
)
```

- No `showDialog`, no `DialogProperties`: visibility and scrim belong to the container the consumer chooses.
- `onDismissRequest = null` hides the close "X" (title X and edit-mode close) — for embedded / in-panel use where
  "dismiss" is meaningless.
- Consumes only the **content** colors (`titleContentColor`, `textContentColor`). `containerColor` /
  `tonalElevation` are ignored here — the background and elevation belong to the parent container. KDoc states it.
- Fills the space its parent gives and scrolls internally; the parent must provide a **bounded height**. KDoc
  states it.

Layout:

```
Column(modifier.fillMaxSize()) {
    FormatDialogTitle(...)                              // pinned top, titleContentColor
    HorizontalDivider()
    CompositionLocalProvider(LocalContentColor provides colors.textContentColor) {
        FormatDialogBody(Modifier.weight(1f).fillMaxWidth(), ...)   // scrolls
    }
    Row(Modifier.fillMaxWidth().padding(...), Arrangement.End) { FormatDialogButtons(...) }  // bottom bar
}
```

### `FormatDialog` (additive only)

Add `colors: FormatDialogColors = FormatDialogDefaults.colors()` as a new optional parameter; the rest of the
public signature is unchanged. Internally rewritten to create the shared state and wire the blocks into
`AlertDialog`'s `title` / `confirmButton` / `text` slots, passing `colors` into the corresponding `AlertDialog`
color parameters. With defaults equal to `AlertDialogDefaults`, the rendered result is identical to today.

## Content / container ownership

| Concern | `FormatDialog` | `FormatDialogContent` |
| --- | --- | --- |
| Visibility / scrim | own (`showDialog` + `AlertDialog`) | container's |
| Background + elevation | own `AlertDialog` (`containerColor`, `tonalElevation`) | container's (fields ignored) |
| Title / text content color | applied by blocks | applied by blocks |
| Size / resize / dismiss behavior | `AlertDialog` | container's |

## Internal units

- `FormatDialogState` (internal) — holds `editItem`, `itemCopyIndex`, `lazyListState`; created by a
  `@Composable internal fun rememberFormatDialogState(settings, key)` that also runs the copy-scroll
  `LaunchedEffect(itemCopyIndex)`. Both entry points create it identically.
- `FormatDialogTitle` (internal) — title Row; renders the "X" only when `editItem == null` **and** a close
  callback is present; applies `titleContentColor` explicitly (no reliance on an outer `AlertDialog`).
- `FormatDialogButtons` (internal) — FAB "add" in list mode; save / delete / copy / close Row in edit mode;
  operates on the `rules` param + `onRulesChange` + `getNewRule` (unchanged logic).
- `FormatDialogBody` (internal, takes `modifier`) — list ↔ edit form; owns `rulesState`, `currentTab`, the
  reorderable state and the debounce effect (unchanged logic, relocated).

The three tab panels (`FormatDialogDesignTab`, `FormatDialogConditionTab`, `FormatDialogFieldTab`) are unchanged —
they already communicate only through `item` / `onChange` and hold no cross-block state.

## Backward compatibility

- `FormatDialog`'s public signature changes only by gaining one optional `colors` parameter with a default equal
  to today's implicit `AlertDialog` styling. Existing call sites compile and render unchanged.
- The sample is untouched.
- New public types (`FormatDialogColors`, `FormatDialogDefaults`, `FormatDialogContent`) are purely additive.

## Consumer adoption

A consumer that wants the formatting UI inside its own dialog host or an embedded region:

1. Depend on the release that ships this (2.1.1).
2. Replace the direct `FormatDialog(showDialog = …, …)` call with its own container gated on the same visibility
   flag, hosting `FormatDialogContent(…)` inside. Pass the container's dismiss callback as `onDismissRequest`.
3. For an embedded (non-modal) placement, pass `onDismissRequest = null` and give the content a bounded-height
   parent.

Default colors match `AlertDialog`, so a consumer whose container already uses `AlertDialogDefaults` for its
surface needs no color overrides.

## Testing

- `:table-format` compiles; `:table-core:jvmTest` and the `table-format` tests stay green.
- New Compose UI test over `FormatDialogContent`: renders the rule list; entering edit mode and pressing save
  round-trips through `onRulesChange`; the close "X" is absent when `onDismissRequest = null` and present
  otherwise.
- Run the sample to confirm the default `FormatDialog` is visually unchanged.

## Release

- Ships in `2.1.1` from `main`, bundled with the pending within-block drag fix.
- Additive API → minor/patch, no breaking change.
- Kotlin toolchain unchanged (2.4.10), so consumers on the current line upgrade without a toolchain bump.

## Risks

| Risk | Mitigation |
| --- | --- |
| `FormatDialogContent` self-layout: the body `LazyColumn` needs a bounded height via `weight(1f)`; an unbounded parent breaks it | KDoc the bounded-parent contract; the risk is isolated to the new path and cannot regress `FormatDialog` |
| Content colors drift from `AlertDialog` in the content path | Colors are explicit (`FormatDialogColors`, defaults = `AlertDialogDefaults`); the blocks apply them, so both paths use one source |
| The FAB "add" (list mode) sits in a bottom end-aligned row in the content path rather than `AlertDialog`'s action area | Matches `AlertDialog`'s `confirmButton` placement; verified against the default path in the sample |

## Out of scope

The nested `ColorPickerDialog` keeps its own chrome. It is opened deep inside the design tab, so content extraction
does not fit. A future change can add an injected `DialogHostRenderer` (a fun-interface renderer threaded like
`scrollbarRenderer`) that the library uses to wrap the picker, with the consumer supplying its own host. Additive,
non-breaking, separate.
