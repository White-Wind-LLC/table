# Drag-to-scroll

By default, the table enables drag-to-scroll functionality, allowing users to pan the table content by dragging with
mouse or touch gestures. While this works well on mobile devices, it may not be ideal for desktop environments where
traditional scrollbars and mouse wheel navigation are preferred.

To disable drag-to-scroll and use standard scrollbars instead:

```kotlin
val state = rememberTableState(
    columns = columns.map { it.key },
    settings = TableSettings(
        enableDragToScroll = false, // Disable drag-to-scroll
        // ... other settings
    )
)
```

When `enableDragToScroll = false`:

- Mouse dragging will not scroll the table
- Horizontal and vertical scrollbars will be available
- Mouse wheel and trackpad gestures will work normally
- Better compatibility with cell selection and text selection workflows
