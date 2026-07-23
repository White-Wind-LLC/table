package ua.wwind.table.format.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import ua.wwind.table.format.data.EditFormatRule
import ua.wwind.table.format.data.FormatDialogSettings

/** Transient UI state shared by the three format-dialog blocks: edited rule, just-copied index, list scroll state. */
internal class FormatDialogState<E : Enum<E>, FILTER>(
    val lazyListState: LazyListState,
) {
    var editItem: EditFormatRule<E, FILTER>? by mutableStateOf(null)
    var itemCopyIndex: Int? by mutableStateOf(null)
}

@Composable
internal fun <E : Enum<E>, FILTER> rememberFormatDialogState(
    settings: FormatDialogSettings,
): FormatDialogState<E, FILTER> {
    val lazyListState = rememberLazyListState()
    val state = remember { FormatDialogState<E, FILTER>(lazyListState) }
    LaunchedEffect(state.itemCopyIndex) {
        val index = state.itemCopyIndex ?: return@LaunchedEffect
        lazyListState.animateScrollToItem(index)
        delay(settings.copiedItemHighlightDuration)
        state.itemCopyIndex = null
    }
    return state
}
