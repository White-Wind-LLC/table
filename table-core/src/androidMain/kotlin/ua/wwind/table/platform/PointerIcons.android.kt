package ua.wwind.table.platform

import androidx.compose.ui.input.pointer.PointerIcon
import android.view.PointerIcon as AndroidPointerIcon

internal actual val ColumnResizePointerIcon: PointerIcon =
    PointerIcon(AndroidPointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW)

internal actual val ColumnGrabPointerIcon: PointerIcon = PointerIcon(AndroidPointerIcon.TYPE_GRAB)
