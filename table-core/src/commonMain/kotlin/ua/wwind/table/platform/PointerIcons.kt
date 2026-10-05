package ua.wwind.table.platform

import androidx.compose.ui.input.pointer.PointerIcon

/** Horizontal-resize cursor shown over a column boundary; [PointerIcon.Hand] where the platform has none. */
internal expect val ColumnResizePointerIcon: PointerIcon

/** Grab cursor shown over the column drag handle; [PointerIcon.Default] where the platform has none. */
internal expect val ColumnGrabPointerIcon: PointerIcon
