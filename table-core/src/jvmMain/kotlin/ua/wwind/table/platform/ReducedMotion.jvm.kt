package ua.wwind.table.platform

import androidx.compose.runtime.Composable

// The JDK exposes no reduced-motion setting on any desktop OS.
@Composable
internal actual fun systemPrefersReducedMotion(): Boolean = false
