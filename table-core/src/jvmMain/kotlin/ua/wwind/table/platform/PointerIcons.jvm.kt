package ua.wwind.table.platform

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual val ColumnResizePointerIcon: PointerIcon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))

// AWT has no grab cursor; the move cursor is its closest stock match.
internal actual val ColumnGrabPointerIcon: PointerIcon = PointerIcon(Cursor(Cursor.MOVE_CURSOR))
