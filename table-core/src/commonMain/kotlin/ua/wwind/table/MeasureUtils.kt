package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Measures the minimal intrinsic width of [content] and reports it via [onMeasured].
 *
 * This is used for both table body cells and header cells to compute a column's
 * max content width. The measurement is deferred to the next frame to avoid
 * feedback loops with composition and measurement.
 */
@Composable
internal fun <T> MeasureCellMinWidth(
    item: T,
    measureKey: Any,
    content: @Composable BoxScope.(T) -> Unit,
    onMeasured: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    // Lightweight mutable box shared between measure (SubcomposeLayout) and effect scopes,
    // without introducing Compose state or recompositions.
    // Two slots: holder[0] = latest measured px; holder[1] = last dispatched px (for de-dup).
    val holder = remember { IntArray(2) { -1 } }
    SubcomposeLayout {
        val measurables =
            subcompose("measure") {
                Box { content(item) }
            }
        holder[0] = measurables.maxOfOrNull { it.maxIntrinsicWidth(0) } ?: 0
        layout(0, 0) {}
    }
    LaunchedEffect(measureKey) {
        // Defer to the next UI frame (after the current composition/measure/layout pass).
        // This ensures SubcomposeLayout has already produced up-to-date intrinsic measurements,
        // so the value in holder[0] is valid. Triggering on the next frame avoids running too
        // early (e.g., in SideEffect) and prevents feedback loops between composition and measurement.
        // We ignore the frame timestamp; we only use this as a synchronization point with the UI frame.
        withFrameNanos {
            val px = holder[0]
            if (px > 0 && px != holder[1]) {
                holder[1] = px
                onMeasured(with(density) { px.toDp() })
            }
        }
    }
}
