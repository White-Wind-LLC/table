package ua.wwind.table.format.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * Renders a vertical scrollbar for a scrollable container.
 */
@Stable
public fun interface VerticalScrollbarRenderer {
    @Composable
    public fun Render(
        modifier: Modifier,
        state: VerticalScrollbarState,
    )
}

/**
 * Provides scroll state details for vertical scrollbar rendering.
 */
@Stable
public sealed interface VerticalScrollbarState {
    public data class LazyList(
        public val state: LazyListState,
    ) : VerticalScrollbarState

    public data class Scroll(
        public val state: ScrollState,
    ) : VerticalScrollbarState
}
