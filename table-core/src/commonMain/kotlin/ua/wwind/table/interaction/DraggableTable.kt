package ua.wwind.table.interaction

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal fun Modifier.draggableTable(
    horizontalState: ScrollState,
    verticalState: LazyListState,
    blockParentScrollConnection: NestedScrollConnection,
    nestedScrollDispatcher: NestedScrollDispatcher,
    enableScrolling: Boolean,
    enableDragToScroll: Boolean,
    coroutineScope: CoroutineScope,
): Modifier {
    val baseModifier = this.nestedScroll(blockParentScrollConnection, nestedScrollDispatcher)

    val modifierWithDrag =
        if (enableDragToScroll) {
            baseModifier.dragToScroll(horizontalState, verticalState, nestedScrollDispatcher, coroutineScope)
        } else {
            baseModifier
        }

    return modifierWithDrag.horizontalScroll(horizontalState, enableScrolling)
}

/**
 * Drags the table content on both axes, participating in nested scroll on every phase, and hands the
 * gesture over to an inertial fling when the pointer is lifted.
 */
private fun Modifier.dragToScroll(
    horizontalState: ScrollState,
    verticalState: LazyListState,
    nestedScrollDispatcher: NestedScrollDispatcher,
    coroutineScope: CoroutineScope,
): Modifier =
    pointerInput(horizontalState, verticalState) {
        // Decay for inertial fling animation
        val decay = exponentialDecay<Float>()

        var velocityTracker: VelocityTracker? = null

        detectDragGestures(
            onDragStart = { velocityTracker = VelocityTracker() },
            onDrag = { change, dragAmount ->
                // Track positions for velocity computation
                velocityTracker?.addPosition(change.uptimeMillis, change.position)

                change.consume()
                // Integrate with nested scroll: consume locally then report post-scroll
                val consumedX = horizontalState.consumeRawDelta(dragAmount.x)
                val consumedY = verticalState.consumeRawDelta(dragAmount.y)
                nestedScrollDispatcher.dispatchPostScroll(
                    consumed = Offset(consumedX, consumedY),
                    available = Offset.Zero,
                    source = NestedScrollSource.UserInput,
                )
            },
            onDragCancel = { velocityTracker = null },
            onDragEnd = {
                val tracker = velocityTracker
                velocityTracker = null

                if (tracker == null) return@detectDragGestures

                val v = tracker.calculateVelocity()
                // Run fling inside the provided scope to call suspend nested-scroll APIs
                coroutineScope.launch {
                    flingBothAxes(
                        initialVelocity = Velocity(v.x, v.y),
                        decay = decay,
                        horizontalState = horizontalState,
                        verticalState = verticalState,
                        nestedScrollDispatcher = nestedScrollDispatcher,
                    )
                }
            },
        )
    }

/** Applies [delta] to the receiver and reports back how much of it was consumed. */
private fun ScrollableState.consumeRawDelta(delta: Float): Float {
    if (delta == 0f) return 0f
    dispatchRawDelta(-delta)
    return delta
}

/** Decays [initialVelocity] on both axes in parallel, bracketed by the nested-scroll fling phase. */
@Suppress("LongParameterList")
private suspend fun flingBothAxes(
    initialVelocity: Velocity,
    decay: DecayAnimationSpec<Float>,
    horizontalState: ScrollState,
    verticalState: LazyListState,
    nestedScrollDispatcher: NestedScrollDispatcher,
) {
    // Participate in nested scroll for fling phase
    val preConsumed = nestedScrollDispatcher.dispatchPreFling(initialVelocity)
    val available = Velocity(initialVelocity.x - preConsumed.x, initialVelocity.y - preConsumed.y)

    // Animate decayed fling for both axes in parallel
    coroutineScope {
        val jobX =
            launch {
                animateFlingAxis(available.x, decay) { delta -> horizontalState.dispatchRawDelta(-delta) }
            }
        val jobY =
            launch {
                animateFlingAxis(available.y, decay) { delta -> verticalState.dispatchRawDelta(-delta) }
            }
        jobX.join()
        jobY.join()
    }

    // Notify parents about fling completion
    nestedScrollDispatcher.dispatchPostFling(consumed = available, available = Velocity.Zero)
}

// Shared helper to animate fling on a single axis with decay and apply produced deltas
private suspend fun animateFlingAxis(
    initialVelocity: Float,
    decay: DecayAnimationSpec<Float>,
    applyDelta: (Float) -> Unit,
) {
    var lastValue = 0f
    val anim = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
    anim.animateDecay(decay) {
        val delta = value - lastValue
        lastValue = value
        if (delta != 0f) applyDelta(delta)
    }
}
