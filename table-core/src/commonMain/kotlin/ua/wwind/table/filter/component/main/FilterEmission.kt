package ua.wwind.table.filter.component.main

import ua.wwind.table.filter.data.TableFilterState

/**
 * The single outcome of resolving a filter's editing input into what should happen to the filter.
 *
 * Both the debounced auto-apply path and the explicit Apply path resolve the same input through one
 * `resolve*Filter` function and act on this result, so the two can no longer drift apart (issue #55).
 */
internal sealed interface FilterEmission<out T> {
    /** Emit this filter state. */
    data class Apply<T>(
        val state: TableFilterState<T>,
    ) : FilterEmission<T>

    /** Clear the filter (emit `null`); the input is empty. */
    data object Clear : FilterEmission<Nothing>

    /**
     * The input is present but does not form a valid, complete filter. Emit nothing, keep the input
     * so the user can correct it, and surface the error in the UI. Produced by the number filter only.
     */
    data object Invalid : FilterEmission<Nothing>
}

/**
 * Emits [emission] through [onStateChange] and reports whether the input settled.
 *
 * Returns `true` for [FilterEmission.Apply] and [FilterEmission.Clear] (the caller should leave
 * editing mode), and `false` for [FilterEmission.Invalid] (emit nothing and keep the input so the
 * user can correct it). Both the debounced auto-apply path and the explicit Apply path route through
 * this single function, so they can never dispatch a resolved result differently.
 */
internal fun <T> applyEmission(
    emission: FilterEmission<T>,
    onStateChange: (TableFilterState<T>?) -> Unit,
): Boolean =
    when (emission) {
        is FilterEmission.Apply -> {
            onStateChange(emission.state)
            true
        }

        FilterEmission.Clear -> {
            onStateChange(null)
            true
        }

        FilterEmission.Invalid -> {
            false
        }
    }
