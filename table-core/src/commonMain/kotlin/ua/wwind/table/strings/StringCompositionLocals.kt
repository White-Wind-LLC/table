package ua.wwind.table.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal to expose the active [StringProvider] to table components.
 * Defaults to [DefaultStrings] so consumers can still resolve strings even if
 * a provider is not explicitly set.
 */
internal val LocalStringProvider: ProvidableCompositionLocal<StringProvider> =
    staticCompositionLocalOf { DefaultStrings }

/**
 * Shorthand accessor for the current [StringProvider].
 */
@Composable
public fun currentStrings(): StringProvider = LocalStringProvider.current
