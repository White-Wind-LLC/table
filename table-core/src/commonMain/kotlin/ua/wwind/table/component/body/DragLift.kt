package ua.wwind.table.component.body

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import ua.wwind.table.state.currentTableState

/**
 * Lifts a dragged row or block the way a dragged column header is lifted: its shadow animates up to
 * `TableDimensions.dragElevation` while [isDragging] and back down on drop.
 */
@Composable
internal fun DragLift(
    isDragging: Boolean,
    content: @Composable () -> Unit,
) {
    val elevation by animateDpAsState(if (isDragging) currentTableState().dimensions.dragElevation else 0.dp)
    // No layer at rest, and no clip when lifted: the pinned-edge shadow draws past a cell's bounds.
    Box(if (elevation > 0.dp) Modifier.shadow(elevation, clip = false) else Modifier) {
        content()
    }
}
