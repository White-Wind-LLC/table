package ua.wwind.table.platform

import androidx.compose.ui.input.pointer.PointerIcon

// Compose keeps CSS cursors (col-resize, grab) internal on the web, so only the stock icons are reachable.
internal actual val ColumnResizePointerIcon: PointerIcon = PointerIcon.Hand

internal actual val ColumnGrabPointerIcon: PointerIcon = PointerIcon.Default
