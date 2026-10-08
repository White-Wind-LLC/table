package ua.wwind.table.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.unit.IntOffset
import ua.wwind.table.config.TableMotion
import ua.wwind.table.config.isReduced
import ua.wwind.table.state.currentTableState
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The table's motion tokens: every animation takes its spec from here, so one switch makes all of them
 * instant under reduced motion. [move] places items (rows, columns) after a reorder or a sort, [fade]
 * shows and hides, and [settle] drives small state changes such as a drag lift or a shadow.
 */
@Immutable
internal class TableMotionSpecs private constructor(
    val reduced: Boolean,
) {
    fun <T> move(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = visibilityThreshold,
            )
        }

    fun <T> fade(): FiniteAnimationSpec<T> = if (reduced) snap() else spring(stiffness = Spring.StiffnessMediumLow)

    fun <T> settle(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(visibilityThreshold = visibilityThreshold)

    /** [enter] for an [androidx.compose.animation.AnimatedVisibility]; none at all when reduced. */
    fun enter(full: () -> EnterTransition = { fadeIn(fade()) }): EnterTransition =
        if (reduced) EnterTransition.None else full()

    fun exit(
        full: () -> ExitTransition = {
            fadeOut(fade())
        },
    ): ExitTransition = if (reduced) ExitTransition.None else full()

    /**
     * Context for a coroutine that animates a scroll. Compose reads the duration scale from it, so when
     * reduced, `animateScrollBy` and `animateScrollToItem` jump straight to their target.
     */
    val scrollContext: CoroutineContext get() = if (reduced) InstantScroll else EmptyCoroutineContext

    private object InstantScroll : MotionDurationScale {
        override val scaleFactor: Float get() = 0f
    }

    companion object {
        val Full = TableMotionSpecs(reduced = false)
        val Reduced = TableMotionSpecs(reduced = true)
    }
}

/** The motion tokens for [ua.wwind.table.config.TableSettings.motion] of the enclosing table. */
@Composable
internal fun currentTableMotion(): TableMotionSpecs = rememberTableMotionSpecs(currentTableState().settings.motion)

@Composable
internal fun rememberTableMotionSpecs(motion: TableMotion): TableMotionSpecs =
    if (motion.isReduced()) TableMotionSpecs.Reduced else TableMotionSpecs.Full

/** Animates a lazy item to its new place (and in and out) with the table's [move] and [fade] tokens. */
internal fun LazyItemScope.tableAnimateItem(motion: TableMotionSpecs): Modifier =
    if (motion.reduced) {
        Modifier
    } else {
        Modifier.animateItem(
            fadeInSpec = motion.fade(),
            placementSpec = motion.move(IntOffset.VisibilityThreshold),
            fadeOutSpec = motion.fade(),
        )
    }
