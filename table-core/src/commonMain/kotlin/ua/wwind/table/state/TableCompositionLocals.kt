package ua.wwind.table.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal to expose current TableState to internal table components.
 *
 * We avoid nullable default to keep failures explicit when provider is missing.
 */
internal val LocalTableState: ProvidableCompositionLocal<TableState<*>> =
    staticCompositionLocalOf { error("No TableState provided") }

@Composable
internal fun currentTableState(): TableState<*> = LocalTableState.current
