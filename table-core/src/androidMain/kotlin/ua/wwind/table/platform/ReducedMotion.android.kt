package ua.wwind.table.platform

import androidx.compose.runtime.Composable

// Compose scales every animation by the system animator duration scale, so "Remove animations"
// already makes them instant; nothing more to switch off here.
@Composable
internal actual fun systemPrefersReducedMotion(): Boolean = false
