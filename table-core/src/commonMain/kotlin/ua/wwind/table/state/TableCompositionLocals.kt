package ua.wwind.table.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal to expose current TableState to internal table components.
 *
 * We avoid nullable default to keep failures explicit when provider is missing.
 */
internal val LocalTableState =
    staticCompositionLocalOf<TableState<*>> {
        error("No TableState provided")
    }

@Composable
internal fun ProvideTableState(
    state: TableState<*>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTableState provides state, content = content)
}

/**
 * Typed accessor for the current TableState from CompositionLocal.
 * Intended for internal use within table components.
 */
@Composable
internal inline fun <reified C> tableState(): TableState<C> = LocalTableState.current as TableState<C>

@Composable
internal fun currentTableState(): TableState<*> = LocalTableState.current
