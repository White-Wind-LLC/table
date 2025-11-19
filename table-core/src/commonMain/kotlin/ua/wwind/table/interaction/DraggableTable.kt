package ua.wwind.table.interaction

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ScrollState
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
            baseModifier.pointerInput(horizontalState, verticalState) {
                // Decay for inertial fling animation
                val decay = exponentialDecay<Float>()

                var velocityTracker: VelocityTracker? = null

                detectDragGestures(
                    onDragStart = { velocityTracker = VelocityTracker() },
                    onDrag = { change, dragAmount ->
                        // Track positions for velocity computation
                        velocityTracker?.addPosition(change.uptimeMillis, change.position)

                        change.consume()
                        // Integrate with nested scroll: consume locally then report
                        // post-scroll
                        val consumedX =
                            if (dragAmount.x != 0f) {
                                horizontalState.dispatchRawDelta(-dragAmount.x)
                                dragAmount.x
                            } else {
                                0f
                            }
                        val consumedY =
                            if (dragAmount.y != 0f) {
                                verticalState.dispatchRawDelta(-dragAmount.y)
                                dragAmount.y
                            } else {
                                0f
                            }
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
                        val initialVelocity = Velocity(v.x, v.y)

                        // Run fling inside the provided scope to call suspend nested-scroll
                        // APIs
                        coroutineScope.launch {
                            // Participate in nested scroll for fling phase
                            val preConsumed =
                                nestedScrollDispatcher.dispatchPreFling(initialVelocity)
                            val available =
                                Velocity(
                                    initialVelocity.x - preConsumed.x,
                                    initialVelocity.y - preConsumed.y,
                                )

                            // Animate decayed fling for both axes in parallel
                            coroutineScope {
                                val jobX =
                                    launch {
                                        animateFlingAxis(
                                            initialVelocity = available.x,
                                            decay = decay,
                                        ) { delta: Float ->
                                            horizontalState.dispatchRawDelta(-delta)
                                        }
                                    }

                                val jobY =
                                    launch {
                                        animateFlingAxis(
                                            initialVelocity = available.y,
                                            decay = decay,
                                        ) { delta: Float ->
                                            verticalState.dispatchRawDelta(-delta)
                                        }
                                    }

                                jobX.join()
                                jobY.join()
                            }

                            // Notify parents about fling completion
                            nestedScrollDispatcher.dispatchPostFling(
                                consumed = available,
                                available = Velocity.Zero,
                            )
                        }
                    },
                )
            }
        } else {
            baseModifier
        }

    return modifierWithDrag.horizontalScroll(horizontalState, enableScrolling)
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
